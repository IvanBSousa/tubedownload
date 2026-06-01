package com.tubedownload;

import com.github.felipeucelli.javatube.StreamQuery;
import com.github.felipeucelli.javatube.Youtube;
import com.mpatric.mp3agic.*;
import com.nextbreakpoint.ffmpeg4java.*;

import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
//        Youtube yt = new Youtube("https://youtu.be/drrfJ-VYsr4?si=KzlTX4ndTtelC5sc");
//        //yt.streams().getAll().forEach(System.out::println);
//        yt.streams().filter(StreamQuery.Filter.builder()
//                .type("audio")
//                .abr("128kbps")
//                .build()
//        ).getFirst().download("C:\\Users\\ivanb\\Music\\TESTE\\");

//        File inputFile = new File("C:\\Users\\ivanb\\Music\\TESTE\\teste.mp4");
//        File outputFile = new File("C:\\Users\\ivanb\\Music\\TESTE\\teste_mp3.mp3");
//
//        ProcessBuilder pb = new ProcessBuilder(
//                "ffmpeg",
//                "-i", inputFile.getAbsolutePath(), // Arquivo de entradaada
//                "-vn",                              // Desativa o fluxo de vídeo
//                "-acodec", "libmp3lame",            // Codec de áudio MP3
//                "-b:a", "128",                        // Qualidade alta (VBR de ~190 kbps)
//                "-y",                               // Sobrescreve o arquivo de saída se já existir
//                outputFile.getAbsolutePath()       // Arquivo de saída
//        );
//
//        Process p = pb.start();
//        p.waitFor();

        //MP3File mp3file = new MP3File("C:\\Users\\ivanb\\Music\\TESTE\\teste_mp3.mp3");
        //TagOptionSingleton.getInstance().setDefaultSaveMode(TagConstant.MP3_FILE_SAVE_OVERWRITE);


// setup id3v2
//        ID3v2_4 id3v2 = new ID3v2_4();
//        id3v2.setSongTitle("Se Ze Rico Estivesse Vivo");
//        id3v2.setLeadArtist("Bison e Comassetto");
//        id3v2.setAlbumTitle("Album Title");
//        mp3file.setID3v2Tag(id3v2);
//        mp3file.save();

        // setup id3v1
//        ID3v1_1 id3v1 = new ID3v1_1();
//        id3v1.setSongTitle("Se Ze Rico Estivesse Vivo");
//        id3v1.setLeadArtist("Bison e Comassetto");
//        id3v1.setAlbumTitle("Album Title");
//        mp3file.setID3v1Tag(id3v1);
//        mp3file.save();

        Mp3File mp3file = new Mp3File("C:\\Users\\ivanb\\Music\\TESTE\\teste_mp3.mp3");
        ID3v24Tag id3v24Tag;
        if (mp3file.hasId3v2Tag()) {
            id3v24Tag = (ID3v24Tag) mp3file.getId3v2Tag();
        } else {
            // mp3 does not have an ID3v2 tag, let's create one..
            id3v24Tag = new ID3v24Tag();

            mp3file.setId3v2Tag(id3v24Tag);
        }
        id3v24Tag.setTrack("5");
        id3v24Tag.setArtist("An Artist");
        id3v24Tag.setTitle("The Title");
        id3v24Tag.setAlbum("The Album");
        id3v24Tag.setYear("2001");
        id3v24Tag.setGenre(12);
        id3v24Tag.setComment("Some comment");

        File imageFile = new File("C:\\Users\\ivanb\\Music\\TESTE\\teste.jpg");
        byte[] imageData = new byte[(int) imageFile.length()];
//        try (var fis = new java.io.FileInputStream(imageFile)) {
//            fis.read(imageData);
//        }

        id3v24Tag.setAlbumImage(imageData, "image/jpeg");

        mp3file.save("C:\\Users\\ivanb\\Music\\TESTE\\MyMp3File.mp3");

    }
}
