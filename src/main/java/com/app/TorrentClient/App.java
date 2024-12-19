package com.app.TorrentClient;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class App extends JFrame {

    private JPanel center_panel;

    App(){
        init();
        init_center_panel();


        setVisible(true);
    }


    private JPanel createHeaderPanel(JPanel center_panel) {
        JPanel header = new JPanel();
        header.setPreferredSize(new Dimension(center_panel.getWidth(), 50));
        header.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 11));
        JButton addButton = add_magnet_button();
        header.add(addButton);
        return header;
    }
    private void init_center_panel(){
        JPanel header = createHeaderPanel(center_panel);

        // Body panel
        JPanel body = new JPanel();
        body.setBackground(Color.PINK);
        body.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.BLACK));
        body.setLayout(new BorderLayout()); // Ensures components fill the entire area
        App.CustomTableModel tableModel = new App.CustomTableModel();
        TableModelContainer tableModelContainer = TableModelContainer.getInstance(tableModel);
        JTable table = new JTable(tableModel);
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
                    System.out.println("Selected Row ID: " + uniqueId);
                }
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

    private JButton add_magnet_button() {
        JButton add = new RoundButton("Add Magnet");
        add.setPreferredSize(new Dimension(100,25));


        add.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (add instanceof RoundButton) {
                    RoundButton roundButton = (RoundButton) add;
                    roundButton.set_new_border(new Color(0x3e98de));
                    roundButton.set_new_background(new Color(0xb6d3ea));

                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (add instanceof RoundButton) {
                    RoundButton roundButton = (RoundButton) add;
                    roundButton.set_new_border(Color.BLACK);
                    roundButton.set_new_background(Color.WHITE);

                }
            }
        });
        add.setBorderPainted(false);
        add.setContentAreaFilled(false);
        add.setFocusPainted(false);
        add.setOpaque(false);

        return add;
    }



    public static class CustomTableModel extends AbstractTableModel {
        private final String[] columnNames = {"ID", "Name", "Size", "Progress", "Seeds", "Peers", "Down Speed", "ETA"};
        private List<Object[]> data;

        public CustomTableModel() {
            List<Torrent> torrents = TorrentUtils.getAllTorrents();
            data = new ArrayList<>();
            for (int i = 0; i < torrents.size(); i++) {
                Torrent torrent = torrents.get(i);
                new Torrent(torrent.name,torrent.size,torrent.sha1hash);
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

        @Override
        public int getRowCount() {
            return data.size(); // Return the size of the list
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
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
