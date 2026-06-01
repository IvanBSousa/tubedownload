package com.tubedownload.sazamAPI.fingerprint;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class JTransformsFFTProcessor
        implements FFTProcessor {

    private static final int WINDOW_SIZE = 2048;

    private final DoubleFFT_1D fft =
            new DoubleFFT_1D(WINDOW_SIZE);

    @Override
    public double[] process(short[] samples) {

        double[] fftData =
                new double[WINDOW_SIZE * 2];

        for (int i = 0; i < samples.length; i++) {
            fftData[i] = samples[i];
        }

        fft.realForwardFull(fftData);

        double[] powers = new double[1025];

        for (int i = 0; i < 1025; i++) {

            double real = fftData[i * 2];
            double imag = fftData[i * 2 + 1];

            powers[i] =
                    Math.max(
                            (real * real + imag * imag)
                                    / (1 << 17),
                            1e-10
                    );
        }

        return powers;
    }
}
