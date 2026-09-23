package com.example.components;

public class TtsService {

    /** Speaks text aloud using Windows' built-in speech engine (async). */
    public static void speak(String text) {
        new Thread(() -> {
            try {
                String safe = text.replace("'", "''");
                String cmd = "Add-Type -AssemblyName System.Speech; " +
                        "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "$s.Speak('" + safe + "')";
                ProcessBuilder pb = new ProcessBuilder(
                        "powershell", "-NoProfile", "-Command", cmd);
                pb.redirectErrorStream(true);
                pb.start().waitFor();
            } catch (Exception e) {
                System.err.println("TTS error: " + e.getMessage());
            }
        }).start();
    }
}