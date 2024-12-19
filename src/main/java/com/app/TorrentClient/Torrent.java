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
    public String name,size,sha1hash;
    public Torrent(String sha1hash) {this.sha1hash = sha1hash;}

    public Torrent(String name, String size, String sha1hash) {
        this.name = name;
        this.size = size;
        this.sha1hash = sha1hash;
        download();
        try {save();} catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() throws IOException {
        Path torrentsDir = Paths.get("torrents");
        if (!Files.exists(torrentsDir)) Files.createDirectories(torrentsDir);
        Path filePath = torrentsDir.resolve(sha1hash + ".ser");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath.toFile()))) {
            oos.writeObject(this);
        }
    }


    private void append(String name,String size,String sha1hash) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                TableModelContainer tableModelContainer = TableModelContainer.getInstance(null); // Null since it’s already initialized
                App.CustomTableModel tableModel = tableModelContainer.getTableModel();
                tableModel.addRow(new Object[] {sha1hash,name,size,null,null,null,null,null});
                return null;
            }
        };

        worker.execute();
    }



    public SessionManager session;
    public TorrentHandle torrent_handle;
    private void download(){
        System.out.println(this.sha1hash);
        String hash = "magnet:?xt=urn:btih:"+this.sha1hash;
        new Thread(() ->{
            try {

                session = new SessionManager();
                session.start();
                session.addListener(new TorrentAlert());
                try {waitForNodesInDHT(session);} catch (InterruptedException e) {return;}
                File output_folder = new File(System.getProperty("user.home"), "Downloads");
                byte[] data = session.fetchMagnet(hash, 10, output_folder);
                TorrentInfo ti = TorrentInfo.bdecode(data);

                append(ti.name(),get_size(ti.totalSize()),this.sha1hash);
                Priority[] priorities = Priority.array(Priority.DEFAULT, ti.numFiles());
                session.download(ti, output_folder, null, priorities, null, TorrentFlags.SEQUENTIAL_DOWNLOAD);
                torrent_handle = session.find(ti.infoHash());
                torrent_handle.unsetFlags(TorrentFlags.AUTO_MANAGED);
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
        final java.util.Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override public void run() {
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

    private class TorrentAlert implements AlertListener{
        @Override public int[] types() {return null;}
        @Override public void alert(Alert<?> alert) {
            switch (alert.type()) {
                case ADD_TORRENT: ((AddTorrentAlert) alert).handle().resume(); break;
                case PIECE_FINISHED: break;
                case TORRENT_FINISHED: ((TorrentFinishedAlert) alert).handle().pause();   break;
                case METADATA_RECEIVED:
                    System.out.println("metadata received (" + sha1hash + ")"); break;
                default: break;
            }
        }
    }

}
