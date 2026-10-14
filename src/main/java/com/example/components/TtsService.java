package com.example.components;

import java.io.IOException;

public class TtsService {

    private static Process currentProcess;
    private static int rate = 0; // -10 (slowest) to +10 (fastest)
    private static int volume = 100; // 0-100

    public static void setRate(int r) {
        rate = Math.max(-10, Math.min(10, r));
    }

    public static int getRate() {
        return rate;
    }

    public static void speedUp() {
        setRate(rate + 2);
    }

    public static void slowDown() {
        setRate(rate - 2);
    }

    public static void resetSpeed() {
        rate = 0;
    }

    public static void setVolume(int v) {
        volume = Math.max(0, Math.min(100, v));
    }

    /** Speaks text aloud using Windows' built-in speech engine. */
    public static synchronized void speak(String text) {
        stop();
        final int currentRate = rate;
        final int currentVolume = volume;

        new Thread(() -> {
            try {
                String safe = text.replace("'", "''")
                        .replace("\n", " ")
                        .replace("\r", " ");
                String cmd = "Add-Type -AssemblyName System.Speech; " +
                        "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "$s.Rate = " + currentRate + "; " +
                        "$s.Volume = " + currentVolume + "; " +
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

    public static synchronized void stop() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroyForcibly();
            currentProcess = null;
        }
    }
}