package com.tubedownload.javatube.nodejsrunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public class Downloader {
    private static final OkHttpClient client = new OkHttpClient();

    public Downloader() {
    }

    public static Path downloadIfNeeded() throws IOException {
        String platform = detectPlatform();
        Path cacheDir = Paths.get(System.getProperty("user.home"), ".nodejswrapper", platform);
        String bin = "node";
        String suffix = "";
        if (platform.equals("win_x64")) {
            suffix = ".exe";
        }

        Path nodePath = cacheDir.resolve(bin + suffix);
        if (Files.exists(nodePath, new LinkOption[0])) {
            return nodePath;
        } else {
            Files.createDirectories(cacheDir);
            String url = String.format("https://github.com/felipeucelli/nodejs_wrapper/releases/download/binaries/node_%s%s.tar.gz", platform, suffix);
            Path tarGz = cacheDir.resolve(bin + "_" + platform + suffix + ".tar.gz");
            Request request = (new Request.Builder()).url(url).build();

            try (Response resp = client.newCall(request).execute()) {
                if (!resp.isSuccessful()) {
                    throw new IOException("Download failed: " + resp);
                }

                assert resp.body() != null;

                Files.copy(resp.body().byteStream(), tarGz, new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
            }

            TarArchiveEntry entry;
            try (
                    InputStream fi = Files.newInputStream(tarGz);
                    GzipCompressorInputStream gzi = new GzipCompressorInputStream(fi);
                    TarArchiveInputStream tar = new TarArchiveInputStream(gzi);
            ) {
                while((entry = tar.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        Files.copy(tar, nodePath, new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
                        break;
                    }
                }
            }

            try {
                Files.setPosixFilePermissions(nodePath, PosixFilePermissions.fromString("rwxr-xr-x"));
            } catch (UnsupportedOperationException var17) {
            }

            return nodePath;
        }
    }

    private static String detectPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        if (os.contains("win")) {
            return "win_x64";
        } else if (os.contains("mac")) {
            return !arch.contains("aarch64") && !arch.contains("arm") ? "darwin_x64" : "darwin_arm64";
        } else if (!os.contains("nux")) {
            throw new RuntimeException("Unsupported platform: " + os + " " + arch);
        } else {
            return !arch.contains("aarch64") && !arch.contains("arm") ? "linux_x64" : "linux_arm64";
        }
    }
}
