package com.tubedownload.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tubedownload.javatube.Playlist;
import com.tubedownload.javatube.StreamQuery;
import com.tubedownload.javatube.Youtube;
import com.mpatric.mp3agic.*;
import com.tubedownload.dto.ResponseShazamAPI;
import com.tubedownload.shazamapi.shazam.RecognizeResult;
import com.tubedownload.shazamapi.shazam.ShazamService;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@ApplicationScoped
public class ShazamAPIServices {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private final ShazamService shazamService;
    File outputDir = new File("C:\\Users\\ivanb\\Music\\TESTE\\NOVO\\");
    File downloadDir = new File("C:\\Users\\ivanb\\Music\\TESTE\\");

    public ShazamAPIServices(ShazamService shazamService) {
        this.shazamService = shazamService;
    }

    public ResponseShazamAPI processarUnicoVideo(String urlYoutube) throws Exception {

        File inputFile = baixarYoutube(urlYoutube);

        File outputFile = new File(outputDir, stripExtension(inputFile.getName()) + ".mp3");
        System.out.println("Input file: " + inputFile.getAbsolutePath());

        byte[] mp3Data = getMp3Data(inputFile, outputFile);

        inserirTags(outputFile.getAbsolutePath(), reconhecerShazam(mp3Data));

        return reconhecerShazam(mp3Data);
    }

    public void processarPlaylist(String urlPlaylist) throws Exception {
        ArrayList<String> videoUrls = new ArrayList<>(new Playlist(
                urlPlaylist).getVideos());

        if (videoUrls.isEmpty()) {
            throw new IOException("Playlist sem videos. URL recebida: " + urlPlaylist);
        }

        for (String videoUrl : videoUrls) {
            System.out.println("Playlist item: " + videoUrl);
        }

        for (int videos = 0; videos < 5; videos++) {
            System.out.println("Processando indice " + videos + ": " + videoUrls.get(videos));
            File inputFile = null;
            File outputFile = null;
            try {
                inputFile = baixarYoutube(videoUrls.get(videos));

                outputFile = new File(outputDir, stripExtension(inputFile.getName()) + ".mp3");
                System.out.println("Input file: " + inputFile.getAbsolutePath());

                byte[] mp3Data = getMp3Data(inputFile, outputFile);
                ResponseShazamAPI response = reconhecerShazam(mp3Data);

                inserirTags(outputFile.getAbsolutePath(), response);
            } catch (IOException e) {
                System.out.println("Pulando video " + videos + ": " + e.getMessage());
            } catch (IndexOutOfBoundsException e) {
                System.out.println("Pulando video " + videos + ": resposta do Shazam incompleta");
            } //finally {
//                if (outputFile != null) {
//                    Files.deleteIfExists(outputFile.toPath());
//                }
//                if (inputFile != null) {
//                    Files.deleteIfExists(inputFile.toPath());
//                }
//            }
        }
    }

    public void processarArquivos(String urlDiretorio) throws Exception {

        File inputDir = new File(urlDiretorio);

        for (File file : Objects.requireNonNull(inputDir.listFiles())) {
            byte[] mp3Data = Files.readAllBytes(file.toPath());

            //inserirTags(file.getAbsolutePath(), reconhecerShazam(mp3Data));
        }


    }

    private ResponseShazamAPI reconhecerShazam(byte[] mp3Data) throws Exception {
        List<RecognizeResult> result = shazamService.recognize(mp3Data);
        if (result.isEmpty()) {
            throw new IOException("No recognition result from Shazam API");
        }

        JsonNode response = result.getFirst().response();
        JsonNode track = response.path("track");
        JsonNode sections = track.path("sections");
        JsonNode metadata = track.path("metadata");

        if (track.isMissingNode() || track.isNull()) {
            throw new IOException("Resposta do Shazam sem dados suficientes para a musica");
        }
        if (sections.isMissingNode() || sections.isNull() || sections.isEmpty()) {
            throw new IOException("Resposta do Shazam sem sections");
        }
//        if (metadata.isMissingNode() || metadata.isNull()) {
//            throw new IOException("Resposta do Shazam sem metadata");
//        }

        return new ResponseShazamAPI(
                track.path("title").asText(),
                track.path("subtitle").asText(),
                track.path("sections").get(0).path("metadata").get(0).path("text").asText(),
                track.path("images").path("coverart").asText()
        );
    }

