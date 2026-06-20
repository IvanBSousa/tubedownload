package com.tubedownload.dto;

public record ResponseShazamAPI(
        String titulo,
        String artista,
        String album,
        String urlImage
) {
}
