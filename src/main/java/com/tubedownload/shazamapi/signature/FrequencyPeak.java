package com.tubedownload.shazamapi.signature;

public record FrequencyPeak(
        int fftPassNumber,
        int peakMagnitude,
        int correctedPeakFrequencyBin,
        int sampleRateHz
) {
    public double frequencyHz() {
        return correctedPeakFrequencyBin * (sampleRateHz / 2.0 / 1024.0 / 64.0);
    }

    public double amplitudePcm() {
        return Math.sqrt(Math.exp((peakMagnitude - 6144) / 1477.3) * (1 << 17) / 2.0) / 1024.0;
    }

    public double seconds() {
        return (fftPassNumber * 128.0) / sampleRateHz;
    }
}
