package com.tubedownload.shazamapi.shazam;

import com.fasterxml.jackson.databind.JsonNode;

public record RecognizeResult(int offsetSeconds, JsonNode response) {
}
