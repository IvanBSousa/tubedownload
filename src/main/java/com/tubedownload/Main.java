package com.tubedownload;

import com.tubedownload.javatube.Playlist;
import com.tubedownload.javatube.StreamQuery;
import com.tubedownload.javatube.Youtube;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.Mp3File;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Main {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    static void main() throws Exception {

        File downloadDir = new File("C:\\Users\\ivanb\\Music\\TESTE\\");

        Youtube yt = new Youtube("https://www.youtube.com/watch?v=CNQasBhFG4Q");
        yt.streams().filter(StreamQuery.Filter.builder()
                .type("audio")
                .abr("128kbps")
                .build()
        ).getFirst().download(downloadDir.getAbsolutePath());

//        ArrayList<String> videoUrls = new ArrayList<>(new Playlist(
//                "https://youtube.com/playlist?list=PLkjk76v4J1ar7czy0q47g5lSXgK5uzrCv")
//                .getVideos());
//
//        for (String videoUrl : videoUrls) {
//            System.out.println(videoUrl);
//        }
//
//        for (int videos = 0; videos < 5; videos++ ) {
//            Youtube yt = new Youtube(videoUrls.get(videos));
//            yt.streams().filter(StreamQuery.Filter.builder()
//                    .type("audio")
//                    .abr("128kbps")
//                    .build()
//            ).getFirst().download("C:\\Users\\ivanb\\Music\\TESTE\\");
//        }

//        Mp3File mp3file = new Mp3File("C:\\Users\\ivanb\\Music\\TESTE\\NOVO\\Bison e Comassetto - Mundão Moderno.mp3");
//        ID3v24Tag id3v24Tag;
//        if (mp3file.hasId3v2Tag()) {
//            id3v24Tag = (ID3v24Tag) mp3file.getId3v2Tag();
//        } else {
//            id3v24Tag = new ID3v24Tag();
//            mp3file.setId3v2Tag(id3v24Tag);
//        }
//        id3v24Tag.setTrack("5");
//        id3v24Tag.setArtist("An Artist");
//        id3v24Tag.setTitle("The Title");
//        id3v24Tag.setAlbum("The Album");
//        id3v24Tag.setYear("2001");
//        id3v24Tag.setGenre(12);
//        id3v24Tag.setComment("Some comment");
//
//        Path imageFile = downloadImage(
//                "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/f0/4a/fa/f04afa9c-a521-2cb2-e71d-49e3c9bc3705/artwork.jpg/400x400cc.jpg",
//                "C:\\Users\\ivanb\\Music\\TESTE\\artwork.jpg"
//        );
//        byte[] imageData = Files.readAllBytes(imageFile);
//
//        id3v24Tag.setAlbumImage(imageData, "image/jpg");
//        mp3file.save("C:\\Users\\ivanb\\Music\\TESTE\\MyMp3File.mp3");
//    }
//
//    private static Path downloadImage(String imageUrl, String targetFile) throws IOException, InterruptedException {
//        HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl))
//                .GET()
//                .build();
//
//        HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
//        if (response.statusCode() < 200 || response.statusCode() >= 300) {
//            throw new IOException("Failed to download image. HTTP " + response.statusCode());
//        }
//
//        Path output = Path.of(targetFile);
//        Path parent = output.getParent();
//        if (parent != null) {
//            Files.createDirectories(parent);
//        }
//        Files.write(output, response.body());
//        return output;
    }
}
