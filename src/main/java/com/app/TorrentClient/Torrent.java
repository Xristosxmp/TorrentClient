package com.app.TorrentClient;

import org.libtorrent4j.*;
import org.libtorrent4j.alerts.AddTorrentAlert;
import org.libtorrent4j.alerts.Alert;
import org.libtorrent4j.alerts.PieceFinishedAlert;
import org.libtorrent4j.alerts.TorrentFinishedAlert;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Torrent implements Serializable {
    private static final long serialVersionUID = 1L;

    public String name, size, sha1hash;

    // Transient fields to avoid serialization issues
    private transient SessionManager session;
    private transient TorrentHandle torrent_handle;
    private transient int current_row;
    private transient App.CustomTableModel tableModel;

    public Torrent(String sha1hash) {
        this.sha1hash = sha1hash;
        download();
    }

    public Torrent(String name, String size, String sha1hash) {
        this.name = name;
        this.size = size;
        this.sha1hash = sha1hash;
        download();
    }

    public void save() throws IOException {
        Path torrentsDir = Paths.get("torrents");
        if (!Files.exists(torrentsDir)) Files.createDirectories(torrentsDir);
        Path filePath = torrentsDir.resolve(sha1hash + ".ser");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath.toFile()))) {
            oos.writeObject(this);
        }
    }

    private void append(String name, String size, String sha1hash) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                TableModelContainer tableModelContainer = TableModelContainer.getInstance(null); // Null since it’s already initialized
                tableModel = tableModelContainer.getTableModel();
                tableModel.addRow(new Object[]{sha1hash, name, size, null, null, null, null, null});
                current_row = tableModel.getRowCount() - 1;
                return null;
            }
        };

        worker.execute();
    }

    private void download() {
        System.out.println(this.sha1hash);
        String hash = "magnet:?xt=urn:btih:" + this.sha1hash;
        new Thread(() -> {
            try {
                session = new SessionManager();
                session.start();
                session.addListener(new TorrentAlert());
                try {
                    waitForNodesInDHT(session);
                } catch (InterruptedException e) {
                    return;
                }
                File output_folder = new File(System.getProperty("user.home"), "Downloads");
                byte[] data = session.fetchMagnet(hash, 10, output_folder);
                TorrentInfo ti = TorrentInfo.bdecode(data);

                this.size = get_size(ti.totalSize());
                this.name = ti.name();
                new Thread(() -> {
                    try {
                        save();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
                append(this.name, this.size, this.sha1hash);

                Priority[] priorities = Priority.array(Priority.DEFAULT, ti.numFiles());
                session.download(ti, output_folder, null, priorities, null, TorrentFlags.SEQUENTIAL_DOWNLOAD);
                torrent_handle = session.find(ti.infoHash());
                torrent_handle.unsetFlags(TorrentFlags.AUTO_MANAGED);
                Thread torrentThread = new Thread(new TorrentThread());
                torrentThread.start();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                removeRowOnError();
            }
        }).start();
    }

    public static String get_size(long bytes) {
        if (bytes < 1024) return bytes + " Bytes";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String[] units = {"Bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), units[exp]);
    }

    private void removeRowOnError() {
        TableModelContainer tableModelContainer = TableModelContainer.getInstance(null);
        App.CustomTableModel tableModel = tableModelContainer.getTableModel();
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    if (tableModel.getValueAt(i, 0).equals(sha1hash)) {
                        tableModel.removeRow(i);
                        break;
                    }
                }
                return null;
            }
        };
        worker.execute();
    }

    private static void waitForNodesInDHT(final SessionManager s) throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long nodes = s.stats().dhtNodes();
                if (nodes >= 10) {
                    signal.countDown();
                    timer.cancel();
                }
            }
        }, 0, 1000);

        boolean r = signal.await(10, TimeUnit.SECONDS);
        if (!r) {
            waitForNodesInDHT(s);
        }
    }

    public static String calculateETA(TorrentHandle torrentHandle) {
        long totalSize = torrentHandle.torrentFile().totalSize();
        double progress = torrentHandle.status().progress();
        long remainingSize = (long) (totalSize * (1 - progress));
        int downloadSpeed = torrentHandle.status().downloadRate();
        if (downloadSpeed <= 0) return "Unknown";
        long etaInSeconds = remainingSize / downloadSpeed;
        return formatTime(etaInSeconds);
    }

    public static String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) return String.format("%02d:%02d:%02d", hours, minutes, secs);
        else return String.format("%02d:%02d", minutes, secs);
    }


    public static String formatSpeed(int bytesPerSecond) {
        if (bytesPerSecond < 1024) return bytesPerSecond + " Bytes/s";
        int exp = (int) (Math.log(bytesPerSecond) / Math.log(1024));
        String[] units = {"Bytes/s", "KB/s", "MB/s", "GB/s", "TB/s"};
        return String.format("%.2f %s", bytesPerSecond / Math.pow(1024, exp), units[exp]);
    }

    private class TorrentThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    if (tableModel != null) {
                        if (torrent_handle != null) {
                            if (torrent_handle.isValid()) {

                                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                                    @Override
                                    protected Void doInBackground() {
                                        double progress = torrent_handle.status().progress() * 100;
                                        int peers = torrent_handle.status().numPeers();
                                        int seeds = torrent_handle.status().numSeeds();
                                        String down_speed = formatSpeed(torrent_handle.status().downloadRate());
                                        String eta = calculateETA(torrent_handle);
                                        tableModel.setValueAt(progress, current_row, 3);
                                        tableModel.setValueAt(seeds, current_row, 4);
                                        tableModel.setValueAt(peers, current_row, 5);
                                        tableModel.setValueAt(down_speed, current_row, 6);
                                        tableModel.setValueAt(eta, current_row, 7);
                                        return null;
                                    }
                                };

                                worker.execute();


                            }
                        }
                    }
                    // Sleep for 1 second before running again
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("TorrentThread interrupted.");
                    break; // Exit the loop if interrupted
                }
            }
        }
    }



    private class TorrentAlert implements AlertListener {
        @Override public int[] types() {return null;}

        @Override
        public void alert(Alert<?> alert) {
            switch (alert.type()) {
                case ADD_TORRENT:
                    ((AddTorrentAlert) alert).handle().resume();
                    break;
                case TORRENT_FINISHED:
                    ((TorrentFinishedAlert) alert).handle().pause();
                    break;
                case METADATA_RECEIVED:
                    System.out.println("metadata received (" + sha1hash + ")");
                    break;
                default:
                    break;
            }
        }
    }
}
