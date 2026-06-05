package com.tubedownload.resource;

import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;
import com.tubedownload.service.ShazamAPIServices;
import com.tubedownload.shazamapi.shazam.RecognizeResult;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.List;

@Path("/shazam")
@Produces(MediaType.APPLICATION_JSON)
public class ShazamResource {

    @Inject
    ShazamAPIServices service;

    @POST
    @Path("/reconhecer/{urlYouTube}")
    @Consumes(MediaType.TEXT_PLAIN)
public List<RecognizeResult> recognize(@PathParam("urlYouTube") String urlYouTube) {
        if (urlYouTube == null || urlYouTube.isEmpty()) {
            throw new WebApplicationException("Send a valid YouTube URL in the request body.", Response.Status.BAD_REQUEST);
        }
        try {
            return service.baixarConverterReconhecer(urlYouTube);
        } catch (IOException exception) {
            throw new WebApplicationException("Unable to process audio: " + exception.getMessage(), exception, Response.Status.BAD_REQUEST);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WebApplicationException("Recognition interrupted.", exception, Response.Status.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
