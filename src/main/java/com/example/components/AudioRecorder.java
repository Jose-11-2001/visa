package com.example.components;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AudioRecorder {

    private TargetDataLine line;
    private Thread recordingThread;
    private volatile boolean running;
    private final ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();

    private static final AudioFormat FORMAT = new AudioFormat(
        16000,    // sampleRate (Vosk expects 16kHz)
        16,       // sampleSizeInBits
        1,        // channels (mono)
        true,     // signed
        false     // littleEndian
    );

    /** Starts recording in a background thread. */
    public void start() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(FORMAT);
        line.start();
        running = true;
        audioBuffer.reset();

        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (running) {
                int count = line.read(buffer, 0, buffer.length);
                if (count > 0) {
                    audioBuffer.write(buffer, 0, count);
                }
            }
        }, "audio-recorder");
        recordingThread.start();
    }

    /** Stops recording and returns captured PCM bytes. */
    public byte[] stop() throws InterruptedException {
        running = false;
        if (recordingThread != null) {
            recordingThread.join(1000);
        }
        if (line != null) {
            line.stop();
            line.close();
        }
        return audioBuffer.toByteArray();
    }
}