package com.example.services;

import java.io.File;

public class ModelLocator {

    private static final String MODEL_NAME = "vosk-model-small-en-us-0.15";

    public static String findModelPath() {
        // 1. Next to the executable (portable install)
        String exeDir = System.getProperty("jpackage.app-path");
        if (exeDir != null) {
            File candidate = new File(new File(exeDir).getParent(),
                    "models" + File.separator + MODEL_NAME);
            if (candidate.exists())
                return candidate.getAbsolutePath();
        }

        // 2. User app data folder
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            File candidate = new File(appData, "VIS Exam App"
                    + File.separator + "models" + File.separator + MODEL_NAME);
            if (candidate.exists())
                return candidate.getAbsolutePath();
        }

        // 3. Development fallback
        File dev = new File("D:/Projects/visa/models/" + MODEL_NAME);
        if (dev.exists())
            return dev.getAbsolutePath();

        // 4. Relative to working directory
        File cwd = new File("models/" + MODEL_NAME);
        if (cwd.exists())
            return cwd.getAbsolutePath();

        throw new IllegalStateException(
                "Vosk model not found. Expected folder: " + MODEL_NAME);
    }
}