package com.tubedownload.shazamapi.signature;

import java.util.Arrays;

public enum FrequencyBand {
    BAND_0_250(-1),
    BAND_250_520(0),
    BAND_520_1450(1),
    BAND_1450_3500(2),
    BAND_3500_5500(3);

    private final int id;

    FrequencyBand(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static FrequencyBand fromId(int id) {
        return Arrays.stream(values())
                .filter(band -> band.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown frequency band id: " + id));
    }
}
