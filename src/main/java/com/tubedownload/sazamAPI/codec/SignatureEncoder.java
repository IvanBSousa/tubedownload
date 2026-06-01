package com.tubedownload.sazamAPI.codec;

public interface SignatureEncoder {

    byte[] encode(DecodedMessage message);

    String encodeUri(DecodedMessage message);
}
