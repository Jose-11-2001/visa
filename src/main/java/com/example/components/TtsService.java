package com.example.components;

import java.io.IOException;

public class TtsService {

    private static Process currentProcess;

    /** Speaks text aloud using Windows' built-in speech engine (async). */
    public static synchronized void speak(String text) {
        stop(); // cancel any ongoing speech first
        new Thread(() -> {
            try {
                String safe = text.replace("'", "''")
                        .replace("\n", " ")
                        .replace("\r", " ");
                String cmd = "Add-Type -AssemblyName System.Speech; " +
                        "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "$s.Rate = 0; " +
                        "$s.Speak('" + safe + "')";
                ProcessBuilder pb = new ProcessBuilder(
                        "powershell", "-NoProfile", "-Command", cmd);
                pb.redirectErrorStream(true);
                currentProcess = pb.start();
                currentProcess.waitFor();
                currentProcess = null;
            } catch (Exception e) {
                System.err.println("TTS error: " + e.getMessage());
            }
        }).start();
    }

    /** Stops any ongoing speech. */
    public static synchronized void stop() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroyForcibly();
            currentProcess = null;
        }
    }
}