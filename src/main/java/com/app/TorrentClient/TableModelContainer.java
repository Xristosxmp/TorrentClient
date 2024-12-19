package com.app.TorrentClient;

public class TableModelContainer {
    private static TableModelContainer instance; // Singleton instance
    private App.CustomTableModel tableModel;

    // Private constructor to prevent instantiation
    private TableModelContainer(App.CustomTableModel tableModel) {
        this.tableModel = tableModel;
    }

    // Public method to get the singleton instance
    public static TableModelContainer getInstance(App.CustomTableModel tableModel) {
        if (instance == null) {
            instance = new TableModelContainer(tableModel);
        }
        return instance;
    }

    // Getter for table model
    public App.CustomTableModel getTableModel() {
        return tableModel;
    }

    // Optionally, set the table model if needed (if the instance needs to be updated)
    public void setTableModel(App.CustomTableModel tableModel) {
        this.tableModel = tableModel;
    }
}
