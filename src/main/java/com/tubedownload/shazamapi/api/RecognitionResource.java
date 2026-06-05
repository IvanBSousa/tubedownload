package com.tubedownload.shazamapi.api;


import com.tubedownload.shazamapi.shazam.RecognizeResult;
import com.tubedownload.shazamapi.shazam.ShazamService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.List;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class RecognitionResource {
    @Inject
    ShazamService shazamService;

    @POST
    @Path("/recognize")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public List<RecognizeResult> recognize(byte[] audioBytes) {
        if (audioBytes == null || audioBytes.length == 0) {
            throw new WebApplicationException("Send audio bytes in the request body.", Response.Status.BAD_REQUEST);
        }
        try {
            return shazamService.recognize(audioBytes);
        } catch (IOException exception) {
            throw new WebApplicationException("Unable to process audio: " + exception.getMessage(), exception, Response.Status.BAD_REQUEST);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WebApplicationException("Recognition interrupted.", exception, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
