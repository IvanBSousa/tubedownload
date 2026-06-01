package com.tubedownload.sazamAPI.fingerprint;

import

public interface PeakDetector {

    void detect(
            RingBuffer<double[]> fftOutputs,
            RingBuffer<double[]> spreadOutputs,
            DecodedMessage message
    );
}