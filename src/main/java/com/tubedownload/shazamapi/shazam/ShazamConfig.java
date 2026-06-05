package com.tubedownload.shazamapi.shazam;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "shazam")
public interface ShazamConfig {
    String lang();
    String region();
    String timezone();
}
