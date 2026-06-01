package com.tubedownload.sazamAPI.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.seuprojeto.shazam.model.DecodedMessage;

public interface ShazamClient {

    JsonNode recognize(
            DecodedMessage signature
    ) throws Exception;
}
