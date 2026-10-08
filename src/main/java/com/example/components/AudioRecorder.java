package com.example.components;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

/**
 * Records microphone audio using whatever format the system supports.
 * Falls back through many combinations of sample rate, channels, and bit depth.
 */
public class AudioRecorder {

    private TargetDataLine line;
    private Thread recordingThread;
    private volatile boolean running;
    private final ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
    private AudioFormat actualFormat;

    /**
     * Tries formats in priority order. Vosk prefers 16 kHz mono 16-bit,
     * but we accept anything the system supports and resample later.
     */
    private static AudioFormat findSupportedFormat() {
        // Priority list of (sampleRate, channels, bits)
        int[][] candidates = {
                { 16000, 1, 16 }, // ideal for Vosk
                { 16000, 2, 16 },
                { 44100, 1, 16 },
                { 44100, 2, 16 },
                { 48000, 1, 16 },
                { 48000, 2, 16 },
                { 22050, 1, 16 },
                { 22050, 2, 16 },
                { 8000, 1, 16 },
                { 8000, 2, 16 },
        };

        for (int[] c : candidates) {
            int rate = c[0], ch = c[1], bits = c[2];
            int frameSize = (bits / 8) * ch;
            AudioFormat fmt = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    rate, bits, ch, frameSize, rate, false);
            if (AudioSystem.isLineSupported(
                    new DataLine.Info(TargetDataLine.class, fmt))) {
                return fmt;
            }
        }

        // Last resort: ask the system what it actually offers
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            Mixer m = AudioSystem.getMixer(mi);
            for (Line.Info li : m.getTargetLineInfo()) {
                if (li.getLineClass() == TargetDataLine.class
                        && li instanceof DataLine.Info) {
                    AudioFormat[] formats = ((DataLine.Info) li).getFormats();
                    if (formats.length > 0)
                        return formats[0];
                }
            }
        }

        throw new IllegalStateException(
                "No supported microphone format found on this system.");
    }

    public void start() throws LineUnavailableException {
        actualFormat = findSupportedFormat();
        System.out.println(">>> Recording format: " + actualFormat);

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, actualFormat);
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(actualFormat);
        line.start();
        running = true;
        audioBuffer.reset();

        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (running) {
                int count = line.read(buffer, 0, buffer.length);
                if (count > 0)
                    audioBuffer.write(buffer, 0, count);
            }
        }, "audio-recorder");
        recordingThread.start();
    }

    public byte[] stop() throws InterruptedException {
        running = false;
        if (recordingThread != null)
            recordingThread.join(1000);
        if (line != null) {
            line.stop();
            line.close();
        }
        return audioBuffer.toByteArray();
    }

    /** Returns the format the mic actually used (needed for resampling). */
    public AudioFormat getActualFormat() {
        return actualFormat;
    }
}