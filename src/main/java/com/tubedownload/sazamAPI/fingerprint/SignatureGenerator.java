package com.tubedownload.sazamAPI.fingerprint;

import java.util.ArrayList;
import java.util.List;

public class SignatureGenerator {

    private final FFTProcessor fftProcessor;

    private final PeakDetector peakDetector;

    private final RingBuffer<Short> sampleBuffer =
            new RingBuffer<>(2048, (short) 0);

    private final RingBuffer<double[]> fftOutputs =
            new RingBuffer<>(256, new double[1025]);

    private final RingBuffer<double[]> spreadOutputs =
            new RingBuffer<>(256, new double[1025]);

    public SignatureGenerator(
            FFTProcessor fftProcessor,
            PeakDetector peakDetector) {

        this.fftProcessor = fftProcessor;
        this.peakDetector = peakDetector;
    }

    public DecodedMessage generate(
            short[] samples) {

        DecodedMessage message =
                DecodedMessage.empty();

        List<Short> pending =
                new ArrayList<>();

        for (short sample : samples) {
            pending.add(sample);
        }

        processSamples(
                pending,
                message
        );

        return message;
    }

    private void processSamples(
            List<Short> samples,
            DecodedMessage message) {

        for (int i = 0;
             i + 128 <= samples.size();
             i += 128) {

            short[] chunk =
                    new short[128];

            for (int j = 0; j < 128; j++) {
                chunk[j] = samples.get(i + j);
            }

            double[] fft =
                    fftProcessor.process(chunk);

            fftOutputs.append(fft);

            peakDetector.detect(
                    fftOutputs,
                    spreadOutputs,
                    message
            );
        }
    }
}
