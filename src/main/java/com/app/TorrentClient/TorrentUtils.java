package com.app.TorrentClient;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class TorrentUtils {
    private static final long serialVersionUID = 4518538560501954162L;  // Ensure this matches on both versions.

    public static List<Torrent> getAllTorrents() {
        List<Torrent> torrents = new ArrayList<>();
        Path torrentsDir = Paths.get("torrents");
        if (Files.exists(torrentsDir)) {
            // Get all .ser files in the torrents directory
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(torrentsDir, "*.ser")) {
                for (Path file : stream) {
                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file.toFile()))) {
                        Torrent torrent = (Torrent) ois.readObject();

                        torrents.add(torrent);
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {e.printStackTrace();}
        } else {
            System.out.println("The 'torrents' directory does not exist.");
        }

        return torrents;
    }

}
