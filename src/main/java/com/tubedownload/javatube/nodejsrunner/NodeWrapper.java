//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.tubedownload.javatube.nodejsrunner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NodeWrapper {
    private static Path nodeBinary;

    public NodeWrapper() {
    }

    public static synchronized Path ensureNode() throws IOException {
        if (nodeBinary == null) {
            nodeBinary = Downloader.downloadIfNeeded();
        }

        return nodeBinary;
    }

    public static Process run(String... args) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(ensureNode().toString());
        cmd.addAll(Arrays.asList(args));
        return (new ProcessBuilder(cmd)).inheritIO().start();
    }
}
