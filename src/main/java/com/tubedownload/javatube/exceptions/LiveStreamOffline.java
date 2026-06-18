package com.tubedownload.javatube.exceptions;

public class LiveStreamOffline extends Exception{
    public LiveStreamOffline(String videoId, String reason){
        super(videoId + " " + reason);
    }
}
