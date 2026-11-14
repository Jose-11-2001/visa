package com.example.components;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;

public class AudioRecorder {

    private TargetDataLine line;
    private Thread recordingThread;
    private volatile boolean running;
    private final ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
    private AudioFormat actualFormat;

    // --- VAD / auto-gain tunables ---
    private static final double SPEECH_START_RMS = 0.018;   // threshold to start
    private static final double SPEECH_STOP_RMS  = 0.010;   // threshold to keep
    private static final int    START_FRAMES     = 3;       // must be loud this many frames to start
    private static final long   SILENCE_STOP_MS  = 1800;    // auto-stop after this much silence
    private static final long   MAX_RECORD_MS    = 60_000;  // safety cap
    private static final double TARGET_RMS       = 0.10;    // auto-gain target

    private Consumer<Double> levelListener;   // optional: report current level for UI
    private Runnable onAutoStop;              // optional: notify when silence auto-stops

    public void setLevelListener(Consumer<Double> l) { this.levelListener = l; }
    public void setOnAutoStop(Runnable r)            { this.onAutoStop = r; }

    /** Try each format until one actually opens successfully. */
    private TargetDataLine openAnyLine() throws LineUnavailableException {
        int[][] candidates = {
                {16000, 1, 16}, {16000, 2, 16},
                {44100, 1, 16}, {44100, 2, 16},
                {48000, 1, 16}, {48000, 2, 16},
                {22050, 1, 16}, {22050, 2, 16},
                {8000, 1, 16},  {8000, 2, 16},
        };
        LineUnavailableException lastError = null;
        for (int[] c : candidates) {
            int rate = c[0], ch = c[1], bits = c[2];
            int frameSize = (bits / 8) * ch;
            AudioFormat fmt = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    rate, bits, ch, frameSize, rate, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, fmt);
            try {
                TargetDataLine testLine = (TargetDataLine) AudioSystem.getLine(info);
                testLine.open(fmt);
                actualFormat = fmt;
                System.out.println(">>> Recording format: " + fmt);
                return testLine;
            } catch (LineUnavailableException | IllegalArgumentException ex) {
                System.out.println(">>> Format failed: " + fmt + " — " + ex.getMessage());
                lastError = (ex instanceof LineUnavailableException)
                        ? (LineUnavailableException) ex
                        : new LineUnavailableException(ex.getMessage());
            }
        }
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mi);
            for (Line.Info li : mixer.getTargetLineInfo()) {
                if (li.getLineClass() == TargetDataLine.class) {
                    try {
                        TargetDataLine testLine = (TargetDataLine) mixer.getLine(li);
                        testLine.open();
                        actualFormat = testLine.getFormat();
                        System.out.println(">>> Recording format (mixer default): " + actualFormat);
                        return testLine;
                    } catch (Exception ignored) {}
                }
            }
        }
        throw (lastError != null) ? lastError
                : new LineUnavailableException("No microphone format could be opened.");
    }

    /** Start listening. Audio is only captured AFTER the user starts speaking. */
    public void start() throws LineUnavailableException {
        line = openAnyLine();
        line.start();
        running = true;
        audioBuffer.reset();

        recordingThread = new Thread(this::captureLoop, "audio-recorder-vad");
        recordingThread.start();
    }

    private void captureLoop() {
        final int bytesPerSample = actualFormat.getSampleSizeInBits() / 8;
        final int channels       = actualFormat.getChannels();
        final int frameBytes     = bytesPerSample * channels;

        // ~30 ms frames for VAD stability
        int frameSizeBytes = (int) (actualFormat.getSampleRate() * 0.03) * frameBytes;
        if (frameSizeBytes < 512) frameSizeBytes = 512;

        byte[] frame = new byte[frameSizeBytes];

        boolean speechStarted = false;
        int loudFrames = 0;
        long lastSpeechAt = 0;
        long startedAt = 0;

        // We pre-buffer frames BEFORE speech triggers, so the first word isn't cut off
        final int PRE_BUFFER_FRAMES = 5;   // 5 × ~30ms ≈ 150ms of pre-roll
        java.util.ArrayDeque<byte[]> preBuffer = new java.util.ArrayDeque<>();

        while (running) {
            int n = line.read(frame, 0, frame.length);
            if (n <= 0) continue;

            double rms = computeRms(frame, n, bytesPerSample);

            // --- VAD decision ---
            if (!speechStarted) {
                if (rms > SPEECH_START_RMS) {
                    loudFrames++;
                } else {
                    loudFrames = 0;
                }

                // Keep a rolling pre-buffer so the start isn't clipped
                preBuffer.addLast(java.util.Arrays.copyOf(frame, n));
                while (preBuffer.size() > PRE_BUFFER_FRAMES) preBuffer.removeFirst();

                if (loudFrames >= START_FRAMES) {
                    speechStarted = true;
                    startedAt = System.currentTimeMillis();
                    lastSpeechAt = startedAt;
                    // Flush pre-buffer into the main buffer
                    for (byte[] f : preBuffer) audioBuffer.write(f, 0, f.length);
                    preBuffer.clear();
                    System.out.println(">>> VAD: speech detected, capturing...");
                }
            } else {
                // Already capturing: apply auto-gain and write
                short[] samples = bytesToShorts(frame, n, bytesPerSample);
                applyAutoGain(samples, channels);
                short[]ToBytes(samples, frame, n);
                audioBuffer.write(frame, 0, n);

                if (rms > SPEECH_STOP_RMS) {
                    lastSpeechAt = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - lastSpeechAt > SILENCE_STOP_MS) {
                    System.out.println(">>> VAD: silence detected, auto-stopping.");
                    running = false;
                    if (onAutoStop != null) onAutoStop.run();
                }
                if (System.currentTimeMillis() - startedAt > MAX_RECORD_MS) {
                    System.out.println(">>> VAD: max duration reached, auto-stopping.");
                    running = false;
                    if (onAutoStop != null) onAutoStop.run();
                }
            }

            if (levelListener != null) levelListener.accept(rms);
        }
    }

    /** RMS energy of a PCM-16 frame, normalized to [0,1]. */
    private double computeRms(byte[] buf, int len, int bytesPerSample) {
        long sum = 0;
        int count = 0;
        for (int i = 0; i + 1 < len; i += bytesPerSample) {
            short s = (short) ((buf[i] & 0xFF) | (buf[i + 1] << 8));
            sum += (long) s * s;
            count++;
        }
        if (count == 0) return 0;
        double mean = sum / (double) count;
        // 32768 = max amplitude → normalize
        return Math.sqrt(mean) / 32768.0;
    }

    private short[] bytesToShorts(byte[] buf, int len, int bytesPerSample) {
        int n = len / bytesPerSample;
        short[] out = new short[n];
        for (int i = 0; i < n; i++) {
            int off = i * bytesPerSample;
            out[i] = (short) ((buf[off] & 0xFF) | (buf[off + 1] << 8));
        }
        return out;
    }

    private void short[]ToBytes(short[] in, byte[] out, int len) {
        for (int i = 0; i < in.length; i++) {
            out[i * 2]     = (byte) (in[i] & 0xFF);
            out[i * 2 + 1] = (byte) ((in[i] >> 8) & 0xFF);
        }
    }

    /**
     * Auto-gain: adapt the user's speaking volume toward a target.
     * Applies a gentle per-frame gain so quiet voices are boosted,
     * loud voices aren't clipped, and background noise isn't amplified.
     */
    private void applyAutoGain(short[] samples, int channels) {
        // Compute frame RMS
        double sum = 0;
        for (short s : samples) sum += (double) s * s;
        double rms = Math.sqrt(sum / Math.max(1, samples.length)) / 32768.0;
        if (rms < 1e-4) return; // silence: don't amplify noise

        double gain = TARGET_RMS / rms;
        // Soft-clamp gain: never amplify more than 4×, never attenuate below 0.5×
        gain = Math.max(0.5, Math.min(4.0, gain));

        for (int i = 0; i < samples.length; i++) {
            int v = (int) (samples[i] * gain);
            if (v > 32767) v = 32767;
            if (v < -32768) v = -32768;
            samples[i] = (short) v;
        }
    }

    public byte[] stop() throws InterruptedException {
        running = false;
        if (recordingThread != null) recordingThread.join(1500);
        if (line != null) { line.stop(); line.close(); }
        return audioBuffer.toByteArray();
    }

    public AudioFormat getActualFormat() { return actualFormat; }
}