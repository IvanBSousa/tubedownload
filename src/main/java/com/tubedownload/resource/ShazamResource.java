package com.tubedownload.resource;

import com.tubedownload.dto.ResponseShazamAPI;
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
    @Path("/reconhecer/video/{urlVideo}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response processaUnicoVideo(@PathParam("urlVideo") String urlVideo) {
        if (urlVideo == null || urlVideo.isEmpty()) {
            throw new WebApplicationException("Send a valid YouTube URL in the request body.", Response.Status.BAD_REQUEST);
        }
        try {
            ResponseShazamAPI response = service.processarUnicoVideo(urlVideo);
            return Response.ok(response).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @POST
    @Path("/reconhecer/playlist/{urlPlaylist}/{primeiroVideo}/{ultimoVideo}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response processaPlaylist(@PathParam("urlPlaylist") String urlPlaylist,
                                     @PathParam("primeiroVideo") int primeiroVideo,
                                     @PathParam("ultimoVideo") int ultimoVideo) {
        if (urlPlaylist == null || urlPlaylist.isEmpty()) {
            throw new WebApplicationException("Send a valid YouTube URL in the request body.", Response.Status.BAD_REQUEST);
        }
        try {
            service.processarPlaylist(urlPlaylist, primeiroVideo, ultimoVideo);
            return Response.ok().build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @POST
    @Path("/reconhecer/diretorio/{urlDiretorio}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response processaDiretorio(@PathParam("urlDiretorio") String urlDiretorio) {
        if (urlDiretorio == null || urlDiretorio.isEmpty()) {
            throw new WebApplicationException("Send a valid directory path in the request body.", Response.Status.BAD_REQUEST);
        }
        try {
            service.processarArquivos(urlDiretorio);
            return Response.ok().build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
