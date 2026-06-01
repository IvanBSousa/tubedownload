package com.tubedownload.resource;

import com.tubedownload.service.MusicBrainzService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/musicbrainz")
public class MusicBrainzResource {

    @Inject
    MusicBrainzService service;

    @GET
    @Path("/recording/{artistaETitulo}")
    @Produces(MediaType.APPLICATION_JSON)
    public String buscar(@PathParam("artistaETitulo") String artistaETitulo) throws Exception {

        return service.buscarGenerico(artistaETitulo);
    }
}