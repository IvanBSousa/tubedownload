package com.tubedownload.shazamapi.shazam;


import com.tubedownload.shazamapi.audio.AudioNormalizer;
import com.tubedownload.shazamapi.signature.DecodedMessage;
import com.tubedownload.shazamapi.signature.SignatureGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ShazamService {
    private static final double MAX_TIME_SECONDS = 8;

    @Inject
    AudioNormalizer audioNormalizer;

    @Inject
    ShazamClient shazamClient;

    public List<RecognizeResult> recognize(byte[] audioBytes) throws IOException, InterruptedException {
        short[] samples = audioNormalizer.normalize(audioBytes);
        SignatureGenerator signatureGenerator = createSignatureGenerator(samples);
        List<RecognizeResult> results = new ArrayList<>();
        for (DecodedMessage signature : signatureGenerator) {
            int currentOffset = signatureGenerator.samplesProcessed() / AudioNormalizer.NORMALIZED_FRAME_RATE;
            var response = shazamClient.recognize(signature);
            results.add(new RecognizeResult(currentOffset, response));
            if (isMatch(response)) {
                break;
            }
        }
        return results;
    }

    private boolean isMatch(com.fasterxml.jackson.databind.JsonNode response) {
        return response != null && response.hasNonNull("track");
    }

    private SignatureGenerator createSignatureGenerator(short[] samples) {
        SignatureGenerator signatureGenerator = new SignatureGenerator();
        signatureGenerator.feedInput(samples);
        signatureGenerator.maxTimeSeconds(MAX_TIME_SECONDS);
        double durationSeconds = samples.length / (double) AudioNormalizer.NORMALIZED_FRAME_RATE;
        if (durationSeconds > 12 * 3) {
            signatureGenerator.addSamplesProcessed(AudioNormalizer.NORMALIZED_FRAME_RATE * ((int) (durationSeconds / 16) - 6));
        }
        return signatureGenerator;
    }
}
