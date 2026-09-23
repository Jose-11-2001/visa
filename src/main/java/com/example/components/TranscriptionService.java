package com.example.components;

import org.vosk.Model;
import org.vosk.Recognizer;
import java.io.IOException;

public class TranscriptionService {

    private final Model model;

    public TranscriptionService(String modelPath) throws IOException {
        this.model = new Model(modelPath);
    }

    /** Transcribes a complete PCM audio buffer (16kHz, 16-bit, mono). */
    public String transcribe(byte[] pcmAudio) throws IOException {
        try (Recognizer recognizer = new Recognizer(model, 16000)) {
            recognizer.acceptWaveForm(pcmAudio, pcmAudio.length);
            String result = recognizer.getFinalResult();
            // Vosk returns JSON: {"text": "hello world"}
            return extractText(result);
        }
    }

    private String extractText(String json) {
        int start = json.indexOf("\"text\" : \"") + 10;
        int end = json.indexOf("\"", start);
        return (start > 9 && end > start) ? json.substring(start, end) : "";
    }

    public void close() {
        model.close();
    }
}