    private void inserirTags(String pathArquivo, ResponseShazamAPI response) throws InvalidDataException, UnsupportedTagException, IOException, InterruptedException, NotSupportedException {
        Mp3File mp3file = new Mp3File(pathArquivo);
        ID3v24Tag id3v24Tag;
        if (mp3file.hasId3v2Tag()) {
            id3v24Tag = (ID3v24Tag) mp3file.getId3v2Tag();
        } else {
            id3v24Tag = new ID3v24Tag();
            mp3file.setId3v2Tag(id3v24Tag);
        }
        id3v24Tag.setTitle(response.titulo());
        id3v24Tag.setArtist(response.artista());
        id3v24Tag.setAlbum(response.album());

        Path imageFile = downloadImage(
                response.urlImage()
        );
        byte[] imageData = Files.readAllBytes(imageFile);

        id3v24Tag.setAlbumImage(imageData, "image/jpg");
        mp3file.save("C:\\Users\\ivanb\\Music\\TESTE\\NOVO\\" + response.artista() + " - " + response.titulo() + ".mp3");

        Files.deleteIfExists(Path.of(pathArquivo));
        Files.deleteIfExists(imageFile);
    }

    private static Path downloadImage(String imageUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl))
                .GET()
                .build();

        HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Failed to download image. HTTP " + response.statusCode());
        }

        Path output = Path.of("C:\\Users\\ivanb\\Music\\TESTE\\artwork.jpg");
        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(output, response.body());
        return output;
    }

    private File baixarYoutube(String urlVideo) throws Exception {
        Youtube yt = new Youtube(urlVideo);
        File downloadDir = new File("C:\\Users\\ivanb\\Music\\TESTE\\");

        Files.createDirectories(downloadDir.toPath());
        clearDirectory(downloadDir.toPath());

        String downloadPath = ensureTrailingSeparator(downloadDir.getAbsolutePath());
        System.out.println("Baixando URL: " + urlVideo);
        System.out.println("Baixando para: " + downloadPath);

        yt.streams().filter(StreamQuery.Filter.builder()
                .type("audio")
                .abr("128kbps")
                .build()
        ).getFirst().download(downloadPath);

        return findNewestFile(downloadDir.toPath())
                .orElseThrow(() -> new IOException("No downloaded file found in: " + downloadDir.getAbsolutePath()));
    }

    private static byte[] getMp3Data(File inputFile, File outputFile) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", inputFile.getAbsolutePath(),
                "-vn",
                "-acodec", "libmp3lame",
                "-b:a", "128k",
                "-y",
                outputFile.getAbsolutePath()
        );
        pb.redirectErrorStream(true);

        Process p = pb.start();
        String ffmpegLog;
        try (InputStream is = p.getInputStream()) {
            ffmpegLog = new String(is.readAllBytes());
        }
        p.waitFor();

        if (p.exitValue() != 0) {
            throw new IOException("ffmpeg failed with exit code " + p.exitValue() + " for " + inputFile.getAbsolutePath() + "\n" + ffmpegLog);
        }
        if (!outputFile.exists()) {
            throw new IOException("Converted file was not created: " + outputFile.getAbsolutePath());
        }

        System.out.println("Process finished with exit code " + p.exitValue() + " arquivo convertido " + outputFile.getAbsolutePath());
        return Files.readAllBytes(outputFile.toPath());
    }

    private static Optional<File> findNewestFile(Path directory) throws IOException {
        try (var stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .max(Comparator.comparingLong(path -> path.toFile().lastModified()))
                    .map(Path::toFile);
        }
    }

    private static void clearDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var stream = Files.list(directory)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (Files.isRegularFile(path)) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }

    private static String ensureTrailingSeparator(String path) {
        if (path.endsWith(File.separator)) {
            return path;
        }
        return path + File.separator;
    }
}
