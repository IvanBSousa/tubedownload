package com.tubedownload.sazamAPI.audio;

public interface AudioNormalizer {

    AudioData normalize(byte[] audioBytes)
            throws Exception;
}
