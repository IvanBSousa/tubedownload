package com.tubedownload.service;

import com.github.felipeucelli.javatube.StreamQuery;
import com.github.felipeucelli.javatube.Youtube;
import com.tubedownload.shazamapi.shazam.RecognizeResult;
import com.tubedownload.shazamapi.shazam.ShazamService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ShazamAPIServices {

    private final ShazamService shazamService;

    public ShazamAPIServices(ShazamService shazamService) {
        this.shazamService = shazamService;
    }

    public List<RecognizeResult> baixarConverterReconhecer(String urlYoutube) throws Exception {
        Youtube yt = new Youtube(urlYoutube);
        File downloadDir = new File("C:\\Users\\ivanb\\Music\\TESTE");
        File outputDir = new File("C:\\Users\\ivanb\\Music\\TESTE\\NOVO");
        downloadDir.mkdirs();
        outputDir.mkdirs();
        clearDirectory(outputDir.toPath());

        yt.streams().filter(StreamQuery.Filter.builder()
                .type("audio")
                .abr("128kbps")
                .build()
        ).getFirst().download(downloadDir.getAbsolutePath());

        File inputFile = findNewestFile(downloadDir.toPath())
                .orElseThrow(() -> new IOException("No downloaded file found in: " + downloadDir.getAbsolutePath()));

        File outputFile = new File(outputDir, stripExtension(inputFile.getName()) + ".mp3");
        System.out.println("Input file: " + inputFile.getAbsolutePath());

        byte[] mp3Data = getMp3Data(inputFile, outputFile);
        return shazamService.recognize(mp3Data);
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
}
