package com.tubedownload.resource;

import com.tubedownload.dto.ResponseShazamAPI;
import com.tubedownload.service.ShazamAPIServices;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;

@Path("/shazam")
@Produces(MediaType.APPLICATION_JSON)
public class ShazamResource {

    @Inject
    ShazamAPIServices service;

    @POST
    @Path("/reconhecer/{urlYouTube}")
    @Consumes(MediaType.TEXT_PLAIN)
public Response recognize(@PathParam("urlYouTube") String urlYouTube) {
        if (urlYouTube == null || urlYouTube.isEmpty()) {
            throw new WebApplicationException("Send a valid YouTube URL in the request body.", Response.Status.BAD_REQUEST);
        }
        try {
            service.baixarConverterReconhecerInserirTAG(urlYouTube);
            return Response.ok().build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
