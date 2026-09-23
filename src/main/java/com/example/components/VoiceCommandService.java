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
 * Commands: "answer", "stop", "next", "repeat", "finish", "quit"
 */
public class VoiceCommandService {

    private final String modelPath;
    private Thread listenerThread;
    private volatile boolean running;
    private Consumer<String> onCommand;
    private volatile boolean paused;

    private static final AudioFormat FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            16000f, 16, 1, 2, 16000f, false);

    public VoiceCommandService(String modelPath) {
        this.modelPath = modelPath;
    }

    /** Sets the callback that receives recognized commands. */
    public void setOnCommand(Consumer<String> callback) {
        this.onCommand = callback;
    }

    /** Starts the background listener. */
    public void start() throws IOException, LineUnavailableException {
        if (running)
            return;

        Model model = new Model(modelPath);
        Recognizer recognizer = new Recognizer(model, 16000);

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException(
                    "Microphone does not support 16 kHz mono.");
        }
        TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(info);
        mic.open(FORMAT);
        mic.start();

        running = true;

        listenerThread = new Thread(() -> {
            byte[] buf = new byte[4096];
            try {
                while (running) {
                    int n = mic.read(buf, 0, buf.length);
                    if (n <= 0)
                        continue;
                    if (paused)
                        continue;

                    if (recognizer.acceptWaveForm(buf, n)) {
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

    /** Maps a full recognized phrase to a command word. */
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
        } else if (containsWord(text, "finish")) {
            command = "finish";
        }

        if (command != null) {
            System.out.println(">>> Voice command: " + command);
            onCommand.accept(command);
        }
    }

    private boolean containsWord(String text, String word) {
        // Simple word-boundary match
        return (" " + text + " ").contains(" " + word + " ");
    }

    private String extract(String json) {
        int start = json.indexOf("\"text\" : \"") + 10;
        int end = json.indexOf("\"", start);
        return (start > 9 && end > start) ? json.substring(start, end) : "";
    }

    /** Temporarily pauses command recognition (e.g. while the student answers). */
    public void pause() {
        paused = true;
    }

    /** Resumes command recognition. */
    public void resume() {
        paused = false;
    }

    /** Stops the listener and frees resources. */
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