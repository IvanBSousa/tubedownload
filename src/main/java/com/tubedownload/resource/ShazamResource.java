package com.tubedownload.resource;

import com.tubedownload.service.ShazamAPIServices;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/shazam")
@Produces(MediaType.APPLICATION_JSON)
public class ShazamResource {

    @Inject
    ShazamAPIServices service;

    @POST
    @Path("/reconhecer/video/{urlYouTube}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response processaUnicoVideo(@PathParam("urlYouTube") String urlYouTube) {
        if (urlYouTube == null || urlYouTube.isEmpty()) {
            throw new WebApplicationException("Send a valid YouTube URL in the request body.", Response.Status.BAD_REQUEST);
        }
        try {
            service.processarUnicoVideo(urlYouTube);
            return Response.ok().build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @POST
    @Path("/reconhecer/playlist/{urlPlaylist}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response processaPlaylist(@PathParam("urlPlaylist") String urlPlaylist) {
        if (urlPlaylist == null || urlPlaylist.isEmpty()) {
            throw new WebApplicationException("Send a valid YouTube URL in the request body.", Response.Status.BAD_REQUEST);
        }
        try {
            service.processarPlaylist(urlPlaylist);
            return Response.ok().build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
