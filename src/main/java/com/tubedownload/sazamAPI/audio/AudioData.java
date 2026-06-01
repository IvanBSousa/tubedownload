package com.tubedownload.sazamAPI.audio;

public record AudioData(
        short[] samples,
        int sampleRate,
        int channels,
        double durationSeconds
) {
}