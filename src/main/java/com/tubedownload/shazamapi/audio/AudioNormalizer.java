package com.tubedownload.shazamapi.audio;

import io.smallrye.config.ConfigMapping;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class AudioNormalizer {
    public static final int NORMALIZED_SAMPLE_WIDTH_BYTES = 2;
    public static final int NORMALIZED_FRAME_RATE = 16000;
    public static final int NORMALIZED_CHANNELS = 1;

    @Inject
    ShazamAudioConfig config;

    public short[] normalize(byte[] audioBytes) throws IOException, InterruptedException {
        Path input = Files.createTempFile("shazam-api-input-", ".audio");
        Path output = Files.createTempFile("shazam-api-output-", ".s16le");
        try {
            Files.write(input, audioBytes);
            Process process = new ProcessBuilder(
                    config.ffmpeg(),
                    "-hide_banner",
                    "-loglevel", "error",
                    "-y",
                    "-i", input.toString(),
                    "-ac", String.valueOf(NORMALIZED_CHANNELS),
                    "-ar", String.valueOf(NORMALIZED_FRAME_RATE),
                    "-f", "s16le",
                    "-acodec", "pcm_s16le",
                    output.toString()
            ).redirectErrorStream(true).start();
            byte[] processOutput = process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("ffmpeg failed with exit code " + exitCode + ": " + new String(processOutput));
            }
            return toLittleEndianSamples(Files.readAllBytes(output));
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(output);
        }
    }

    private short[] toLittleEndianSamples(byte[] pcmBytes) {
        ByteBuffer buffer = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN);
        short[] samples = new short[pcmBytes.length / NORMALIZED_SAMPLE_WIDTH_BYTES];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = buffer.getShort();
        }
        return samples;
    }

    @ConfigMapping(prefix = "shazam")
    public interface ShazamAudioConfig {
        String ffmpeg();
    }
}
