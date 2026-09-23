package com.example.components;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

public class AudioRecorder {

    private TargetDataLine line;
    private Thread recordingThread;
    private volatile boolean running;
    private final ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
    private AudioFormat actualFormat;

    /** Requested format (Vosk's preferred). */
    private static final AudioFormat REQUESTED = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            16000f, 16, 1, 2, 16000f, false);

    /** Finds a mic-supported format closest to the requested one. */
    private static AudioFormat findSupportedFormat() {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, REQUESTED);
        if (AudioSystem.isLineSupported(info))
            return REQUESTED;

        float[] rates = { 16000f, 44100f, 48000f, 22050f, 8000f };
        for (float rate : rates) {
            AudioFormat candidate = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    rate, 16, 1, 2, rate, false);
            if (AudioSystem.isLineSupported(
                    new DataLine.Info(TargetDataLine.class, candidate))) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "No supported microphone format found on this system.");
    }

    public void start() throws LineUnavailableException {
        actualFormat = findSupportedFormat();
        System.out.println("Recording with format: " + actualFormat);

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

    /** The format that was actually used — needed for resampling. */
    public AudioFormat getActualFormat() {
        return actualFormat;
    }
}