package com.app.TorrentClient;

import org.libtorrent4j.SessionManager;
import org.libtorrent4j.Sha1Hash;
import org.libtorrent4j.TorrentHandle;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class App extends JFrame {

    private JPanel center_panel;
    private JTable table;
    static private App.CustomTableModel tableModel;
    private SessionManager main_session_manager;


    static HashMap<Sha1Hash,Torrent> hash_map_torrent = new HashMap<>();

    App(){
        init();
        init_center_panel();


        setVisible(true);
    }


    private JPanel createHeaderPanel(JPanel center_panel) {
        JPanel header = new JPanel();
        header.setPreferredSize(new Dimension(center_panel.getWidth(), 50));
        header.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 11));
        JButton addButton = add_magnet_button(this);
        header.add(addButton);
        resume_pause_torrent(header);
        return header;
    }
    private void init_center_panel(){
        JPanel header = createHeaderPanel(center_panel);

        // Body panel
        JPanel body = new JPanel();
        body.setBackground(Color.PINK);
        body.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.BLACK));
        body.setLayout(new BorderLayout()); // Ensures components fill the entire area
        tableModel = new App.CustomTableModel();
        TableModelContainer tableModelContainer = TableModelContainer.getInstance(tableModel);
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getTableHeader().getColumnModel().getColumn(0).setMaxWidth(0);
        table.getTableHeader().getColumnModel().getColumn(0).setMinWidth(0);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    int modelRow = table.convertRowIndexToModel(selectedRow);
                    CustomTableModel model = (CustomTableModel) table.getModel();
                    Object uniqueId = model.getRowId(modelRow);
                    System.out.println("Selected Row " +  selectedRow +" ID: " + uniqueId);
                }
            }
        });

        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                if (value instanceof Double) setText(String.format("%.2f%%", (Double) value));
                else super.setValue(value);
            }
        });

        body.add(scrollPane, BorderLayout.CENTER);


        // Footer panel
        JPanel footer = new JPanel();
        footer.setPreferredSize(new Dimension(center_panel.getWidth(), 50));
        JLabel footerLabel = new JLabel("Torrent Client © Copyright 2024");
        footerLabel.setHorizontalAlignment(SwingConstants.CENTER); // Center horizontally
        footerLabel.setVerticalAlignment(SwingConstants.CENTER);   // Center vertically
        footer.setLayout(new BorderLayout()); // Use BorderLayout for centering
        footer.add(footerLabel, BorderLayout.CENTER);

        // Add header, body, and footer to center_panel
        center_panel.add(header, BorderLayout.NORTH);
        center_panel.add(body, BorderLayout.CENTER);
        center_panel.add(footer, BorderLayout.SOUTH);
    }
    private void init(){
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(1280,720));
        setMaximumSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width,850));
        setMinimumSize(new Dimension(800,800));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.white);
        center_panel = new JPanel();
        center_panel.setBackground(Color.LIGHT_GRAY);
        center_panel.setLayout(new BorderLayout());
        center_panel.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.BLACK));
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(50, 50, 50, 50));
        wrapper.add(center_panel, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    private static JButton add_magnet_button(JFrame frame) {
        RoundButton add = new RoundButton("Add Magnet");
        add.setPreferredSize(new Dimension(100,25));


        add.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                    add.set_new_border(new Color(0x3e98de));
                    add.set_new_background(new Color(0xb6d3ea));
            }
            @Override public void mouseExited(MouseEvent e) {
                    add.set_new_border(Color.BLACK);
                    add.set_new_background(Color.WHITE);
            }
        });
        add.setBorderPainted(false);
        add.setContentAreaFilled(false);
        add.setFocusPainted(false);
        add.setOpaque(false);

        add.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                JDialog dialog = new JDialog(frame, "Download from torrent hashes", true);
                dialog.setSize(500, 480);
                dialog.setMinimumSize(new Dimension(500, 240));
                dialog.setLayout(new BorderLayout());
                dialog.setLocationRelativeTo(frame);

                JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                headerPanel.setBorder(new EmptyBorder(0, 10, 0, 10)); // Add padding
                JLabel headerLabel = new JLabel("Add torrent links");
                headerPanel.add(headerLabel);
                dialog.add(headerPanel, BorderLayout.NORTH);

                JPanel centerPanel = new JPanel(new BorderLayout());
                centerPanel.setBorder(new EmptyBorder(0, 10, 0, 10)); // Add padding
                JTextArea textArea = new JTextArea();
                textArea.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
                centerPanel.add(textArea, BorderLayout.CENTER);
                dialog.add(centerPanel, BorderLayout.CENTER);

                JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                footerPanel.setBorder(new EmptyBorder(10, 10, 10, 10)); // Add padding

                RoundButton okButton = new RoundButton("Download");
                okButton.setPreferredSize(new Dimension(110,25));
                okButton.addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) {
                        okButton.set_new_border(new Color(0x3e98de));
                        okButton.set_new_background(new Color(0xb6d3ea));
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        okButton.set_new_border(Color.BLACK);
                        okButton.set_new_background(Color.WHITE);
                    }
                });
                okButton.setBorderPainted(false);
                okButton.setContentAreaFilled(false);
                okButton.setFocusPainted(false);
                okButton.setOpaque(false);

                RoundButton cancelButton = new RoundButton("Cancel");
                cancelButton.setPreferredSize(new Dimension(100,25));
                cancelButton.addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) {
                        cancelButton.set_new_border(new Color(0x3e98de));
                        cancelButton.set_new_background(new Color(0xb6d3ea));
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        cancelButton.set_new_border(Color.BLACK);
                        cancelButton.set_new_background(Color.WHITE);
                    }
                });
                cancelButton.setBorderPainted(false);
                cancelButton.setContentAreaFilled(false);
                cancelButton.setFocusPainted(false);
                cancelButton.setOpaque(false);

                footerPanel.add(okButton);
                footerPanel.add(cancelButton);
                dialog.add(footerPanel, BorderLayout.SOUTH);

                // Torrent Download
                okButton.addActionListener(evt -> {
                    String hash = textArea.getText().toString();
                    //87a2d22eb879593b48b3d3ee6828f56e2bfb4415
                    hash_map_torrent.put(Sha1Hash.parseHex(hash),new Torrent(hash));
                    dialog.dispose();
                });

                cancelButton.addActionListener(evt -> {dialog.dispose();});
                dialog.setVisible(true);
            }
        });

        return add;
    }

    private void resume_pause_torrent(JPanel header){
        RoundButton resume_button = new RoundButton("Resume");
        RoundButton pause_button = new RoundButton("Pause");

        pause_button.setBorderPainted(false);
        pause_button.setContentAreaFilled(false);
        pause_button.setFocusPainted(false);
        pause_button.setOpaque(false);

        resume_button.setBorderPainted(false);
        resume_button.setContentAreaFilled(false);
        resume_button.setFocusPainted(false);
        resume_button.setOpaque(false);

        resume_button.setPreferredSize(new Dimension(100,25));
        pause_button.setPreferredSize(new Dimension(100,25));
        resume_button.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) { // Ensure a row is selected
                Sha1Hash hash = Sha1Hash.parseHex(tableModel.getValueAt(selectedRow,0).toString());
                TorrentHandle th = hash_map_torrent.get(hash).torrent_handle;
                if (th != null) {
                    th.resume();
                    System.out.println("Resumed torrent: " + th.torrentFile().name());
                }
            }
        });

        pause_button.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) { // Ensure a row is selected
                Sha1Hash hash = Sha1Hash.parseHex(tableModel.getValueAt(selectedRow,0).toString());
                TorrentHandle th = hash_map_torrent.get(hash).torrent_handle;
                if (th != null) {
                    th.pause();
                    System.out.println("Resumed torrent: " + th.torrentFile().name());
                }
            }
        });

        header.add(resume_button);
        header.add(pause_button);
    }



    public static class CustomTableModel extends AbstractTableModel {
        private final String[] columnNames = {"ID", "Name", "Size", "Progress", "Seeds", "Peers", "Down Speed", "ETA"};
        private List<Object[]> data;

        public CustomTableModel() {
            List<Torrent> torrents = TorrentUtils.getAllTorrents();
            data = new ArrayList<>();
            for (int i = 0; i < torrents.size(); i++) {
                Torrent torrent = torrents.get(i);

                synchronized (hash_map_torrent) {

                    addRow(new Object[]{torrent.sha1hash, torrent.name, torrent.size, null, null, null, null, null});
                    hash_map_torrent.put(Sha1Hash.parseHex(torrent.sha1hash),
                            new Torrent(torrent.name, torrent.size, torrent.sha1hash));
                }


            }
        }

        public void addRow(Object[] row) {
            data.add(row);
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }

        public void removeRow(int rowIndex) {
            data.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }

        @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= data.size() || columnIndex < 0 || columnIndex >= getColumnCount()) {
                return; // Validate the indices
            }
            data.get(rowIndex)[columnIndex] = aValue; // Update the value in the model
            fireTableCellUpdated(rowIndex, columnIndex); // Notify the table of the update
        }

        @Override public int getRowCount() {
            return data.size(); // Return the size of the list
        }

        @Override public int getColumnCount() {
            return columnNames.length;
        }

        @Override public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override public Object getValueAt(int rowIndex, int columnIndex) {
            Object value = data.get(rowIndex)[columnIndex];
            if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                return "";
            }
            return value;
        }

        public Object getRowId(int rowIndex) {
            return data.get(rowIndex)[0]; // Assuming the first column contains the ID
        }
    }


}
