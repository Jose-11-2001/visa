package com.example.components;

import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Continuously listens to the microphone and fires a callback when a
 * keyword is heard. Uses Vosk streaming recognition.
 *
 * Commands: "answer", "stop", "next", "repeat", "hear", "finish"
 */
public class VoiceCommandService {

    private final String modelPath;
    private Thread listenerThread;
    private volatile boolean running;
    private Consumer<String> onCommand;
    private volatile boolean paused;

    public VoiceCommandService(String modelPath) {
        this.modelPath = modelPath;
    }

    public void setOnCommand(Consumer<String> callback) {
        this.onCommand = callback;
    }

    /**
     * Picks the best mic format that ACTUALLY opens on this system.
     * Does not rely on isLineSupported(), which lies on Windows.
     */
    private static AudioFormat pickVoiceFormat() {
        int[][] candidates = {
                { 16000, 1, 16 }, { 16000, 2, 16 },
                { 44100, 1, 16 }, { 44100, 2, 16 },
                { 48000, 1, 16 }, { 48000, 2, 16 },
                { 22050, 1, 16 }, { 22050, 2, 16 },
                { 8000, 1, 16 }, { 8000, 2, 16 },
        };

        for (int[] c : candidates) {
            int frameSize = (c[2] / 8) * c[1];
            AudioFormat fmt = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    c[0], c[2], c[1], frameSize, c[0], false);
            try {
                TargetDataLine test = (TargetDataLine) AudioSystem
                        .getLine(new DataLine.Info(TargetDataLine.class, fmt));
                test.open(fmt);
                test.close();
                return fmt;
            } catch (Exception ignored) {
            }
        }

        // Last resort: iterate every mixer's TargetDataLine
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            Mixer m = AudioSystem.getMixer(mi);
            for (Line.Info li : m.getTargetLineInfo()) {
                if (li.getLineClass() == TargetDataLine.class) {
                    try {
                        TargetDataLine test = (TargetDataLine) m.getLine(li);
                        test.open();
                        AudioFormat fmt = test.getFormat();
                        test.close();
                        return fmt;
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        throw new IllegalStateException(
                "No supported microphone format found on this system.");
    }

    public void start() throws IOException, LineUnavailableException {
        if (running)
            return;

        Model model = new Model(modelPath);
        Recognizer recognizer = new Recognizer(model, 16000);

        AudioFormat chosenFormat = pickVoiceFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, chosenFormat);
        TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(info);
        mic.open(chosenFormat);
        mic.start();

        running = true;
        final float sourceRate = chosenFormat.getSampleRate();
        final int channels = chosenFormat.getChannels();

        System.out.println(">>> Voice listener format: " + chosenFormat);

        listenerThread = new Thread(() -> {
            byte[] buf = new byte[4096];
            try {
                while (running) {
                    int n = mic.read(buf, 0, buf.length);
                    if (n <= 0)
                        continue;
                    if (paused)
                        continue;

                    byte[] audio16k = (sourceRate == 16000f && channels == 1)
                            ? java.util.Arrays.copyOf(buf, n)
                            : resample(buf, n, sourceRate, channels, 16000f);

                    if (recognizer.acceptWaveForm(audio16k, audio16k.length)) {
                        String json = recognizer.getResult();
                        String text = extract(json).toLowerCase();
                        if (!text.isBlank()) {
                            handlePhrase(text);
                        }
                    }
                }
            } catch (Exception e) {
                if (running)
                    e.printStackTrace();
            } finally {
                try {
                    mic.stop();
                    mic.close();
                    recognizer.close();
                    model.close();
                } catch (Exception ignored) {
                }
            }
        }, "voice-command-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /** Converts any PCM buffer to 16 kHz mono. */
    private byte[] resample(byte[] input, int inputLength,
            float fromRate, int channels, float toRate) {
        int bytesPerSample = 2 * channels;
        int inSamples = inputLength / bytesPerSample;
        int outSamples = (int) (inSamples * toRate / fromRate);
        byte[] output = new byte[outSamples * 2];

        for (int i = 0; i < outSamples; i++) {
            float srcIndex = i * fromRate / toRate;
            int i0 = (int) Math.floor(srcIndex);
            int i1 = Math.min(i0 + 1, inSamples - 1);
            float frac = srcIndex - i0;

            short s0 = readSample(input, i0, channels);
            short s1 = readSample(input, i1, channels);
            short s = (short) (s0 * (1 - frac) + s1 * frac);

            output[i * 2] = (byte) (s & 0xFF);
            output[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return output;
    }

    private short readSample(byte[] data, int sampleIndex, int channels) {
        if (channels == 1) {
            int offset = sampleIndex * 2;
            return (short) ((data[offset] & 0xFF) | (data[offset + 1] << 8));
        }
        int offset = sampleIndex * 4;
        short left = (short) ((data[offset] & 0xFF) | (data[offset + 1] << 8));
        short right = (short) ((data[offset + 2] & 0xFF) | (data[offset + 3] << 8));
        return (short) ((left + right) / 2);
    }

    private void handlePhrase(String text) {
        if (onCommand == null)
            return;

        String command = null;
        if (containsWord(text, "answer") || containsWord(text, "start")) {
            command = "answer";
        } else if (containsWord(text, "stop") || containsWord(text, "done")) {
            command = "stop";
        } else if (containsWord(text, "next")) {
            command = "next";
        } else if (containsWord(text, "repeat")) {
            command = "repeat";
        } else if (containsWord(text, "hear")) {
            command = "hear";
        } else if (containsWord(text, "finish") || containsWord(text, "submit")) {
            command = "finish";
        }

        if (command != null) {
            System.out.println(">>> Voice command: " + command);
            onCommand.accept(command);
        }
    }

    private boolean containsWord(String text, String word) {
        return (" " + text + " ").contains(" " + word + " ");
    }

    private String extract(String json) {
        int start = json.indexOf("\"text\" : \"") + 10;
        int end = json.indexOf("\"", start);
        return (start > 9 && end > start) ? json.substring(start, end) : "";
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public void stop() {
        running = false;
        if (listenerThread != null) {
            try {
                listenerThread.join(500);
            } catch (InterruptedException ignored) {
            }
        }
    }
}