package com.tubedownload.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;

@ApplicationScoped
public class MusicBrainzService {

    private static final String USERNAME = "seu_usuario";
    private static final String PASSWORD = "sua_senha";



    public String buscarGenerico(String artistaETitulo) throws IOException {

        BasicCredentialsProvider credentialsProvider =
                new BasicCredentialsProvider();

        credentialsProvider.setCredentials(
                new AuthScope("musicbrainz.org", 443),
                new UsernamePasswordCredentials(
                        USERNAME,
                        PASSWORD.toCharArray()
                )
        );

        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build()) {

            HttpGet request = new HttpGet(
                    "https://musicbrainz.org/ws/2/recording/?query=" +
                            artistaETitulo +
                            "&fmt=json"
            );

            request.setHeader(
                    "User-Agent",
                    "MinhaAplicacao/1.0 (contato@empresa.com)"
            );

            return client.execute(request,
                    response -> EntityUtils.toString(response.getEntity()));
        }
    }
}