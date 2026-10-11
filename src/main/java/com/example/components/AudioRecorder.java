package com.example.components;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

public class AudioRecorder {

    private TargetDataLine line;
    private Thread recordingThread;
    private volatile boolean running;
    private final ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
    private AudioFormat actualFormat;

    /** Try each format until one actually opens successfully. */
    private TargetDataLine openAnyLine() throws LineUnavailableException {
        int[][] candidates = {
                { 16000, 1, 16 }, { 16000, 2, 16 },
                { 44100, 1, 16 }, { 44100, 2, 16 },
                { 48000, 1, 16 }, { 48000, 2, 16 },
                { 22050, 1, 16 }, { 22050, 2, 16 },
                { 8000, 1, 16 }, { 8000, 2, 16 },
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
                if (ex instanceof LineUnavailableException) {
                    lastError = (LineUnavailableException) ex;
                } else {
                    lastError = new LineUnavailableException(ex.getMessage());
                }
            }
        }

        // Last resort: iterate every mixer, every TargetDataLine
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
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        throw (lastError != null)
                ? lastError
                : new LineUnavailableException("No microphone format could be opened.");
    }

    public void start() throws LineUnavailableException {
        line = openAnyLine();
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

    public AudioFormat getActualFormat() {
        return actualFormat;
    }
}