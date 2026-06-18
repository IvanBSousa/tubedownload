package com.tubedownload.javatube.exceptions;

public class VideoPrivateError extends Exception{
    public VideoPrivateError(String videoId) {
        super(videoId + " is a private video");
    }
}
