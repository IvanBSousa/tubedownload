package com.tubedownload.sazamAPI.codec;

public interface SignatureDecoder {

    DecodedMessage decode(byte[] data);

    DecodedMessage decodeUri(String uri);
}
