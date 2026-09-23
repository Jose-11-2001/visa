package com.example.components;

import org.vosk.Model;
import org.vosk.Recognizer;
import java.io.IOException;

public class TranscriptionService {

    private final Model model;

    public TranscriptionService(String modelPath) throws IOException {
        this.model = new Model(modelPath);
    }

    /** Transcribes PCM audio. Automatically resamples to 16 kHz if needed. */
    public String transcribe(byte[] pcmAudio, float sourceSampleRate) throws IOException {
        byte[] audio16k = (sourceSampleRate == 16000f)
                ? pcmAudio
                : resample(pcmAudio, sourceSampleRate, 16000f);

        try (Recognizer recognizer = new Recognizer(model, 16000)) {
            recognizer.acceptWaveForm(audio16k, audio16k.length);
            String result = recognizer.getFinalResult();
            return extractText(result);
        }
    }

    /** Backwards-compatible overload — assumes 16 kHz input. */
    public String transcribe(byte[] pcmAudio) throws IOException {
        return transcribe(pcmAudio, 16000f);
    }

    private byte[] resample(byte[] input, float fromRate, float toRate) {
        int inSamples = input.length / 2; // 16-bit mono = 2 bytes per sample
        int outSamples = (int) (inSamples * toRate / fromRate);
        byte[] output = new byte[outSamples * 2];

        for (int i = 0; i < outSamples; i++) {
            float srcIndex = i * fromRate / toRate;
            int i0 = (int) Math.floor(srcIndex);
            int i1 = Math.min(i0 + 1, inSamples - 1);
            float frac = srcIndex - i0;

            short s0 = (short) ((input[i0 * 2] & 0xFF) | (input[i0 * 2 + 1] << 8));
            short s1 = (short) ((input[i1 * 2] & 0xFF) | (input[i1 * 2 + 1] << 8));
            short s = (short) (s0 * (1 - frac) + s1 * frac);

            output[i * 2] = (byte) (s & 0xFF);
            output[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return output;
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