package com.example.components;

import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;

/**
 * Transcribes PCM audio using Vosk. Accepts any input format
 * and converts to 16 kHz mono internally.
 */
public class TranscriptionService {

    private final Model model;

    public TranscriptionService(String modelPath) throws IOException {
        this.model = new Model(modelPath);
    }

    /**
     * Transcribe audio from any format. Handles:
     * - sample rate conversion (any rate → 16 kHz)
     * - channel conversion (stereo → mono)
     * - bit depth conversion (8/24/32-bit → 16-bit)
     */
    public String transcribe(byte[] pcmAudio, AudioFormat format)
            throws IOException {
        byte[] audio16k = toVoskFormat(pcmAudio, format);

        try (Recognizer recognizer = new Recognizer(model, 16000)) {
            recognizer.acceptWaveForm(audio16k, audio16k.length);
            return extractText(recognizer.getFinalResult());
        }
    }

    /** Backwards-compatible overload. */
    public String transcribe(byte[] pcmAudio, float sampleRate, int channels)
            throws IOException {
        AudioFormat fmt = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate, 16, channels, channels * 2, sampleRate, false);
        return transcribe(pcmAudio, fmt);
    }

    /** Converts any PCM format into 16 kHz mono signed 16-bit little-endian. */
    private byte[] toVoskFormat(byte[] input, AudioFormat fmt) {
        int bytesPerSample = fmt.getSampleSizeInBits() / 8;
        int channels = fmt.getChannels();
        float fromRate = fmt.getSampleRate();

        // Decode input to float samples in [-1, 1], averaged to mono
        int sampleCount = input.length / (bytesPerSample * channels);
        float[] mono = new float[sampleCount];

        for (int i = 0; i < sampleCount; i++) {
            float sum = 0f;
            for (int ch = 0; ch < channels; ch++) {
                int offset = (i * channels + ch) * bytesPerSample;
                sum += decodeSample(input, offset, fmt);
            }
            mono[i] = sum / channels;
        }

        // Resample to 16 kHz using linear interpolation
        float toRate = 16000f;
        int outSamples = (int) (sampleCount * toRate / fromRate);
        byte[] output = new byte[outSamples * 2];

        for (int i = 0; i < outSamples; i++) {
            float srcIndex = i * fromRate / toRate;
            int i0 = (int) Math.floor(srcIndex);
            int i1 = Math.min(i0 + 1, sampleCount - 1);
            float frac = srcIndex - i0;

            float s = mono[i0] * (1 - frac) + mono[i1] * frac;

            // Encode as signed 16-bit little-endian
            short s16 = (short) Math.max(-32768, Math.min(32767, s * 32767));
            output[i * 2] = (byte) (s16 & 0xFF);
            output[i * 2 + 1] = (byte) ((s16 >> 8) & 0xFF);
        }

        return output;
    }

    /** Decodes one sample from any supported bit depth into [-1, 1]. */
    private float decodeSample(byte[] data, int offset, AudioFormat fmt) {
        int bits = fmt.getSampleSizeInBits();
        boolean signed = fmt.getEncoding() == AudioFormat.Encoding.PCM_SIGNED;
        boolean bigEndian = fmt.isBigEndian();

        switch (bits) {
            case 8: {
                int raw = data[offset] & 0xFF;
                if (signed)
                    return (byte) raw / 128f;
                return (raw - 128) / 128f;
            }
            case 16: {
                int lo, hi;
                if (bigEndian) {
                    hi = data[offset] & 0xFF;
                    lo = data[offset + 1] & 0xFF;
                } else {
                    lo = data[offset] & 0xFF;
                    hi = data[offset + 1] & 0xFF;
                }
                short s = (short) ((hi << 8) | lo);
                return s / 32768f;
            }
            case 24: {
                int b0, b1, b2;
                if (bigEndian) {
                    b0 = data[offset] & 0xFF;
                    b1 = data[offset + 1] & 0xFF;
                    b2 = data[offset + 2] & 0xFF;
                } else {
                    b0 = data[offset] & 0xFF;
                    b1 = data[offset + 1] & 0xFF;
                    b2 = data[offset + 2] & 0xFF;
                }
                int v = (b0 << 16) | (b1 << 8) | b2;
                if ((v & 0x800000) != 0)
                    v |= 0xFF000000; // sign extend
                return v / 8388608f;
            }
            case 32: {
                int b0, b1, b2, b3;
                if (bigEndian) {
                    b0 = data[offset] & 0xFF;
                    b1 = data[offset + 1] & 0xFF;
                    b2 = data[offset + 2] & 0xFF;
                    b3 = data[offset + 3] & 0xFF;
                } else {
                    b0 = data[offset] & 0xFF;
                    b1 = data[offset + 1] & 0xFF;
                    b2 = data[offset + 2] & 0xFF;
                    b3 = data[offset + 3] & 0xFF;
                }
                int v = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
                return v / 2147483648f;
            }
            default:
                return 0f;
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