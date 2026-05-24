

package com.mycompany.oopfinals;
import java.awt.CardLayout;
import java.awt.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.*;

public class MainMenu extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MainMenu.class.getName());
 CardLayout cl;

 private String loggedUsername;
private String loggedRole;

private SalesGraphPanel salesGraphPanel;

private String formatOrderCode(int orderId) {
    return String.format("ORD-%06d", orderId);
}

private int parseOrderCode(Object value) {
    String text = String.valueOf(value).trim();
    if (text.toUpperCase().startsWith("ORD-")) {
        text = text.substring(4);
    }
    return Integer.parseInt(text);
}

private void styleFlatComponents(Container container) {
    for (Component component : container.getComponents()) {
        if (component instanceof JButton button) {
            Color background = button.getBackground();
            button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            button.setFocusPainted(false);
            button.setBackground(background);

            if (button.isContentAreaFilled() && background != null
                    && background.getRed() > 220 && background.getGreen() > 220 && background.getBlue() > 220) {
                button.setBackground(Color.WHITE);
                button.setOpaque(true);
                button.setBorderPainted(true);
            }
        } else if (component instanceof JScrollPane scrollPane) {
            scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI());
            scrollPane.getHorizontalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI());
        }

        if (component instanceof Container child) {
            styleFlatComponents(child);
        }
    }
}

private boolean isLastActiveAdmin(int userId) {
    try {
        Connection conn = DBConnection.getConnection();

        PreparedStatement ps = conn.prepareStatement(
            "SELECT COUNT(*) FROM users " +
            "WHERE role='admin' AND status='active'"
        );

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            int activeAdmins = rs.getInt(1);

            PreparedStatement currentUserPs = conn.prepareStatement(
                "SELECT role, status FROM users WHERE id=?"
            );

            currentUserPs.setInt(1, userId);

            ResultSet currentRs = currentUserPs.executeQuery();

            if (currentRs.next()) {
                String role = currentRs.getString("role");
                String status = currentRs.getString("status");

                return activeAdmins == 1
                        && role.equalsIgnoreCase("admin")
                        && status.equalsIgnoreCase("active");
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
    }

    return false;
}

private void lockTableSizes() {
    mainPanel.setPreferredSize(new Dimension(893, 733));
    mainPanel.setMinimumSize(new Dimension(893, 733));
    dashboardPanel.setPreferredSize(new Dimension(893, 733));
    ordersPanel.setPreferredSize(new Dimension(893, 733));
    catalogPanel.setPreferredSize(new Dimension(893, 733));
    accountsPanel.setPreferredSize(new Dimension(893, 733));
    activityLogPanel.setPreferredSize(new Dimension(893, 733));
    salesReportPanel.setPreferredSize(new Dimension(893, 733));

    allUsersTable.setRowHeight(24);
    allUsersTable.setFillsViewportHeight(true);
    jScrollPane3.setPreferredSize(new Dimension(843, 371));
    jScrollPane3.setMinimumSize(new Dimension(843, 371));

    jTable1.setRowHeight(24);
    jTable1.setFillsViewportHeight(true);
    jScrollPane1.setPreferredSize(new Dimension(842, 513));
    jScrollPane1.setMinimumSize(new Dimension(842, 513));

    catalogTable.setRowHeight(24);
    catalogTable.setFillsViewportHeight(true);
    jScrollPane4.setPreferredSize(new Dimension(845, 548));
    jScrollPane4.setMinimumSize(new Dimension(845, 548));

    activityLogTable.setRowHeight(24);
    activityLogTable.setFillsViewportHeight(true);
    jScrollPane5.setPreferredSize(new Dimension(656, 647));
    jScrollPane5.setMinimumSize(new Dimension(656, 647));
}


private void saveOrderWithItems(String customerName, java.util.List<OrderLine> lines,
        String platform, String paymentMethod, String deliveryMethod, String status) throws Exception {

    Connection conn = DBConnection.getConnection();
    conn.setAutoCommit(false);

    try {
        double total = 0;

        for (OrderLine line : lines) {
            total += line.subtotal;
        }

        PreparedStatement orderPs = conn.prepareStatement(
    "INSERT INTO orders (customer_name, total_price, platform, payment_method, delivery_method, status, date_completed) " +
    "VALUES (?, ?, ?, ?, ?, ?, CASE WHEN ? = 'Completed' THEN CURRENT_TIMESTAMP ELSE NULL END)",
    Statement.RETURN_GENERATED_KEYS
);

        orderPs.setString(1, customerName);
orderPs.setDouble(2, total);
orderPs.setString(3, platform);
orderPs.setString(4, paymentMethod);
orderPs.setString(5, deliveryMethod);
orderPs.setString(6, status);
orderPs.setString(7, status);
        orderPs.executeUpdate();

        ResultSet keys = orderPs.getGeneratedKeys();

        if (!keys.next()) {
            throw new SQLException("Failed to get order ID.");
        }

        int orderId = keys.getInt(1);

        PreparedStatement itemPs = conn.prepareStatement(
            "INSERT INTO order_items (order_id, item_id, item_type, item_name, price, quantity, subtotal) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)"
        );

        PreparedStatement stockPs = conn.prepareStatement(
            "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?"
        );

        for (OrderLine line : lines) {
            itemPs.setInt(1, orderId);

            if (line.itemId == 0) {
                itemPs.setNull(2, java.sql.Types.INTEGER);
            } else {
                itemPs.setInt(2, line.itemId);
            }

            itemPs.setString(3, line.itemType);
            itemPs.setString(4, line.itemName);
            itemPs.setDouble(5, line.price);
            itemPs.setInt(6, line.quantity);
            itemPs.setDouble(7, line.subtotal);
            itemPs.executeUpdate();

            if (line.itemType.equalsIgnoreCase("product")) {
                stockPs.setInt(1, line.quantity);
                stockPs.setInt(2, line.itemId);
                stockPs.setInt(3, line.quantity);

                int updated = stockPs.executeUpdate();

                if (updated == 0) {
                    throw new SQLException("Not enough stock for " + line.itemName);
                }
            }
        }

        conn.commit();

    } catch (Exception e) {
        conn.rollback();
        throw e;
    } finally {
        conn.setAutoCommit(true);
    }
}


private void addLog(String action, String details) {
    try {
        Connection conn = DBConnection.getConnection();

        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO activity_logs (username, role, action, details) VALUES (?, ?, ?, ?)"
        );

        ps.setString(1, loggedUsername);
        ps.setString(2, loggedRole);
        ps.setString(3, action);
        ps.setString(4, details);

        ps.executeUpdate();

if (activityLogTable != null) {
    loadActivityLogs();
}

    } catch (Exception e) {
        e.printStackTrace();
    }
}

private void updateOrderWithItems(int orderId, String customerName, java.util.List<OrderLine> lines,
        String platform, String paymentMethod, String deliveryMethod, String status) throws Exception {

    Connection conn = DBConnection.getConnection();
    conn.setAutoCommit(false);

    try {
        double total = 0;

        for (OrderLine line : lines) {
            total += line.subtotal;
        }

        // Return old product quantities to stock first.
        PreparedStatement oldItemsPs = conn.prepareStatement(
            "SELECT item_id, quantity FROM order_items WHERE order_id=? AND item_type='product' AND item_id IS NOT NULL"
        );

        oldItemsPs.setInt(1, orderId);

        ResultSet oldItemsRs = oldItemsPs.executeQuery();

        PreparedStatement returnStockPs = conn.prepareStatement(
            "UPDATE products SET stock = stock + ? WHERE id=?"
        );

        while (oldItemsRs.next()) {
            returnStockPs.setInt(1, oldItemsRs.getInt("quantity"));
            returnStockPs.setInt(2, oldItemsRs.getInt("item_id"));
            returnStockPs.executeUpdate();
        }

        PreparedStatement orderPs = conn.prepareStatement(
            "UPDATE orders SET customer_name=?, total_price=?, platform=?, payment_method=?, delivery_method=?, status=?, " +
"date_completed = CASE WHEN ? = 'Completed' THEN CURRENT_TIMESTAMP ELSE NULL END WHERE id=?"
        );

        orderPs.setString(1, customerName);
orderPs.setDouble(2, total);
orderPs.setString(3, platform);
orderPs.setString(4, paymentMethod);
orderPs.setString(5, deliveryMethod);
orderPs.setString(6, status);
orderPs.setString(7, status);
orderPs.setInt(8, orderId);
        orderPs.executeUpdate();

        PreparedStatement deletePs = conn.prepareStatement(
            "DELETE FROM order_items WHERE order_id=?"
        );

        deletePs.setInt(1, orderId);
        deletePs.executeUpdate();

        PreparedStatement itemPs = conn.prepareStatement(
            "INSERT INTO order_items (order_id, item_id, item_type, item_name, price, quantity, subtotal) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)"
        );

        PreparedStatement stockPs = conn.prepareStatement(
            "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?"
        );

        for (OrderLine line : lines) {
            itemPs.setInt(1, orderId);

            if (line.itemId == 0) {
                itemPs.setNull(2, java.sql.Types.INTEGER);
            } else {
                itemPs.setInt(2, line.itemId);
            }

            itemPs.setString(3, line.itemType);
            itemPs.setString(4, line.itemName);
            itemPs.setDouble(5, line.price);
            itemPs.setInt(6, line.quantity);
            itemPs.setDouble(7, line.subtotal);
            itemPs.executeUpdate();

            if (line.itemType.equalsIgnoreCase("product")) {
                stockPs.setInt(1, line.quantity);
                stockPs.setInt(2, line.itemId);
                stockPs.setInt(3, line.quantity);

                int updated = stockPs.executeUpdate();

                if (updated == 0) {
                    throw new SQLException("Not enough stock for " + line.itemName);
                }
            }
        }

        conn.commit();

    } catch (Exception e) {
        conn.rollback();
        throw e;
    } finally {
        conn.setAutoCommit(true);
    }
}

private class RemoveButtonEditor extends DefaultCellEditor {
    private JButton button;
    private JTable table;

    RemoveButtonEditor(JCheckBox checkBox, JTable table) {
        super(checkBox);
        this.table = table;
        button = new JButton("Remove");

        button.addActionListener(e -> {
            int row = table.getEditingRow();

            if (row >= 0 && table.getRowCount() > 1) {
                ((DefaultTableModel) table.getModel()).removeRow(row);
            } else {
                JOptionPane.showMessageDialog(MainMenu.this, "At least one item is required.");
            }

            fireEditingStopped();
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        return "Remove";
    }
}

private static class RemoveButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
    RemoveButtonRenderer() {
        setText("Remove");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        return this;
    }
}



private static class OrderLine {
    int itemId;
    String itemType;
    String itemName;
    double price;
    int quantity;
    double subtotal;

    OrderLine(int itemId, String itemType, String itemName, double price, int quantity) {
        this.itemId = itemId;
        this.itemType = itemType;
        this.itemName = itemName;
        this.price = price;
        this.quantity = quantity;
        this.subtotal = price * quantity;
    }
}

private static class OrderItem {
    int id;
    String type; // "product", "service", or "custom"
    String name;
    double price;
    int stock;

    OrderItem(int id, String type, String name, double price, int stock) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    @Override
    public String toString() {
        if (type.equalsIgnoreCase("product")) {
            return name + " (" + type + ", stock: " + stock + ")";
        }

        return name + " (" + type + ")";
    }
}

private void applyOrderStatusColors() {
    jTable1.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
        @Override
        public java.awt.Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            java.awt.Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column
            );

            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
                return c;
            }

            Object statusValue = table.getValueAt(row, 7);

            if (statusValue != null && statusValue.toString().equalsIgnoreCase("Completed")) {
                c.setBackground(new java.awt.Color(210, 245, 220));
                c.setForeground(new java.awt.Color(20, 90, 45));
            } else if (statusValue != null && statusValue.toString().equalsIgnoreCase("Cancelled")) {
                c.setBackground(new java.awt.Color(255, 215, 215));
                c.setForeground(new java.awt.Color(130, 25, 25));
            } else {
                c.setBackground(java.awt.Color.WHITE);
                c.setForeground(java.awt.Color.BLACK);
            }

            return c;
        }
    });
}
private void addDetailRow(JPanel panel, GridBagConstraints gbc, int row, String label, Object value) {
    JLabel labelComponent = new JLabel(label);
    labelComponent.setFont(new Font("Poppins", Font.PLAIN, 12));
    labelComponent.setForeground(new Color(120, 120, 120));

    JLabel valueComponent = new JLabel(value == null ? "-" : value.toString());
    valueComponent.setFont(new Font("Poppins", Font.BOLD, 14));
    valueComponent.setForeground(new Color(35, 35, 35));

    gbc.gridx = 0;
    gbc.gridy = row;
    gbc.weightx = 0.35;
    panel.add(labelComponent, gbc);

    gbc.gridx = 1;
    gbc.gridy = row;
    gbc.weightx = 0.65;
    panel.add(valueComponent, gbc);
}

private void loadActivityUsers() {
    try {
        Connection conn = DBConnection.getConnection();

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("All Users");

        PreparedStatement ps = conn.prepareStatement("SELECT username FROM users ORDER BY username");
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            model.addElement(rs.getString("username"));
        }

        userComboBox.setModel(model);

    } catch (Exception e) {
        e.printStackTrace();
    }
}

private void loadActivityLogs() {
    try {
        Connection conn = DBConnection.getConnection();
        StringBuilder sql = new StringBuilder(
            "SELECT id, username, role, action, details, created_at FROM activity_logs WHERE 1=1"
        );
        java.util.List<Object> params = new java.util.ArrayList<>();

        java.util.Date startDate = activityStartDateChooser.getDate();
        java.util.Date endDate = activityEndDateChooser.getDate();
        String selectedUser = userComboBox.getSelectedItem() == null ? "All Users" : userComboBox.getSelectedItem().toString();

        if (startDate != null) {
            sql.append(" AND DATE(created_at) >= ?");
            params.add(new java.sql.Date(startDate.getTime()));
        }

        if (endDate != null) {
            sql.append(" AND DATE(created_at) <= ?");
            params.add(new java.sql.Date(endDate.getTime()));
        }

        if (!"All Users".equals(selectedUser)) {
            sql.append(" AND username = ?");
            params.add(selectedUser);
        }

        sql.append(" ORDER BY id DESC");

        PreparedStatement ps = conn.prepareStatement(sql.toString());

        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }

        ResultSet rs = ps.executeQuery();

        DefaultTableModel model = (DefaultTableModel) activityLogTable.getModel();
        model.setRowCount(0);

        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("role"),
                rs.getString("action"),
                rs.getString("details"),
                rs.getTimestamp("created_at")
            });
        }

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to load activity logs.");
    }
}

private void resetActivityLogFilters() {
    activityStartDateChooser.setDate(null);
    activityEndDateChooser.setDate(null);
    userComboBox.setSelectedItem("All Users");
    loadActivityLogs();
}

private String buildReceiptText(int orderId, int row, DefaultTableModel itemModel) {
    StringBuilder receipt = new StringBuilder();
    receipt.append("        BARODISENO\n");
    receipt.append("        ORDER RECEIPT\n");
    receipt.append("--------------------------------\n");
    receipt.append("Order #: ").append(formatOrderCode(orderId)).append("\n");
    receipt.append("Customer: ").append(jTable1.getValueAt(row, 1)).append("\n");
    receipt.append("Platform: ").append(jTable1.getValueAt(row, 4)).append("\n");
    receipt.append("Payment: ").append(jTable1.getValueAt(row, 5)).append("\n");
    receipt.append("Delivery: ").append(jTable1.getValueAt(row, 6)).append("\n");
    receipt.append("Status: ").append(jTable1.getValueAt(row, 7)).append("\n");
    receipt.append("Date: ").append(jTable1.getValueAt(row, 8)).append("\n");
    receipt.append("--------------------------------\n");
    receipt.append(String.format("%-14s %3s %10s\n", "Item", "Qty", "Subtotal"));
    receipt.append("--------------------------------\n");

    for (int i = 0; i < itemModel.getRowCount(); i++) {
        String item = String.valueOf(itemModel.getValueAt(i, 0));
        int quantity = Integer.parseInt(String.valueOf(itemModel.getValueAt(i, 3)));
        double subtotal = Double.parseDouble(String.valueOf(itemModel.getValueAt(i, 4)));

        if (item.length() > 14) {
            item = item.substring(0, 14);
        }

        receipt.append(String.format("%-14s %3d PHP %6.2f\n", item, quantity, subtotal));
    }

    double total = Double.parseDouble(String.valueOf(jTable1.getValueAt(row, 3)));
    double vat = total * 12 / 112;
    double netOfVat = total - vat;

    receipt.append("--------------------------------\n");
    receipt.append(String.format("VATable Sales:      PHP %6.2f\n", netOfVat));
    receipt.append(String.format("VAT 12%% included:   PHP %6.2f\n", vat));
    receipt.append(String.format("TOTAL:              PHP %6.2f\n", total));
    receipt.append("--------------------------------\n");
    receipt.append("Thank you for your order!\n");

    return receipt.toString();
}

private void showReceiptDialog(int orderId, int row, DefaultTableModel itemModel) {
    JDialog receiptDialog = new JDialog(this, "Receipt", true);
    receiptDialog.setSize(380, 560);
    receiptDialog.setLocationRelativeTo(this);
    receiptDialog.setLayout(new BorderLayout());

    JTextArea receiptArea = new JTextArea(buildReceiptText(orderId, row, itemModel));
    receiptArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    receiptArea.setEditable(false);
    receiptArea.setMargin(new Insets(14, 14, 14, 14));

    JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton printButton = new JButton("Print");
    JButton closeButton = new JButton("Close");

    printButton.addActionListener(e -> {
        try {
            receiptArea.print();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(receiptDialog, "Failed to print receipt.");
        }
    });

    closeButton.addActionListener(e -> receiptDialog.dispose());
    footer.add(printButton);
    footer.add(closeButton);

    receiptDialog.add(new JScrollPane(receiptArea), BorderLayout.CENTER);
    receiptDialog.add(footer, BorderLayout.SOUTH);
    styleFlatComponents(receiptDialog);
    receiptDialog.setVisible(true);
}

private void viewActivityLogDetails() {
    int row = activityLogTable.getSelectedRow();

    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Select a log first.");
        return;
    }

    JDialog dialog = new JDialog(this, "Activity Log Details", true);
    dialog.setSize(520, 420);
    dialog.setLocationRelativeTo(this);
    dialog.setLayout(new BorderLayout());

    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setBackground(Color.WHITE);
    headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 20, 14, 20));

    JLabel titleLabel = new JLabel("Log #" + activityLogTable.getValueAt(row, 0));
    titleLabel.setFont(new Font("Poppins", Font.BOLD, 22));

    JLabel actionLabel = new JLabel(String.valueOf(activityLogTable.getValueAt(row, 3)));
    actionLabel.setFont(new Font("Poppins", Font.BOLD, 12));
    actionLabel.setOpaque(true);
    actionLabel.setBackground(new Color(220, 235, 255));
    actionLabel.setForeground(new Color(20, 70, 130));
    actionLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

    headerPanel.add(titleLabel, BorderLayout.WEST);
    headerPanel.add(actionLabel, BorderLayout.EAST);

    JPanel bodyPanel = new JPanel(new GridBagLayout());
    bodyPanel.setBackground(Color.WHITE);
    bodyPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 0, 8, 0);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    addDetailRow(bodyPanel, gbc, 0, "Username", activityLogTable.getValueAt(row, 1));
    addDetailRow(bodyPanel, gbc, 1, "Role", activityLogTable.getValueAt(row, 2));
    addDetailRow(bodyPanel, gbc, 2, "Action", activityLogTable.getValueAt(row, 3));
    addDetailRow(bodyPanel, gbc, 3, "Details", activityLogTable.getValueAt(row, 4));
    addDetailRow(bodyPanel, gbc, 4, "Date/Time", activityLogTable.getValueAt(row, 5));

    JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    footerPanel.setBackground(Color.WHITE);
    footerPanel.setBorder(BorderFactory.createEmptyBorder(8, 20, 18, 20));

    JButton closeButton = new JButton("Close");
    closeButton.addActionListener(e -> dialog.dispose());
    footerPanel.add(closeButton);

    dialog.add(headerPanel, BorderLayout.NORTH);
    dialog.add(bodyPanel, BorderLayout.CENTER);
    dialog.add(footerPanel, BorderLayout.SOUTH);

    styleFlatComponents(dialog);
    dialog.setVisible(true);
}

private void addTableDoubleClickActions() {
    jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
            if (e.getClickCount() == 2 && jTable1.getSelectedRow() != -1) {
                viewDetailsButtonActionPerformed(null);
            }
        }
    });

    activityLogTable.addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
            if (e.getClickCount() == 2 && activityLogTable.getSelectedRow() != -1) {
                viewActivityLogDetails();
            }
        }
    });
}

private void loadAllUsers() {
    try {
        Connection conn = DBConnection.getConnection();

        PreparedStatement ps = conn.prepareStatement(
            "SELECT id, name, username, role, status FROM users"
        );

        ResultSet rs = ps.executeQuery();

        DefaultTableModel model = (DefaultTableModel) allUsersTable.getModel();
        model.setRowCount(0);

        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("username"),
                rs.getString("role"),
                rs.getString("status")
            });
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}

private void loadOrders() {
    try {
        Connection conn = DBConnection.getConnection();

        PreparedStatement ps = conn.prepareStatement(
    "SELECT o.id, o.customer_name, COUNT(oi.id) AS item_count, o.total_price, " +
    "o.platform, o.payment_method, o.delivery_method, o.status, o.order_date, o.date_completed " +
    "FROM orders o " +
    "LEFT JOIN order_items oi ON o.id = oi.order_id " +
    "GROUP BY o.id, o.customer_name, o.total_price, o.platform, o.payment_method, o.delivery_method, o.status, o.order_date, o.date_completed " +
    "ORDER BY o.id DESC"
);

        ResultSet rs = ps.executeQuery();

        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);

        while (rs.next()) {
            model.addRow(new Object[]{
    formatOrderCode(rs.getInt("id")),
    rs.getString("customer_name"),
    rs.getInt("item_count") + " item(s)",
    rs.getDouble("total_price"),
    rs.getString("platform"),
    rs.getString("payment_method"),
    rs.getString("delivery_method"),
    rs.getString("status"),
    rs.getTimestamp("order_date"),
    rs.getTimestamp("date_completed")
});
        }

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to load orders.");
    }
}

private java.util.List<OrderItem> loadOrderItems() {
    java.util.List<OrderItem> items = new java.util.ArrayList<>();

    try {
        Connection conn = DBConnection.getConnection();

       PreparedStatement psProducts = conn.prepareStatement(
    "SELECT id, name, price, stock FROM products WHERE status='active' AND stock > 0"
);
        ResultSet rsProducts = psProducts.executeQuery();

        while (rsProducts.next()) {
            items.add(new OrderItem(
    rsProducts.getInt("id"),
    "product",
    rsProducts.getString("name"),
    rsProducts.getDouble("price"),
    rsProducts.getInt("stock")
));
        }

        PreparedStatement psServices = conn.prepareStatement(
            "SELECT id, name, base_price FROM services WHERE status='active'"
        );
        ResultSet rsServices = psServices.executeQuery();

        while (rsServices.next()) {
    items.add(new OrderItem(
        rsServices.getInt("id"),
        "service",
        rsServices.getString("name"),
        rsServices.getDouble("base_price"),
        0
    ));
}

    } catch (Exception e) {
        e.printStackTrace();
    }

    return items;
}

private void loadDashboard() {
    try {
        Connection conn = DBConnection.getConnection();

        PreparedStatement ps = conn.prepareStatement(
            "SELECT " +
            "COUNT(*) AS total_orders, " +
            "SUM(CASE WHEN TRIM(status)='Pending' THEN 1 ELSE 0 END) AS pending_orders, " +
            "SUM(CASE WHEN TRIM(status)='Processing' THEN 1 ELSE 0 END) AS processing_orders, " +
            "SUM(CASE WHEN TRIM(status)='Completed' THEN 1 ELSE 0 END) AS completed_orders " +
            "FROM orders"
        );

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            totalOrdersNumberLabel4.setText(String.valueOf(rs.getInt("total_orders")));
            pendingOrdersNumberLabel.setText(String.valueOf(rs.getInt("pending_orders")));
            processingOrdersNumberLabel.setText(String.valueOf(rs.getInt("processing_orders")));
            completedOrdersNumberLabel.setText(String.valueOf(rs.getInt("completed_orders")));
        }

        catalogStockTextArea.setText(buildCatalogStockSummary(conn));

        if (salesGraphPanel != null) {
            salesGraphPanel.loadGraphData();
        }

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to load dashboard.");
    }
}

private String buildCatalogStockSummary(Connection conn) throws Exception {
    StringBuilder summary = new StringBuilder();

    PreparedStatement countsPs = conn.prepareStatement(
        "SELECT " +
        "SUM(CASE WHEN status='active' THEN 1 ELSE 0 END) AS active_products, " +
        "SUM(CASE WHEN status<>'active' THEN 1 ELSE 0 END) AS inactive_products, " +
        "SUM(CASE WHEN stock=0 THEN 1 ELSE 0 END) AS out_of_stock, " +
        "SUM(CASE WHEN stock BETWEEN 1 AND 5 THEN 1 ELSE 0 END) AS low_stock, " +
        "COALESCE(SUM(stock), 0) AS total_units " +
        "FROM products"
    );

    ResultSet counts = countsPs.executeQuery();
    if (counts.next()) {
        summary.append("Active products: ").append(counts.getInt("active_products")).append("\n");
        summary.append("Inactive products: ").append(counts.getInt("inactive_products")).append("\n");
        summary.append("Total product units: ").append(counts.getInt("total_units")).append("\n");
        summary.append("Out of stock: ").append(counts.getInt("out_of_stock")).append("\n");
        summary.append("Low stock: ").append(counts.getInt("low_stock")).append("\n\n");
    }

    PreparedStatement servicesPs = conn.prepareStatement(
        "SELECT " +
        "SUM(CASE WHEN status='active' THEN 1 ELSE 0 END) AS active_services, " +
        "SUM(CASE WHEN status<>'active' THEN 1 ELSE 0 END) AS inactive_services " +
        "FROM services"
    );

    ResultSet services = servicesPs.executeQuery();
    if (services.next()) {
        summary.append("Active services: ").append(services.getInt("active_services")).append("\n");
        summary.append("Inactive services: ").append(services.getInt("inactive_services")).append("\n\n");
    }

    PreparedStatement stockPs = conn.prepareStatement(
        "SELECT name, stock FROM products WHERE status='active' AND stock <= 5 ORDER BY stock ASC, name"
    );

    ResultSet stockRs = stockPs.executeQuery();
    summary.append("Needs attention:\n");
    boolean hasAttentionItems = false;

    while (stockRs.next()) {
        hasAttentionItems = true;
        int stock = stockRs.getInt("stock");
        summary.append("- ")
                .append(stockRs.getString("name"))
                .append(stock == 0 ? " is out of stock" : " has only " + stock + " left")
                .append("\n");
    }

    if (!hasAttentionItems) {
        summary.append("- No low stock items.");
    }

    return summary.toString();
}

private void loadCatalog() {
    try {
        Connection conn = DBConnection.getConnection();

        DefaultTableModel model = (DefaultTableModel) catalogTable.getModel();
        model.setRowCount(0);

       PreparedStatement psProducts = conn.prepareStatement(
    "SELECT id, name, stock, price, status FROM products"
);

        ResultSet rp = psProducts.executeQuery();

        while (rp.next()) {
            model.addRow(new Object[]{
    rp.getInt("id"),
    rp.getString("name"),
    "product",
    rp.getDouble("price"),
    rp.getInt("stock"),
    rp.getString("status")
});
        }

        PreparedStatement psServices = conn.prepareStatement(
            "SELECT id, name, base_price, unit, status FROM services"
        );

        ResultSet rs = psServices.executeQuery();

        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getInt("id"),
                rs.getString("name"),
                "service",
                rs.getDouble("base_price"),
                rs.getString("unit"),
                rs.getString("status")
            });
        }

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to load catalog.");
    }
}

private boolean hasMoreThanOneActiveAdmin() {
    try {
        Connection conn = DBConnection.getConnection();

        PreparedStatement ps = conn.prepareStatement(
            "SELECT COUNT(*) FROM users WHERE role='admin' AND status='active'"
        );

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt(1) > 1;
        }

    } catch (Exception e) {
        e.printStackTrace();
    }

    return false;
}

private boolean isAdmin() {
    return "admin".equalsIgnoreCase(loggedRole);
}
private void loadCurrentUser() {
    try {
        Connection conn = DBConnection.getConnection();

        PreparedStatement ps = conn.prepareStatement(
            "SELECT id, name, username, role FROM users WHERE username=?"
        );

        ps.setString(1, loggedUsername);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            String displayName = rs.getString("name");
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = rs.getString("username");
            }

            accountNameButton.setText(displayName);
            actualIdLabel.setText(String.valueOf(rs.getInt("id")));
            actualNameLabel.setText(displayName);
            actualUsernameLabel.setText(rs.getString("username"));
            actualRoleLabel.setText(rs.getString("role"));
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}
   public MainMenu(String username, String role) {
    initComponents();
    styleFlatComponents(this);
    lockTableSizes();
    
    cl = (CardLayout) mainPanel.getLayout();

    accountNameButton.setText("");

salesGraphPanel = new SalesGraphPanel();

actualsalesAnalysisPanel.removeAll();
actualsalesAnalysisPanel.setLayout(new BorderLayout());
actualsalesAnalysisPanel.setPreferredSize(new Dimension(464, 210));
actualsalesAnalysisPanel.setMinimumSize(new Dimension(360, 170));
actualsalesAnalysisPanel.add(salesGraphPanel, BorderLayout.CENTER);

catalogStockTextArea.setEditable(false);
catalogStockTextArea.setFont(new Font("Poppins", Font.PLAIN, 12));
catalogStockTextArea.setLineWrap(true);
catalogStockTextArea.setWrapStyleWord(true);
catalogStockTextArea.setBackground(Color.WHITE);
jScrollPane2.setPreferredSize(new Dimension(279, 253));
jScrollPane2.setMinimumSize(new Dimension(240, 220));
    
    jTable1.getTableHeader().setFont(
    new java.awt.Font("Poppins", java.awt.Font.BOLD, 12)
);
    
    
    allUsersTable.getTableHeader().setFont(
    new java.awt.Font("Poppins", java.awt.Font.BOLD, 12)
);
    
     catalogTable.getTableHeader().setFont(
    new java.awt.Font("Poppins", java.awt.Font.BOLD, 12)
);
     
     activityLogTable.getTableHeader().setFont(
    new java.awt.Font("Poppins", java.awt.Font.BOLD, 12)
);
    
       salesReportPanel.setLayout(new java.awt.BorderLayout());
    salesReportPanel.removeAll();
    salesReportPanel.add(new SalesReportPanel(), java.awt.BorderLayout.CENTER);
    salesReportPanel.revalidate();
    salesReportPanel.repaint();

    applyOrderStatusColors();
    this.loggedUsername = username;
    this.loggedRole = role;
    addLog("LOGIN", "User logged in");

    cl = (CardLayout) mainPanel.getLayout();

loadCurrentUser();
loadAllUsers();
loadActivityUsers();
resetLogButton.addActionListener(e -> resetActivityLogFilters());
loadOrders();
loadCatalog();
loadActivityLogs();
addTableDoubleClickActions();
loadDashboard();

 if (!isAdmin()) {
    accountsButton.setVisible(false);
    activityLogButton.setVisible(false);


    changeUsernameAllAccountsButton.setEnabled(false);

}   


    cl.show(mainPanel, "dashboard");

    revalidate();
    pack();
    setLocationRelativeTo(null);
    setVisible(true);
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        optionPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        salesReportButton = new javax.swing.JButton();
        dashboardButton = new javax.swing.JButton();
        accountsButton = new javax.swing.JButton();
        logOutButton = new javax.swing.JButton();
        catalogButton = new javax.swing.JButton();
        activityLogButton = new javax.swing.JButton();
        ordersButton = new javax.swing.JButton();
        mainPanel = new javax.swing.JPanel();
        dashboardPanel = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        totalOrdersPanel4 = new javax.swing.JPanel();
        totalOrdersLabel4 = new javax.swing.JLabel();
        totalOrdersNumberLabel4 = new javax.swing.JLabel();
        pendingPanel = new javax.swing.JPanel();
        pendingLabel = new javax.swing.JLabel();
        pendingOrdersNumberLabel = new javax.swing.JLabel();
        processingPanel = new javax.swing.JPanel();
        processingLabel = new javax.swing.JLabel();
        processingOrdersNumberLabel = new javax.swing.JLabel();
        completedPanel = new javax.swing.JPanel();
        completedLabel = new javax.swing.JLabel();
        completedOrdersNumberLabel = new javax.swing.JLabel();
        completedLabel1 = new javax.swing.JLabel();
        dashboardCreateOrderButton = new javax.swing.JButton();
        graphContainerPanel = new javax.swing.JPanel();
        completedLabel2 = new javax.swing.JLabel();
        actualsalesAnalysisPanel = new javax.swing.JPanel();
        totalOrdersLabel5 = new javax.swing.JLabel();
        accountNameButton = new javax.swing.JButton();
        graphContainerPanel1 = new javax.swing.JPanel();
        completedLabel3 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        catalogStockTextArea = new javax.swing.JTextArea();
        jLabel9 = new javax.swing.JLabel();
        salesReportPanel = new javax.swing.JPanel();
        accountsPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        allUsersTable = new javax.swing.JTable();
        addAccountButton = new javax.swing.JButton();
        changeUsernameAllAccountsButton = new javax.swing.JButton();
        idLabel = new javax.swing.JLabel();
        nameLabel = new javax.swing.JLabel();
        usernameLabel = new javax.swing.JLabel();
        roleLabel = new javax.swing.JLabel();
        actualIdLabel = new javax.swing.JLabel();
        actualNameLabel = new javax.swing.JLabel();
        actualUsernameLabel = new javax.swing.JLabel();
        actualRoleLabel = new javax.swing.JLabel();
        ordersPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        createOrderButton = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        updateStatusButton = new javax.swing.JButton();
        cancelOrderButton = new javax.swing.JButton();
        viewDetailsButton = new javax.swing.JButton();
        editOrderButton = new javax.swing.JButton();
        catalogPanel = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        catalogTable = new javax.swing.JTable();
        addProductServiceButton = new javax.swing.JButton();
        editProductServiceButton = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        activityLogPanel = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        activityLogTable = new javax.swing.JTable();
        activityStartDateChooser = new com.toedter.calendar.JDateChooser();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        activityEndDateChooser = new com.toedter.calendar.JDateChooser();
        userComboBox = new javax.swing.JComboBox<>();
        jLabel12 = new javax.swing.JLabel();
        generateLogButton = new javax.swing.JButton();
        resetLogButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        optionPanel.setBackground(new java.awt.Color(27, 25, 24));
        optionPanel.setForeground(new java.awt.Color(27, 25, 24));

        jLabel1.setIcon(new javax.swing.ImageIcon("C:\\Users\\admin\\Downloads\\barodisenologowhite.png")); // NOI18N

        salesReportButton.setBackground(new java.awt.Color(27, 25, 24));
        salesReportButton.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        salesReportButton.setForeground(new java.awt.Color(255, 255, 255));
        salesReportButton.setText("Sales Report");
        salesReportButton.setBorderPainted(false);
        salesReportButton.setContentAreaFilled(false);
        salesReportButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        salesReportButton.addActionListener(this::salesReportButtonActionPerformed);

        dashboardButton.setBackground(new java.awt.Color(27, 25, 24));
        dashboardButton.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        dashboardButton.setForeground(new java.awt.Color(255, 255, 255));
        dashboardButton.setText("Dashboard");
        dashboardButton.setBorderPainted(false);
        dashboardButton.setContentAreaFilled(false);
        dashboardButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        dashboardButton.setMaximumSize(new java.awt.Dimension(88, 29));
        dashboardButton.setMinimumSize(new java.awt.Dimension(88, 29));
        dashboardButton.setPreferredSize(new java.awt.Dimension(88, 29));
        dashboardButton.addActionListener(this::dashboardButtonActionPerformed);

        accountsButton.setBackground(new java.awt.Color(27, 25, 24));
        accountsButton.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        accountsButton.setForeground(new java.awt.Color(255, 255, 255));
        accountsButton.setText("Accounts");
        accountsButton.setBorderPainted(false);
        accountsButton.setContentAreaFilled(false);
        accountsButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        accountsButton.addActionListener(this::accountsButtonActionPerformed);

        logOutButton.setBackground(new java.awt.Color(27, 25, 24));
        logOutButton.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        logOutButton.setForeground(new java.awt.Color(255, 255, 255));
        logOutButton.setText("Log Out");
        logOutButton.setBorderPainted(false);
        logOutButton.setContentAreaFilled(false);
        logOutButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        logOutButton.addActionListener(this::logOutButtonActionPerformed);

        catalogButton.setBackground(new java.awt.Color(27, 25, 24));
        catalogButton.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        catalogButton.setForeground(new java.awt.Color(255, 255, 255));
        catalogButton.setText("Catalog");
        catalogButton.setBorderPainted(false);
        catalogButton.setContentAreaFilled(false);
        catalogButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        catalogButton.addActionListener(this::catalogButtonActionPerformed);

        activityLogButton.setBackground(new java.awt.Color(27, 25, 24));
        activityLogButton.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        activityLogButton.setForeground(new java.awt.Color(255, 255, 255));
        activityLogButton.setText("Activity Log");
        activityLogButton.setBorderPainted(false);
        activityLogButton.setContentAreaFilled(false);
        activityLogButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        activityLogButton.addActionListener(this::activityLogButtonActionPerformed);

        ordersButton.setBackground(new java.awt.Color(27, 25, 24));
        ordersButton.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        ordersButton.setForeground(new java.awt.Color(255, 255, 255));
        ordersButton.setText("Orders");
        ordersButton.setBorderPainted(false);
        ordersButton.setContentAreaFilled(false);
        ordersButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        ordersButton.setMaximumSize(new java.awt.Dimension(88, 29));
        ordersButton.setMinimumSize(new java.awt.Dimension(88, 29));
        ordersButton.setPreferredSize(new java.awt.Dimension(88, 29));
        ordersButton.addActionListener(this::ordersButtonActionPerformed);

        javax.swing.GroupLayout optionPanelLayout = new javax.swing.GroupLayout(optionPanel);
        optionPanel.setLayout(optionPanelLayout);
        optionPanelLayout.setHorizontalGroup(
            optionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optionPanelLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(optionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(dashboardButton, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(optionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(logOutButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, optionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ordersButton, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(optionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(accountsButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(activityLogButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(salesReportButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(catalogButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                .addContainerGap(25, Short.MAX_VALUE))
        );
        optionPanelLayout.setVerticalGroup(
            optionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optionPanelLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32)
                .addComponent(dashboardButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(ordersButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(catalogButton)
                .addGap(18, 18, 18)
                .addComponent(salesReportButton)
                .addGap(18, 18, 18)
                .addComponent(accountsButton)
                .addGap(18, 18, 18)
                .addComponent(activityLogButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(logOutButton)
                .addGap(39, 39, 39))
        );

        mainPanel.setLayout(new java.awt.CardLayout());

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));
        jPanel5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));

        totalOrdersPanel4.setBackground(new java.awt.Color(255, 255, 255));
        totalOrdersPanel4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));

        totalOrdersLabel4.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        totalOrdersLabel4.setText("Total Orders");

        totalOrdersNumberLabel4.setFont(new java.awt.Font("Poppins", 1, 40)); // NOI18N
        totalOrdersNumberLabel4.setText("43");

        javax.swing.GroupLayout totalOrdersPanel4Layout = new javax.swing.GroupLayout(totalOrdersPanel4);
        totalOrdersPanel4.setLayout(totalOrdersPanel4Layout);
        totalOrdersPanel4Layout.setHorizontalGroup(
            totalOrdersPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(totalOrdersPanel4Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(totalOrdersPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(totalOrdersNumberLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(totalOrdersLabel4))
                .addContainerGap(46, Short.MAX_VALUE))
        );
        totalOrdersPanel4Layout.setVerticalGroup(
            totalOrdersPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(totalOrdersPanel4Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(totalOrdersLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(totalOrdersNumberLabel4)
                .addContainerGap(28, Short.MAX_VALUE))
        );

        pendingPanel.setBackground(new java.awt.Color(255, 255, 255));
        pendingPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));

        pendingLabel.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        pendingLabel.setText("Pending");

        pendingOrdersNumberLabel.setFont(new java.awt.Font("Poppins", 1, 40)); // NOI18N
        pendingOrdersNumberLabel.setText("3");

        javax.swing.GroupLayout pendingPanelLayout = new javax.swing.GroupLayout(pendingPanel);
        pendingPanel.setLayout(pendingPanelLayout);
        pendingPanelLayout.setHorizontalGroup(
            pendingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pendingPanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(pendingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pendingOrdersNumberLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pendingLabel))
                .addContainerGap(61, Short.MAX_VALUE))
        );
        pendingPanelLayout.setVerticalGroup(
            pendingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pendingPanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(pendingLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pendingOrdersNumberLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        processingPanel.setBackground(new java.awt.Color(255, 255, 255));
        processingPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));

        processingLabel.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        processingLabel.setText("Processing");

        processingOrdersNumberLabel.setFont(new java.awt.Font("Poppins", 1, 40)); // NOI18N
        processingOrdersNumberLabel.setText("12");

        javax.swing.GroupLayout processingPanelLayout = new javax.swing.GroupLayout(processingPanel);
        processingPanel.setLayout(processingPanelLayout);
        processingPanelLayout.setHorizontalGroup(
            processingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(processingPanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(processingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(processingOrdersNumberLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(processingLabel))
                .addContainerGap(49, Short.MAX_VALUE))
        );
        processingPanelLayout.setVerticalGroup(
            processingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(processingPanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(processingLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(processingOrdersNumberLabel)
                .addContainerGap(28, Short.MAX_VALUE))
        );

        completedPanel.setBackground(new java.awt.Color(255, 255, 255));
        completedPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));

        completedLabel.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        completedLabel.setText("Completed");

        completedOrdersNumberLabel.setFont(new java.awt.Font("Poppins", 1, 40)); // NOI18N
        completedOrdersNumberLabel.setText("30");

        javax.swing.GroupLayout completedPanelLayout = new javax.swing.GroupLayout(completedPanel);
        completedPanel.setLayout(completedPanelLayout);
        completedPanelLayout.setHorizontalGroup(
            completedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(completedPanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(completedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(completedOrdersNumberLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(completedLabel))
                .addContainerGap(46, Short.MAX_VALUE))
        );
        completedPanelLayout.setVerticalGroup(
            completedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(completedPanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(completedLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(completedOrdersNumberLabel)
                .addContainerGap(28, Short.MAX_VALUE))
        );

        completedLabel1.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
        completedLabel1.setText("Orders");

        dashboardCreateOrderButton.setBackground(new java.awt.Color(27, 25, 24));
        dashboardCreateOrderButton.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        dashboardCreateOrderButton.setForeground(new java.awt.Color(255, 255, 255));
        dashboardCreateOrderButton.setText("+ Create Order ");
        dashboardCreateOrderButton.setBorderPainted(false);
        dashboardCreateOrderButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        dashboardCreateOrderButton.setMaximumSize(new java.awt.Dimension(88, 29));
        dashboardCreateOrderButton.setMinimumSize(new java.awt.Dimension(88, 29));
        dashboardCreateOrderButton.setPreferredSize(new java.awt.Dimension(88, 29));
        dashboardCreateOrderButton.addActionListener(this::dashboardCreateOrderButtonActionPerformed);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(completedLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(dashboardCreateOrderButton, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(totalOrdersPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(completedPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(processingPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(pendingPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(21, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel5Layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(completedLabel1)
                            .addComponent(dashboardCreateOrderButton, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(22, 22, 22)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(totalOrdersPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(completedPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(processingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(pendingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap(24, Short.MAX_VALUE))
        );

        graphContainerPanel.setBackground(new java.awt.Color(255, 255, 255));
        graphContainerPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));

        completedLabel2.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
        completedLabel2.setText("Sales Analysis");

        actualsalesAnalysisPanel.setBackground(new java.awt.Color(255, 255, 255));
        actualsalesAnalysisPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));

        javax.swing.GroupLayout actualsalesAnalysisPanelLayout = new javax.swing.GroupLayout(actualsalesAnalysisPanel);
        actualsalesAnalysisPanel.setLayout(actualsalesAnalysisPanelLayout);
        actualsalesAnalysisPanelLayout.setHorizontalGroup(
            actualsalesAnalysisPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 472, Short.MAX_VALUE)
        );
        actualsalesAnalysisPanelLayout.setVerticalGroup(
            actualsalesAnalysisPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 251, Short.MAX_VALUE)
        );

        totalOrdersLabel5.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        totalOrdersLabel5.setText("(within the last 7 days)");

        javax.swing.GroupLayout graphContainerPanelLayout = new javax.swing.GroupLayout(graphContainerPanel);
        graphContainerPanel.setLayout(graphContainerPanelLayout);
        graphContainerPanelLayout.setHorizontalGroup(
            graphContainerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(graphContainerPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(graphContainerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(actualsalesAnalysisPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(graphContainerPanelLayout.createSequentialGroup()
                        .addComponent(completedLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(totalOrdersLabel5)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        graphContainerPanelLayout.setVerticalGroup(
            graphContainerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(graphContainerPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(graphContainerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(completedLabel2)
                    .addComponent(totalOrdersLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(actualsalesAnalysisPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21))
        );

        accountNameButton.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        accountNameButton.setText("(name)");
        accountNameButton.setBorderPainted(false);
        accountNameButton.setContentAreaFilled(false);
        accountNameButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        accountNameButton.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        accountNameButton.setMargin(new java.awt.Insets(2, 14, 3, 0));
        accountNameButton.addActionListener(this::accountNameButtonActionPerformed);

        graphContainerPanel1.setBackground(new java.awt.Color(255, 255, 255));
        graphContainerPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));

        completedLabel3.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
        completedLabel3.setText("Catalog Stock");

        catalogStockTextArea.setColumns(20);
        catalogStockTextArea.setRows(5);
        jScrollPane2.setViewportView(catalogStockTextArea);

        javax.swing.GroupLayout graphContainerPanel1Layout = new javax.swing.GroupLayout(graphContainerPanel1);
        graphContainerPanel1.setLayout(graphContainerPanel1Layout);
        graphContainerPanel1Layout.setHorizontalGroup(
            graphContainerPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(graphContainerPanel1Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(graphContainerPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(completedLabel3)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 279, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(21, Short.MAX_VALUE))
        );
        graphContainerPanel1Layout.setVerticalGroup(
            graphContainerPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(graphContainerPanel1Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(completedLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 253, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel9.setFont(new java.awt.Font("Poppins", 1, 24)); // NOI18N
        jLabel9.setText("DASHBOARD");

        javax.swing.GroupLayout dashboardPanelLayout = new javax.swing.GroupLayout(dashboardPanel);
        dashboardPanel.setLayout(dashboardPanelLayout);
        dashboardPanelLayout.setHorizontalGroup(
            dashboardPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dashboardPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(dashboardPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(dashboardPanelLayout.createSequentialGroup()
                        .addComponent(graphContainerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(graphContainerPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(dashboardPanelLayout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(accountNameButton, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(21, 21, 21))
        );
        dashboardPanelLayout.setVerticalGroup(
            dashboardPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dashboardPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(dashboardPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addComponent(accountNameButton))
                .addGap(21, 21, 21)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(dashboardPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(graphContainerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(graphContainerPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(48, Short.MAX_VALUE))
        );

        mainPanel.add(dashboardPanel, "dashboard");

        javax.swing.GroupLayout salesReportPanelLayout = new javax.swing.GroupLayout(salesReportPanel);
        salesReportPanel.setLayout(salesReportPanelLayout);
        salesReportPanelLayout.setHorizontalGroup(
            salesReportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 894, Short.MAX_VALUE)
        );
        salesReportPanelLayout.setVerticalGroup(
            salesReportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 703, Short.MAX_VALUE)
        );

        mainPanel.add(salesReportPanel, "salesReport");

        jLabel2.setFont(new java.awt.Font("Poppins", 1, 24)); // NOI18N
        jLabel2.setText("ACCOUNTS");

        jLabel4.setFont(new java.awt.Font("Poppins", 1, 14)); // NOI18N
        jLabel4.setText("CURRENT ACCOUNT");

        jLabel5.setFont(new java.awt.Font("Poppins", 1, 14)); // NOI18N
        jLabel5.setText("ALL ACCOUNTS");

        allUsersTable.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        allUsersTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "ID", "Name", "Username", "Role", "Status"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Object.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane3.setViewportView(allUsersTable);

        addAccountButton.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        addAccountButton.setText("Add Account");
        addAccountButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        addAccountButton.addActionListener(this::addAccountButtonActionPerformed);

        changeUsernameAllAccountsButton.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        changeUsernameAllAccountsButton.setText("Edit Account");
        changeUsernameAllAccountsButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        changeUsernameAllAccountsButton.addActionListener(this::changeUsernameAllAccountsButtonActionPerformed);

        idLabel.setFont(new java.awt.Font("Poppins", 1, 12)); // NOI18N
        idLabel.setText("ID:");

        nameLabel.setFont(new java.awt.Font("Poppins", 1, 12)); // NOI18N
        nameLabel.setText("Name:");

        usernameLabel.setFont(new java.awt.Font("Poppins", 1, 12)); // NOI18N
        usernameLabel.setText("Username:");

        roleLabel.setFont(new java.awt.Font("Poppins", 1, 12)); // NOI18N
        roleLabel.setText("Role:");

        actualIdLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        actualIdLabel.setText("(id)");

        actualNameLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        actualNameLabel.setText("(name)");

        actualUsernameLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        actualUsernameLabel.setText("(username)");

        actualRoleLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        actualRoleLabel.setText("(role)");

        javax.swing.GroupLayout accountsPanelLayout = new javax.swing.GroupLayout(accountsPanel);
        accountsPanel.setLayout(accountsPanelLayout);
        accountsPanelLayout.setHorizontalGroup(
            accountsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(accountsPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(accountsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(accountsPanelLayout.createSequentialGroup()
                        .addComponent(roleLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(actualRoleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(accountsPanelLayout.createSequentialGroup()
                        .addComponent(usernameLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(actualUsernameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel4)
                    .addComponent(jLabel5)
                    .addComponent(jLabel2)
                    .addGroup(accountsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(accountsPanelLayout.createSequentialGroup()
                            .addComponent(idLabel)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(actualIdLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(accountsPanelLayout.createSequentialGroup()
                            .addComponent(nameLabel)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(actualNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(accountsPanelLayout.createSequentialGroup()
                        .addComponent(addAccountButton, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(changeUsernameAllAccountsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 852, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(21, Short.MAX_VALUE))
        );
        accountsPanelLayout.setVerticalGroup(
            accountsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(accountsPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(accountsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(idLabel)
                    .addComponent(actualIdLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(accountsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nameLabel)
                    .addComponent(actualNameLabel))
                .addGap(6, 6, 6)
                .addGroup(accountsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(usernameLabel)
                    .addComponent(actualUsernameLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(accountsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(roleLabel)
                    .addComponent(actualRoleLabel))
                .addGap(31, 31, 31)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 328, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(accountsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(changeUsernameAllAccountsButton)
                    .addComponent(addAccountButton))
                .addContainerGap(74, Short.MAX_VALUE))
        );

        mainPanel.add(accountsPanel, "accounts");

        jTable1.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Customer", "Items", "Total", "Platform", "Payment", "Delivery", "Status", "Date", "Date Completed"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        createOrderButton.setBackground(new java.awt.Color(27, 25, 24));
        createOrderButton.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        createOrderButton.setForeground(new java.awt.Color(255, 255, 255));
        createOrderButton.setText("Create Order");
        createOrderButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        createOrderButton.addActionListener(this::createOrderButtonActionPerformed);

        jLabel6.setFont(new java.awt.Font("Poppins", 1, 24)); // NOI18N
        jLabel6.setText("ORDERS");

        updateStatusButton.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        updateStatusButton.setText("Update Status");
        updateStatusButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        updateStatusButton.addActionListener(this::updateStatusButtonActionPerformed);

        cancelOrderButton.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        cancelOrderButton.setText("Cancel Order");
        cancelOrderButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        cancelOrderButton.addActionListener(this::cancelOrderButtonActionPerformed);

        viewDetailsButton.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        viewDetailsButton.setText("View Details");
        viewDetailsButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        viewDetailsButton.addActionListener(this::viewDetailsButtonActionPerformed);

        editOrderButton.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        editOrderButton.setText("Edit Order");
        editOrderButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        editOrderButton.addActionListener(this::editOrderButtonActionPerformed);

        javax.swing.GroupLayout ordersPanelLayout = new javax.swing.GroupLayout(ordersPanel);
        ordersPanel.setLayout(ordersPanelLayout);
        ordersPanelLayout.setHorizontalGroup(
            ordersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ordersPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(ordersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addGroup(ordersPanelLayout.createSequentialGroup()
                        .addGroup(ordersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(updateStatusButton, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
                            .addComponent(createOrderButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(ordersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(editOrderButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(viewDetailsButton, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cancelOrderButton, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 842, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(31, Short.MAX_VALUE))
        );
        ordersPanelLayout.setVerticalGroup(
            ordersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ordersPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(ordersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(createOrderButton)
                    .addComponent(cancelOrderButton)
                    .addComponent(editOrderButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(ordersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(viewDetailsButton)
                    .addComponent(updateStatusButton))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 513, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(38, Short.MAX_VALUE))
        );

        mainPanel.add(ordersPanel, "orders");

        catalogTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Name", "Type", "Price", "Stock/Unit", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane4.setViewportView(catalogTable);

        addProductServiceButton.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        addProductServiceButton.setText("Add Product/Service");
        addProductServiceButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        addProductServiceButton.addActionListener(this::addProductServiceButtonActionPerformed);

        editProductServiceButton.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        editProductServiceButton.setText("Edit Product/Service");
        editProductServiceButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        editProductServiceButton.addActionListener(this::editProductServiceButtonActionPerformed);

        jLabel7.setFont(new java.awt.Font("Poppins", 1, 24)); // NOI18N
        jLabel7.setText("CATALOG");

        javax.swing.GroupLayout catalogPanelLayout = new javax.swing.GroupLayout(catalogPanel);
        catalogPanel.setLayout(catalogPanelLayout);
        catalogPanelLayout.setHorizontalGroup(
            catalogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(catalogPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(catalogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 845, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(catalogPanelLayout.createSequentialGroup()
                        .addComponent(addProductServiceButton, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(editProductServiceButton, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(28, Short.MAX_VALUE))
        );
        catalogPanelLayout.setVerticalGroup(
            catalogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(catalogPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(jLabel7)
                .addGap(18, 18, 18)
                .addGroup(catalogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addProductServiceButton)
                    .addComponent(editProductServiceButton))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 563, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        mainPanel.add(catalogPanel, "card5");

        jLabel8.setFont(new java.awt.Font("Poppins", 1, 24)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(20, 6, 10));
        jLabel8.setText("ACTIVITY LOG");

        activityLogTable.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        activityLogTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Username", "Role", "Action", "Details", "Date/Time"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane5.setViewportView(activityLogTable);

        activityStartDateChooser.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N

        jLabel10.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel10.setText("START DATE");

        jLabel11.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel11.setText("END DATE");

        activityEndDateChooser.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N

        userComboBox.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        userComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel12.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel12.setText("USER");

        generateLogButton.setBackground(new java.awt.Color(27, 25, 24));
        generateLogButton.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        generateLogButton.setForeground(new java.awt.Color(255, 255, 255));
        generateLogButton.setText("Generate Log");
        generateLogButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        generateLogButton.addActionListener(this::generateLogButtonActionPerformed);

        resetLogButton.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        resetLogButton.setText("Reset");
        resetLogButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout activityLogPanelLayout = new javax.swing.GroupLayout(activityLogPanel);
        activityLogPanel.setLayout(activityLogPanelLayout);
        activityLogPanelLayout.setHorizontalGroup(
            activityLogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(activityLogPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(activityLogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(activityLogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(activityStartDateChooser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel10)
                        .addComponent(jLabel11)
                        .addComponent(activityEndDateChooser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(userComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12)
                    .addComponent(generateLogButton)
                    .addComponent(resetLogButton))
                .addGap(21, 21, 21)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 663, Short.MAX_VALUE)
                .addGap(23, 23, 23))
        );
        activityLogPanelLayout.setVerticalGroup(
            activityLogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(activityLogPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(activityLogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 647, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(activityLogPanelLayout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(activityStartDateChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(activityEndDateChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(userComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(43, 43, 43)
                        .addComponent(generateLogButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(resetLogButton)))
                .addContainerGap(35, Short.MAX_VALUE))
        );

        mainPanel.add(activityLogPanel, "card6");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(optionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(optionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void dashboardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dashboardButtonActionPerformed

    cl.show(mainPanel, "dashboard");      
    }//GEN-LAST:event_dashboardButtonActionPerformed

    private void salesReportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_salesReportButtonActionPerformed
        cl.show(mainPanel, "salesReport");
    }//GEN-LAST:event_salesReportButtonActionPerformed

    private void accountsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_accountsButtonActionPerformed
        cl.show(mainPanel, "accounts");
    }//GEN-LAST:event_accountsButtonActionPerformed

    private void logOutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logOutButtonActionPerformed
         addLog("LOGOUT", "User logged out");

    new LoginForm().setVisible(true);
    dispose();
    }//GEN-LAST:event_logOutButtonActionPerformed

    private void createOrderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createOrderButtonActionPerformed
java.util.List<OrderItem> items = loadOrderItems();

JDialog dialog = new JDialog(this, "Create Order", true);
dialog.setSize(800, 560);
dialog.setLocationRelativeTo(this);
dialog.setLayout(new BorderLayout());
dialog.getContentPane().setBackground(Color.WHITE);

JPanel headerPanel = new JPanel(new BorderLayout());
headerPanel.setBackground(Color.WHITE);
headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 22, 14, 22));

JLabel titleLabel = new JLabel("Create Order");
titleLabel.setFont(new Font("Poppins", Font.BOLD, 22));

JLabel subtitleLabel = new JLabel("Add customer details, delivery, payment, and order items.");
subtitleLabel.setFont(new Font("Poppins", Font.PLAIN, 12));
subtitleLabel.setForeground(new Color(100, 100, 100));

JPanel titlePanel = new JPanel(new GridLayout(0, 1));
titlePanel.setBackground(Color.WHITE);
titlePanel.add(titleLabel);
titlePanel.add(subtitleLabel);

headerPanel.add(titlePanel, BorderLayout.WEST);

JPanel customerPanel = new JPanel(new GridLayout(0, 2, 10, 8));
customerPanel.setBackground(Color.WHITE);
customerPanel.setBorder(BorderFactory.createEmptyBorder(0, 22, 10, 22));

JPanel orderDetailsPanel = new JPanel(new GridLayout(0, 2, 10, 8));
orderDetailsPanel.setBackground(Color.WHITE);
orderDetailsPanel.setBorder(BorderFactory.createEmptyBorder(12, 22, 0, 22));

JTextField customerNameField = new JTextField();

String[] platforms = {"Facebook", "Shopee", "Lazada", "TikTok"};
JComboBox<String> platformBox = new JComboBox<>(platforms);

String[] payments = {"Cash", "GCash", "Bank Transfer", "COD"};
JComboBox<String> paymentBox = new JComboBox<>(payments);

String[] deliveries = {"Pickup", "Meetup", "J&T", "Flash", "LBC", "Shopee", "Lazada", "TikTok"};
JComboBox<String> deliveryBox = new JComboBox<>(deliveries);

String[] statuses = {"Pending", "Processing", "Completed"};
JComboBox<String> statusBox = new JComboBox<>(statuses);

platformBox.addActionListener(e -> {
    String platform = (String) platformBox.getSelectedItem();

    if ("Shopee".equals(platform) || "Lazada".equals(platform) || "TikTok".equals(platform)) {
        deliveryBox.setSelectedItem(platform);
        deliveryBox.setEnabled(false);
    } else {
        deliveryBox.setEnabled(true);
        deliveryBox.setSelectedItem("Pickup");
    }
});

customerPanel.add(new JLabel("Customer Name"));
customerPanel.add(customerNameField);
orderDetailsPanel.add(new JLabel("Platform"));
orderDetailsPanel.add(platformBox);
orderDetailsPanel.add(new JLabel("Payment Method"));
orderDetailsPanel.add(paymentBox);
orderDetailsPanel.add(new JLabel("Delivery Method"));
orderDetailsPanel.add(deliveryBox);
orderDetailsPanel.add(new JLabel("Status"));
orderDetailsPanel.add(statusBox);

DefaultTableModel itemModel = new DefaultTableModel(
    new Object[]{"Product / Service", "Price", "Quantity", "Remove"}, 0
) {
    @Override
    public boolean isCellEditable(int row, int column) {
        return true;
    }
};

JTable itemTable = new JTable(itemModel);
itemTable.setRowHeight(28);

JComboBox<OrderItem> itemEditorBox = new JComboBox<>();
itemEditorBox.setEditable(true);

for (OrderItem item : items) {
    itemEditorBox.addItem(item);
}

itemTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(itemEditorBox));
itemTable.getColumnModel().getColumn(3).setCellRenderer(new RemoveButtonRenderer());
itemTable.getColumnModel().getColumn(3).setCellEditor(new RemoveButtonEditor(new JCheckBox(), itemTable));

itemTable.getColumnModel().getColumn(0).setPreferredWidth(340);
itemTable.getColumnModel().getColumn(1).setPreferredWidth(100);
itemTable.getColumnModel().getColumn(2).setPreferredWidth(100);
itemTable.getColumnModel().getColumn(3).setPreferredWidth(100);

if (!items.isEmpty()) {
    OrderItem first = items.get(0);
    itemModel.addRow(new Object[]{first, first.price, "", "Remove"});
} else {
    itemModel.addRow(new Object[]{"", "", "", "Remove"});
}

JScrollPane itemScroll = new JScrollPane(itemTable);
itemScroll.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 22));

JButton addItemButton = new JButton("+ Add Item");
JPanel itemButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
itemButtonPanel.setBackground(Color.WHITE);
itemButtonPanel.setBorder(BorderFactory.createEmptyBorder(8, 22, 0, 22));
itemButtonPanel.add(addItemButton);

JPanel itemPanel = new JPanel(new BorderLayout());
itemPanel.setBackground(Color.WHITE);
itemPanel.add(itemScroll, BorderLayout.CENTER);
itemPanel.add(itemButtonPanel, BorderLayout.SOUTH);

JLabel totalLabel = new JLabel("Total: PHP 0.00");
totalLabel.setFont(new Font("Poppins", Font.BOLD, 14));

JButton saveButton = new JButton("Save Order");
JButton cancelButton = new JButton("Cancel");

Runnable updateTotal = () -> {
    double total = 0;

    for (int i = 0; i < itemModel.getRowCount(); i++) {
        try {
            double price = Double.parseDouble(itemModel.getValueAt(i, 1).toString());
            int quantity = Integer.parseInt(itemModel.getValueAt(i, 2).toString());

            if (price > 0 && quantity > 0) {
                total += price * quantity;
            }
        } catch (Exception ignored) {
        }
    }

    totalLabel.setText("Total: PHP " + String.format("%.2f", total));
};

itemModel.addTableModelListener(e -> {
    if (e.getColumn() == 0) {
        int changedRow = e.getFirstRow();

        if (changedRow >= 0 && changedRow < itemModel.getRowCount()) {
            Object selected = itemModel.getValueAt(changedRow, 0);

            if (selected instanceof OrderItem item) {
                itemModel.setValueAt(item.price, changedRow, 1);
            }
        }
    }

    updateTotal.run();
});

addItemButton.addActionListener(e -> {
    if (!items.isEmpty()) {
        OrderItem first = items.get(0);
        itemModel.addRow(new Object[]{first, first.price, "", "Remove"});
    } else {
        itemModel.addRow(new Object[]{"", "", "", "Remove"});
    }

    updateTotal.run();
});

cancelButton.addActionListener(e -> dialog.dispose());

saveButton.addActionListener(e -> {
    if (itemTable.isEditing()) {
        itemTable.getCellEditor().stopCellEditing();
    }

    try {
        String customerName = customerNameField.getText().trim();

        if (customerName.isEmpty()) {
            customerName = "Walk-in";
        }

        java.util.List<OrderLine> lines = new java.util.ArrayList<>();
        java.util.Map<Integer, Integer> productQuantities = new java.util.HashMap<>();

        for (int i = 0; i < itemModel.getRowCount(); i++) {
            Object selected = itemModel.getValueAt(i, 0);

            String itemName;
            String itemType = "custom";
            int itemId = 0;
            OrderItem selectedOrderItem = null;

            if (selected instanceof OrderItem item) {
                selectedOrderItem = item;
                itemId = item.id;
                itemType = item.type;
                itemName = item.name;
            } else {
                itemName = selected.toString().trim();
            }

            if (itemName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Enter a product or service name.");
                return;
            }

            double price = Double.parseDouble(itemModel.getValueAt(i, 1).toString());
            int quantity = Integer.parseInt(itemModel.getValueAt(i, 2).toString());

            if (price <= 0 || quantity <= 0) {
                JOptionPane.showMessageDialog(dialog, "Price and quantity must be greater than 0.");
                return;
            }

            if (selectedOrderItem != null && selectedOrderItem.type.equalsIgnoreCase("product")) {
                int currentQty = productQuantities.getOrDefault(selectedOrderItem.id, 0);
                int newTotalQty = currentQty + quantity;

                if (newTotalQty > selectedOrderItem.stock) {
                    JOptionPane.showMessageDialog(
                        dialog,
                        "Not enough stock for " + selectedOrderItem.name +
                        ". Available stock: " + selectedOrderItem.stock
                    );
                    return;
                }

                productQuantities.put(selectedOrderItem.id, newTotalQty);
            }

            lines.add(new OrderLine(itemId, itemType, itemName, price, quantity));
        }

        String platform = (String) platformBox.getSelectedItem();
        String paymentMethod = (String) paymentBox.getSelectedItem();
        String deliveryMethod = (String) deliveryBox.getSelectedItem();
        String status = (String) statusBox.getSelectedItem();

        saveOrderWithItems(customerName, lines, platform, paymentMethod, deliveryMethod, status);
        
        addLog("CREATE_ORDER", "Created order for " + customerName + " via " + platform);

        JOptionPane.showMessageDialog(dialog, "Order created successfully!");

        dialog.dispose();

        loadOrders();
        loadCatalog();
        loadDashboard();

    } catch (NumberFormatException ex) {
        JOptionPane.showMessageDialog(dialog, "Price and quantity must be valid numbers.");
    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(dialog, "Order error.");
    }
});

JPanel footerPanel = new JPanel(new BorderLayout());
footerPanel.setBackground(Color.WHITE);
footerPanel.setBorder(BorderFactory.createEmptyBorder(14, 22, 18, 22));

JPanel leftFooter = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
leftFooter.setBackground(Color.WHITE);

JPanel rightFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
rightFooter.setBackground(Color.WHITE);
rightFooter.add(totalLabel);
rightFooter.add(cancelButton);
rightFooter.add(saveButton);

footerPanel.add(leftFooter, BorderLayout.WEST);
footerPanel.add(rightFooter, BorderLayout.EAST);

JPanel centerPanel = new JPanel(new BorderLayout());
centerPanel.setBackground(Color.WHITE);
centerPanel.add(customerPanel, BorderLayout.NORTH);
centerPanel.add(itemPanel, BorderLayout.CENTER);
centerPanel.add(orderDetailsPanel, BorderLayout.SOUTH);

dialog.add(headerPanel, BorderLayout.NORTH);
dialog.add(centerPanel, BorderLayout.CENTER);
dialog.add(footerPanel, BorderLayout.SOUTH);

styleFlatComponents(dialog);
dialog.setVisible(true);
    }//GEN-LAST:event_createOrderButtonActionPerformed

    private boolean showAddAccountDialog() {
        if (!isAdmin()) {
            JOptionPane.showMessageDialog(this, "Only admins can add accounts.");
            return true;
        }

        JTextField nameField = new JTextField();
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"admin", "employee"});
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"active", "inactive"});

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Confirm Password:"));
        panel.add(confirmPasswordField);
        panel.add(new JLabel("Role:"));
        panel.add(roleBox);
        panel.add(new JLabel("Status:"));
        panel.add(statusBox);

        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Add Account",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return true;

        try {
            String name = nameField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String confirmPassword = new String(confirmPasswordField.getPassword()).trim();
            String role = (String) roleBox.getSelectedItem();
            String status = (String) statusBox.getSelectedItem();

            if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name, username, and password are required.");
                return true;
            }

            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match.");
                return true;
            }

            Connection conn = DBConnection.getConnection();

            PreparedStatement checkPs = conn.prepareStatement(
                "SELECT COUNT(*) FROM users WHERE username=?"
            );
            checkPs.setString(1, username);
            ResultSet checkRs = checkPs.executeQuery();

            if (checkRs.next() && checkRs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this, "Username is already taken.");
                return true;
            }

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (name, username, password, role, status) VALUES (?, ?, ?, ?, ?)"
            );

            ps.setString(1, name);
            ps.setString(2, username);
            ps.setString(3, password);
            ps.setString(4, role);
            ps.setString(5, status);
            ps.executeUpdate();

            addLog("ADD_ACCOUNT", "Added account: " + username + " as " + role);
            JOptionPane.showMessageDialog(this, "Account added successfully!");
            loadAllUsers();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to add account.");
        }

        return true;
    }

    private void addAccountButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAccountButtonActionPerformed
    if (showAddAccountDialog()) return;
 if (!isAdmin()) {
        JOptionPane.showMessageDialog(this, "Only admins can add accounts.");
        return;
    }

    try {
        String name = JOptionPane.showInputDialog("Enter name:");
        if (name == null) return;

        String username = JOptionPane.showInputDialog("Enter username:");
        if (username == null) return;

        String password = JOptionPane.showInputDialog("Enter password:");
        if (password == null) return;

        String[] roles = {"admin", "employee"};
JComboBox<String> roleBox = new JComboBox<>(roles);

int roleChoice = JOptionPane.showConfirmDialog(
        this,
        roleBox,
        "Select Role",
        JOptionPane.OK_CANCEL_OPTION
);

if (roleChoice != JOptionPane.OK_OPTION) return;

String role = (String) roleBox.getSelectedItem();

        // ✅ STATUS DROPDOWN
        String[] statuses = {"active", "inactive"};
        JComboBox<String> statusBox = new JComboBox<>(statuses);

        int statusChoice = JOptionPane.showConfirmDialog(
                this,
                statusBox,
                "Select Status",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (statusChoice != JOptionPane.OK_OPTION) return;

        String status = (String) statusBox.getSelectedItem();

        Connection conn = DBConnection.getConnection();

        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO users (name, username, password, role, status) VALUES (?, ?, ?, ?, ?)"
        );

        ps.setString(1, name);
        ps.setString(2, username);
        ps.setString(3, password);
        ps.setString(4, role);
        ps.setString(5, status);

        ps.executeUpdate();
        
        addLog("ADD_ACCOUNT", "Added account: " + username + " as " + role);

        JOptionPane.showMessageDialog(this, "Account added successfully!");
        loadAllUsers();

    } catch (Exception e) {
        e.printStackTrace();
    }
    }//GEN-LAST:event_addAccountButtonActionPerformed

    private void catalogButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_catalogButtonActionPerformed
        loadCatalog();
    cl.show(mainPanel, "card5");
    }//GEN-LAST:event_catalogButtonActionPerformed

    private void updateStatusButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateStatusButtonActionPerformed
int row = jTable1.getSelectedRow();

    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Select an order first.");
        return;
    }

    int orderId = parseOrderCode(jTable1.getValueAt(row, 0));
    String currentStatus = String.valueOf(jTable1.getValueAt(row, 7));

    String[] statuses = {"Pending", "Processing", "Completed"};
    JComboBox<String> statusBox = new JComboBox<>(statuses);

    if (!currentStatus.equalsIgnoreCase("Cancelled")) {
        statusBox.setSelectedItem(currentStatus);
    }

    int choice = JOptionPane.showConfirmDialog(
        this,
        statusBox,
        "Update Order Status",
        JOptionPane.OK_CANCEL_OPTION
    );

    if (choice != JOptionPane.OK_OPTION) return;

    String newStatus = (String) statusBox.getSelectedItem();

    Connection conn = null;

    try {
        conn = DBConnection.getConnection();
        conn.setAutoCommit(false);

        if (currentStatus.equalsIgnoreCase("Cancelled")
                && !newStatus.equalsIgnoreCase("Cancelled")) {

            PreparedStatement itemsPs = conn.prepareStatement(
                "SELECT item_id, item_name, quantity FROM order_items " +
                "WHERE order_id=? AND item_type='product' AND item_id IS NOT NULL"
            );

            itemsPs.setInt(1, orderId);
            ResultSet itemsRs = itemsPs.executeQuery();

            PreparedStatement stockPs = conn.prepareStatement(
                "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?"
            );

            while (itemsRs.next()) {
                int itemId = itemsRs.getInt("item_id");
                int quantity = itemsRs.getInt("quantity");
                String itemName = itemsRs.getString("item_name");

                stockPs.setInt(1, quantity);
                stockPs.setInt(2, itemId);
                stockPs.setInt(3, quantity);

                int updated = stockPs.executeUpdate();

                if (updated == 0) {
                    throw new SQLException("Not enough stock for " + itemName);
                }
            }
        }

        PreparedStatement ps = conn.prepareStatement(
            "UPDATE orders SET status=?, date_completed = CASE WHEN ? = 'Completed' THEN CURRENT_TIMESTAMP ELSE NULL END WHERE id=?"
        );

        ps.setString(1, newStatus);
        ps.setString(2, newStatus);
        ps.setInt(3, orderId);

        ps.executeUpdate();

        conn.commit();

        addLog("UPDATE_ORDER_STATUS", "Updated order " + formatOrderCode(orderId) + " from " + currentStatus + " to " + newStatus);

        JOptionPane.showMessageDialog(this, "Order status updated.");
        loadOrders();
        loadCatalog();
        loadDashboard();

    } catch (Exception e) {
        try {
            if (conn != null) conn.rollback();
        } catch (Exception rollbackError) {
            rollbackError.printStackTrace();
        }

        e.printStackTrace();
        JOptionPane.showMessageDialog(this, e.getMessage());
    } finally {
        try {
            if (conn != null) conn.setAutoCommit(true);
        } catch (Exception autoCommitError) {
            autoCommitError.printStackTrace();
        }
    }
    }//GEN-LAST:event_updateStatusButtonActionPerformed

    private void cancelOrderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelOrderButtonActionPerformed
    int row = jTable1.getSelectedRow();

    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Select an order first.");
        return;
    }

    int orderId = parseOrderCode(jTable1.getValueAt(row, 0));

    Object currentStatus = jTable1.getValueAt(row, 7);
    if (currentStatus != null && currentStatus.toString().equalsIgnoreCase("Cancelled")) {
        JOptionPane.showMessageDialog(this, "This order is already cancelled.");
        return;
    }

    int confirm = JOptionPane.showConfirmDialog(
        this,
        "Cancel selected order and return product stock?",
        "Confirm Cancel",
        JOptionPane.YES_NO_OPTION
    );

    if (confirm != JOptionPane.YES_OPTION) return;

    Connection conn = null;

    try {
        conn = DBConnection.getConnection();
        conn.setAutoCommit(false);

        PreparedStatement oldItemsPs = conn.prepareStatement(
            "SELECT item_id, quantity FROM order_items WHERE order_id=? AND item_type='product' AND item_id IS NOT NULL"
        );

        oldItemsPs.setInt(1, orderId);

        ResultSet oldItemsRs = oldItemsPs.executeQuery();

        PreparedStatement returnStockPs = conn.prepareStatement(
            "UPDATE products SET stock = stock + ? WHERE id=?"
        );

        while (oldItemsRs.next()) {
            returnStockPs.setInt(1, oldItemsRs.getInt("quantity"));
            returnStockPs.setInt(2, oldItemsRs.getInt("item_id"));
            returnStockPs.executeUpdate();
        }

        PreparedStatement cancelPs = conn.prepareStatement(
            "UPDATE orders SET status='Cancelled', date_completed=NULL WHERE id=?"
        );

        cancelPs.setInt(1, orderId);
        cancelPs.executeUpdate();

        conn.commit();
        
        addLog("CANCEL_ORDER", "Cancelled order " + formatOrderCode(orderId) + " and returned stock");

        JOptionPane.showMessageDialog(this, "Order cancelled and stock returned.");
        loadOrders();
        loadCatalog();
loadDashboard();
    } catch (Exception e) {
        try {
            if (conn != null) conn.rollback();
        } catch (Exception rollbackError) {
            rollbackError.printStackTrace();
        }

        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to cancel order.");
    } finally {
        try {
            if (conn != null) conn.setAutoCommit(true);
        } catch (Exception autoCommitError) {
            autoCommitError.printStackTrace();
        }
    }
    }//GEN-LAST:event_cancelOrderButtonActionPerformed

    private void viewDetailsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewDetailsButtonActionPerformed
 int row = jTable1.getSelectedRow();

    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Select an order first.");
        return;
    }

    int orderId = parseOrderCode(jTable1.getValueAt(row, 0));
    String status = String.valueOf(jTable1.getValueAt(row, 7));

    Color statusBg;
    Color statusFg;

    if (status.equalsIgnoreCase("Completed")) {
        statusBg = new Color(210, 245, 220);
        statusFg = new Color(20, 90, 45);
    } else if (status.equalsIgnoreCase("Cancelled")) {
        statusBg = new Color(255, 215, 215);
        statusFg = new Color(130, 25, 25);
    } else if (status.equalsIgnoreCase("Processing")) {
        statusBg = new Color(220, 235, 255);
        statusFg = new Color(20, 70, 130);
    } else {
        statusBg = new Color(255, 245, 210);
        statusFg = new Color(120, 80, 10);
    }

    JDialog dialog = new JDialog(this, "Order Details", true);
    dialog.setSize(650, 560);
    dialog.setLocationRelativeTo(this);
    dialog.setLayout(new BorderLayout());

    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 20, 14, 20));
    headerPanel.setBackground(Color.WHITE);

    JLabel titleLabel = new JLabel("Order " + formatOrderCode(orderId));
    titleLabel.setFont(new Font("Poppins", Font.BOLD, 22));

    JLabel statusLabel = new JLabel(status);
    statusLabel.setOpaque(true);
    statusLabel.setBackground(statusBg);
    statusLabel.setForeground(statusFg);
    statusLabel.setFont(new Font("Poppins", Font.BOLD, 12));
    statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
    statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

    headerPanel.add(titleLabel, BorderLayout.WEST);
    headerPanel.add(statusLabel, BorderLayout.EAST);

    JPanel infoPanel = new JPanel(new GridBagLayout());
    infoPanel.setBackground(Color.WHITE);
    infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(6, 0, 6, 0);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    addDetailRow(infoPanel, gbc, 0, "Customer", jTable1.getValueAt(row, 1));
    addDetailRow(infoPanel, gbc, 1, "Platform", jTable1.getValueAt(row, 4));
    addDetailRow(infoPanel, gbc, 2, "Total", "PHP " + jTable1.getValueAt(row, 3));
 addDetailRow(infoPanel, gbc, 3, "Payment", jTable1.getValueAt(row, 5));
addDetailRow(infoPanel, gbc, 4, "Delivery", jTable1.getValueAt(row, 6));
addDetailRow(infoPanel, gbc, 5, "Date Ordered", jTable1.getValueAt(row, 8));
addDetailRow(infoPanel, gbc, 6, "Date Completed", jTable1.getValueAt(row, 9));

    DefaultTableModel itemModel = new DefaultTableModel(
        new Object[]{"Item", "Type", "Price", "Quantity", "Subtotal"}, 0
    ) {
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    JTable itemTable = new JTable(itemModel);

    try {
        Connection conn = DBConnection.getConnection();

        PreparedStatement ps = conn.prepareStatement(
            "SELECT item_name, item_type, price, quantity, subtotal FROM order_items WHERE order_id=?"
        );

        ps.setInt(1, orderId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            itemModel.addRow(new Object[]{
                rs.getString("item_name"),
                rs.getString("item_type"),
                rs.getDouble("price"),
                rs.getInt("quantity"),
                rs.getDouble("subtotal")
            });
        }

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to load order items.");
        return;
    }

    JScrollPane itemScroll = new JScrollPane(itemTable);
    itemScroll.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.setBackground(Color.WHITE);
    centerPanel.add(infoPanel, BorderLayout.NORTH);
    centerPanel.add(itemScroll, BorderLayout.CENTER);

    JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    footerPanel.setBackground(Color.WHITE);
    footerPanel.setBorder(BorderFactory.createEmptyBorder(8, 20, 18, 20));

    JButton closeButton = new JButton("Close");
    closeButton.addActionListener(e -> dialog.dispose());

    JButton printReceiptButton = new JButton("Print Receipt");
    printReceiptButton.addActionListener(e -> showReceiptDialog(orderId, row, itemModel));

    footerPanel.add(printReceiptButton);
    footerPanel.add(closeButton);

    dialog.add(headerPanel, BorderLayout.NORTH);
    dialog.add(centerPanel, BorderLayout.CENTER);
    dialog.add(footerPanel, BorderLayout.SOUTH);

    styleFlatComponents(dialog);
    dialog.setVisible(true);
    }//GEN-LAST:event_viewDetailsButtonActionPerformed

    private void editOrderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editOrderButtonActionPerformed
int row = jTable1.getSelectedRow();

    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Select an order first.");
        return;
    }
    
    String currentStatus = String.valueOf(jTable1.getValueAt(row, 7));

if (currentStatus.equalsIgnoreCase("Cancelled")) {
    JOptionPane.showMessageDialog(this, "Cancelled orders cannot be edited. Change the status first.");
    return;
}

    int orderId = parseOrderCode(jTable1.getValueAt(row, 0));
    java.util.List<OrderItem> items = loadOrderItems();

    JDialog dialog = new JDialog(this, "Edit Order", true);
    dialog.setSize(800, 560);
    dialog.setLocationRelativeTo(this);
    dialog.setLayout(new BorderLayout());
    dialog.getContentPane().setBackground(Color.WHITE);

    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setBackground(Color.WHITE);
    headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 22, 14, 22));

    JLabel titleLabel = new JLabel("Edit Order " + formatOrderCode(orderId));
    titleLabel.setFont(new Font("Poppins", Font.BOLD, 22));

    JLabel subtitleLabel = new JLabel("Update customer details, delivery, payment, and order items.");
    subtitleLabel.setFont(new Font("Poppins", Font.PLAIN, 12));
    subtitleLabel.setForeground(new Color(100, 100, 100));

    JPanel titlePanel = new JPanel(new GridLayout(0, 1));
    titlePanel.setBackground(Color.WHITE);
    titlePanel.add(titleLabel);
    titlePanel.add(subtitleLabel);

    headerPanel.add(titlePanel, BorderLayout.WEST);

    JPanel customerPanel = new JPanel(new GridLayout(0, 2, 10, 8));
    customerPanel.setBackground(Color.WHITE);
    customerPanel.setBorder(BorderFactory.createEmptyBorder(0, 22, 10, 22));

    JPanel orderDetailsPanel = new JPanel(new GridLayout(0, 2, 10, 8));
    orderDetailsPanel.setBackground(Color.WHITE);
    orderDetailsPanel.setBorder(BorderFactory.createEmptyBorder(12, 22, 0, 22));

    JTextField customerNameField = new JTextField(jTable1.getValueAt(row, 1).toString());

    String[] platforms = {"Facebook", "Shopee", "Lazada", "TikTok"};
    JComboBox<String> platformBox = new JComboBox<>(platforms);
    platformBox.setSelectedItem(jTable1.getValueAt(row, 4).toString());

    String[] payments = {"Cash", "GCash", "Bank Transfer", "COD"};
    JComboBox<String> paymentBox = new JComboBox<>(payments);
    paymentBox.setSelectedItem(jTable1.getValueAt(row, 5).toString());

    String[] deliveries = {"Pickup", "Meetup", "J&T", "Flash", "LBC", "Shopee", "Lazada", "TikTok"};
    JComboBox<String> deliveryBox = new JComboBox<>(deliveries);
    deliveryBox.setSelectedItem(jTable1.getValueAt(row, 6).toString());

    String[] statuses = {"Pending", "Processing", "Completed", "Cancelled"};
    JComboBox<String> statusBox = new JComboBox<>(statuses);
    statusBox.setSelectedItem(jTable1.getValueAt(row, 7).toString());

    platformBox.addActionListener(e -> {
        String platform = (String) platformBox.getSelectedItem();

        if ("Shopee".equals(platform) || "Lazada".equals(platform) || "TikTok".equals(platform)) {
            deliveryBox.setSelectedItem(platform);
            deliveryBox.setEnabled(false);
        } else {
            deliveryBox.setEnabled(true);
        }
    });

    String initialPlatform = (String) platformBox.getSelectedItem();
    if ("Shopee".equals(initialPlatform) || "Lazada".equals(initialPlatform) || "TikTok".equals(initialPlatform)) {
        deliveryBox.setSelectedItem(initialPlatform);
        deliveryBox.setEnabled(false);
    }

    customerPanel.add(new JLabel("Customer Name"));
    customerPanel.add(customerNameField);
    orderDetailsPanel.add(new JLabel("Platform"));
    orderDetailsPanel.add(platformBox);
    orderDetailsPanel.add(new JLabel("Payment Method"));
    orderDetailsPanel.add(paymentBox);
    orderDetailsPanel.add(new JLabel("Delivery Method"));
    orderDetailsPanel.add(deliveryBox);
    orderDetailsPanel.add(new JLabel("Status"));
    orderDetailsPanel.add(statusBox);

    DefaultTableModel itemModel = new DefaultTableModel(
        new Object[]{"Product / Service", "Price", "Quantity", "Remove"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return true;
        }
    };

    JTable itemTable = new JTable(itemModel);
    itemTable.setRowHeight(28);

    JComboBox<OrderItem> itemEditorBox = new JComboBox<>();
    itemEditorBox.setEditable(true);

    for (OrderItem item : items) {
        itemEditorBox.addItem(item);
    }

    itemTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(itemEditorBox));
    itemTable.getColumnModel().getColumn(3).setCellRenderer(new RemoveButtonRenderer());
    itemTable.getColumnModel().getColumn(3).setCellEditor(new RemoveButtonEditor(new JCheckBox(), itemTable));

    itemTable.getColumnModel().getColumn(0).setPreferredWidth(340);
    itemTable.getColumnModel().getColumn(1).setPreferredWidth(100);
    itemTable.getColumnModel().getColumn(2).setPreferredWidth(100);
    itemTable.getColumnModel().getColumn(3).setPreferredWidth(100);

    try {
        Connection conn = DBConnection.getConnection();

        PreparedStatement ps = conn.prepareStatement(
            "SELECT item_name, item_type, price, quantity FROM order_items WHERE order_id=?"
        );

        ps.setInt(1, orderId);

        ResultSet rs = ps.executeQuery();

        boolean hasItems = false;

        while (rs.next()) {
            hasItems = true;

            String itemName = rs.getString("item_name");
            String itemType = rs.getString("item_type");
            double price = rs.getDouble("price");
            int quantity = rs.getInt("quantity");

            Object selectedValue = itemName;

            for (OrderItem item : items) {
                if (item.name.equalsIgnoreCase(itemName) && item.type.equalsIgnoreCase(itemType)) {
                    selectedValue = item;
                    break;
                }
            }

            itemModel.addRow(new Object[]{selectedValue, price, quantity, "Remove"});
        }

        if (!hasItems) {
            if (!items.isEmpty()) {
                OrderItem first = items.get(0);
                itemModel.addRow(new Object[]{first, first.price, "", "Remove"});
            } else {
                itemModel.addRow(new Object[]{"", "", "", "Remove"});
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to load order items.");
        return;
    }

    JScrollPane itemScroll = new JScrollPane(itemTable);
    itemScroll.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 22));

    JButton addItemButton = new JButton("+ Add Item");
    JPanel itemButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    itemButtonPanel.setBackground(Color.WHITE);
    itemButtonPanel.setBorder(BorderFactory.createEmptyBorder(8, 22, 0, 22));
    itemButtonPanel.add(addItemButton);

    JPanel itemPanel = new JPanel(new BorderLayout());
    itemPanel.setBackground(Color.WHITE);
    itemPanel.add(itemScroll, BorderLayout.CENTER);
    itemPanel.add(itemButtonPanel, BorderLayout.SOUTH);

    JLabel totalLabel = new JLabel("Total: PHP 0.00");
    totalLabel.setFont(new Font("Poppins", Font.BOLD, 14));

    JButton saveButton = new JButton("Save Changes");
    JButton cancelButton = new JButton("Cancel");

    Runnable updateTotal = () -> {
        double total = 0;

        for (int i = 0; i < itemModel.getRowCount(); i++) {
            try {
                double price = Double.parseDouble(itemModel.getValueAt(i, 1).toString());
                int quantity = Integer.parseInt(itemModel.getValueAt(i, 2).toString());

                if (price > 0 && quantity > 0) {
                    total += price * quantity;
                }
            } catch (Exception ignored) {
            }
        }

        totalLabel.setText("Total: PHP " + String.format("%.2f", total));
    };

    itemModel.addTableModelListener(e -> {
        if (e.getColumn() == 0) {
            int changedRow = e.getFirstRow();

            if (changedRow >= 0 && changedRow < itemModel.getRowCount()) {
                Object selected = itemModel.getValueAt(changedRow, 0);

                if (selected instanceof OrderItem item) {
                    itemModel.setValueAt(item.price, changedRow, 1);
                }
            }
        }

        updateTotal.run();
    });

    updateTotal.run();

    addItemButton.addActionListener(e -> {
        if (!items.isEmpty()) {
            OrderItem first = items.get(0);
            itemModel.addRow(new Object[]{first, first.price, "", "Remove"});
        } else {
            itemModel.addRow(new Object[]{"", "", "", "Remove"});
        }

        updateTotal.run();
    });

    cancelButton.addActionListener(e -> dialog.dispose());

    saveButton.addActionListener(e -> {
        if (itemTable.isEditing()) {
            itemTable.getCellEditor().stopCellEditing();
        }

        try {
            String customerName = customerNameField.getText().trim();

            if (customerName.isEmpty()) {
                customerName = "Walk-in";
            }

            java.util.List<OrderLine> lines = new java.util.ArrayList<>();

            for (int i = 0; i < itemModel.getRowCount(); i++) {
                Object selected = itemModel.getValueAt(i, 0);

                String itemName;
                String itemType = "custom";
                int itemId = 0;

                if (selected instanceof OrderItem item) {
                    itemId = item.id;
                    itemType = item.type;
                    itemName = item.name;
                } else {
                    itemName = selected.toString().trim();
                }

                if (itemName.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Enter a product or service name.");
                    return;
                }

                double price = Double.parseDouble(itemModel.getValueAt(i, 1).toString());
                int quantity = Integer.parseInt(itemModel.getValueAt(i, 2).toString());

                if (price <= 0 || quantity <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Price and quantity must be greater than 0.");
                    return;
                }

                lines.add(new OrderLine(itemId, itemType, itemName, price, quantity));
            }

            String platform = (String) platformBox.getSelectedItem();
            String paymentMethod = (String) paymentBox.getSelectedItem();
            String deliveryMethod = (String) deliveryBox.getSelectedItem();
            String status = (String) statusBox.getSelectedItem();

            updateOrderWithItems(orderId, customerName, lines, platform, paymentMethod, deliveryMethod, status);
            
            addLog("EDIT_ORDER", "Edited order " + formatOrderCode(orderId) + " for " + customerName);

            JOptionPane.showMessageDialog(dialog, "Order updated.");

            dialog.dispose();

            loadOrders();
            loadCatalog();
loadDashboard();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(dialog, "Price and quantity must be valid numbers.");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(dialog, "Failed to edit order.");
        }
    });

    JPanel footerPanel = new JPanel(new BorderLayout());
    footerPanel.setBackground(Color.WHITE);
    footerPanel.setBorder(BorderFactory.createEmptyBorder(14, 22, 18, 22));

    JPanel leftFooter = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    leftFooter.setBackground(Color.WHITE);

    JPanel rightFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    rightFooter.setBackground(Color.WHITE);
    rightFooter.add(totalLabel);
    rightFooter.add(cancelButton);
    rightFooter.add(saveButton);

    footerPanel.add(leftFooter, BorderLayout.WEST);
    footerPanel.add(rightFooter, BorderLayout.EAST);

    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.setBackground(Color.WHITE);
    centerPanel.add(customerPanel, BorderLayout.NORTH);
    centerPanel.add(itemPanel, BorderLayout.CENTER);
    centerPanel.add(orderDetailsPanel, BorderLayout.SOUTH);

    dialog.add(headerPanel, BorderLayout.NORTH);
    dialog.add(centerPanel, BorderLayout.CENTER);
    dialog.add(footerPanel, BorderLayout.SOUTH);

    styleFlatComponents(dialog);
    dialog.setVisible(true);
    
    
    }//GEN-LAST:event_editOrderButtonActionPerformed

    private void addProductServiceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addProductServiceButtonActionPerformed
 String[] types = {"product", "service"};
    JComboBox<String> typeBox = new JComboBox<>(types);

    JTextField nameField = new JTextField();
    JTextField priceField = new JTextField();
    JTextField stockUnitField = new JTextField();

    String[] statuses = {"active", "inactive"};
    JComboBox<String> statusBox = new JComboBox<>(statuses);

    JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
    panel.add(new JLabel("Type:"));
    panel.add(typeBox);
    panel.add(new JLabel("Name:"));
    panel.add(nameField);
    panel.add(new JLabel("Price:"));
    panel.add(priceField);
    panel.add(new JLabel("Stock / Unit:"));
    panel.add(stockUnitField);
    panel.add(new JLabel("Status:"));
    panel.add(statusBox);

    int result = JOptionPane.showConfirmDialog(
        this,
        panel,
        "Add Product / Service",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE
    );

    if (result != JOptionPane.OK_OPTION) return;

    try {
        String type = (String) typeBox.getSelectedItem();
        String name = nameField.getText().trim();
        double price = Double.parseDouble(priceField.getText().trim());
        String stockUnit = stockUnitField.getText().trim();
        String status = (String) statusBox.getSelectedItem();

        if (name.isEmpty() || stockUnit.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and Stock/Unit are required.");
            return;
        }

        if (price <= 0) {
            JOptionPane.showMessageDialog(this, "Price must be greater than 0.");
            return;
        }

        Connection conn = DBConnection.getConnection();

        if (type.equals("product")) {
            int stock = Integer.parseInt(stockUnit);

           PreparedStatement ps = conn.prepareStatement(
    "INSERT INTO products (name, stock, price, status) VALUES (?, ?, ?, ?)"
);

ps.setString(1, name);
ps.setInt(2, stock);
ps.setDouble(3, price);
ps.setString(4, status);

            ps.executeUpdate();

        } else {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO services (name, base_price, unit, status) VALUES (?, ?, ?, ?)"
            );

            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.setString(3, stockUnit);
            ps.setString(4, status);

            ps.executeUpdate();
        }

        JOptionPane.showMessageDialog(this, "Item added successfully.");
        loadCatalog();
        
        addLog("ADD_CATALOG_ITEM", "Added " + type + ": " + name);
     

    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "Price must be a number. Product stock must be a whole number.");
    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to add item.");
        
        
    }
    }//GEN-LAST:event_addProductServiceButtonActionPerformed

    private void editProductServiceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editProductServiceButtonActionPerformed
    int row = catalogTable.getSelectedRow();

    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Select a product or service first.");
        return;
    }

    int id = Integer.parseInt(catalogTable.getValueAt(row, 0).toString());
    String currentName = catalogTable.getValueAt(row, 1).toString();
    String type = catalogTable.getValueAt(row, 2).toString();
    String currentPrice = catalogTable.getValueAt(row, 3).toString();
    String currentStockUnit = catalogTable.getValueAt(row, 4).toString();
    String currentStatus = catalogTable.getValueAt(row, 5).toString();

    JTextField nameField = new JTextField(currentName);
    JTextField priceField = new JTextField(currentPrice);
    JTextField stockUnitField = new JTextField(currentStockUnit);

    String[] statuses = {"active", "inactive"};
    JComboBox<String> statusBox = new JComboBox<>(statuses);
    statusBox.setSelectedItem(currentStatus);

    JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
    panel.add(new JLabel("Type: " + type));
    panel.add(new JLabel("Name:"));
    panel.add(nameField);
    panel.add(new JLabel("Price:"));
    panel.add(priceField);
    panel.add(new JLabel(type.equals("product") ? "Stock:" : "Unit:"));
    panel.add(stockUnitField);
    panel.add(new JLabel("Status:"));
    panel.add(statusBox);

    int result = JOptionPane.showConfirmDialog(
        this,
        panel,
        "Edit Product / Service",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE
    );

    if (result != JOptionPane.OK_OPTION) return;

    try {
        String name = nameField.getText().trim();
        double price = Double.parseDouble(priceField.getText().trim());
        String stockUnit = stockUnitField.getText().trim();
        String status = (String) statusBox.getSelectedItem();

        if (name.isEmpty() || stockUnit.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and Stock/Unit are required.");
            return;
        }

        if (price <= 0) {
            JOptionPane.showMessageDialog(this, "Price must be greater than 0.");
            return;
        }

        Connection conn = DBConnection.getConnection();

        if (type.equals("product")) {
            int stock = Integer.parseInt(stockUnit);

            PreparedStatement ps = conn.prepareStatement(
                "UPDATE products SET name=?, stock=?, price=?, status=? WHERE id=?"
            );

            ps.setString(1, name);
            ps.setInt(2, stock);
            ps.setDouble(3, price);
            ps.setString(4, status);
            ps.setInt(5, id);

            ps.executeUpdate();

        } else {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE services SET name=?, base_price=?, unit=?, status=? WHERE id=?"
            );

            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.setString(3, stockUnit);
            ps.setString(4, status);
            ps.setInt(5, id);

            ps.executeUpdate();
        }

        JOptionPane.showMessageDialog(this, "Item updated successfully.");
        loadCatalog();
        addLog("EDIT_CATALOG_ITEM", "Edited " + type + ": " + name);

    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "Price must be a number. Product stock must be a whole number.");
    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to update item.");
    }
    }//GEN-LAST:event_editProductServiceButtonActionPerformed

    private void activityLogButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_activityLogButtonActionPerformed
    loadActivityUsers();
    loadActivityLogs();
    cl.show(mainPanel, "card6");
    }//GEN-LAST:event_activityLogButtonActionPerformed

    private boolean showEditAccountDialog() {
    if (!isAdmin()) {
        JOptionPane.showMessageDialog(this, "Only admins can edit accounts.");
        return true;
    }

    int row = allUsersTable.getSelectedRow();

    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Select a user first.");
        return true;
    }

    int id = Integer.parseInt(allUsersTable.getValueAt(row, 0).toString());
    String currentName = String.valueOf(allUsersTable.getValueAt(row, 1));
    String currentUsername = String.valueOf(allUsersTable.getValueAt(row, 2));
    String currentRole = String.valueOf(allUsersTable.getValueAt(row, 3));
    String currentStatus = String.valueOf(allUsersTable.getValueAt(row, 4));

    JTextField nameField = new JTextField(currentName);
    JTextField usernameField = new JTextField(currentUsername);
    JPasswordField passwordField = new JPasswordField();
    JPasswordField confirmPasswordField = new JPasswordField();

    JComboBox<String> roleBox = new JComboBox<>(new String[]{"admin", "employee"});
    JComboBox<String> statusBox = new JComboBox<>(new String[]{"active", "inactive"});

    roleBox.setSelectedItem(currentRole);
    statusBox.setSelectedItem(currentStatus);

    JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
    panel.add(new JLabel("Name:"));
    panel.add(nameField);
    panel.add(new JLabel("Username:"));
    panel.add(usernameField);
    panel.add(new JLabel("New Password (leave blank to keep current):"));
    panel.add(passwordField);
    panel.add(new JLabel("Confirm New Password:"));
    panel.add(confirmPasswordField);
    panel.add(new JLabel("Role:"));
    panel.add(roleBox);
    panel.add(new JLabel("Status:"));
    panel.add(statusBox);

    int result = JOptionPane.showConfirmDialog(
        this,
        panel,
        "Edit Account",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE
    );

    if (result != JOptionPane.OK_OPTION) return true;

    try {
        String name = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();
        String newRole = (String) roleBox.getSelectedItem();
        String newStatus = (String) statusBox.getSelectedItem();

        if (name.isEmpty() || username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and username are required.");
            return true;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.");
            return true;
        }

        /*
         * Prevent the last active admin from being changed to employee
         * or inactive.
         */
        if (isLastActiveAdmin(id)) {
            boolean changingRoleFromAdmin =
                    currentRole.equalsIgnoreCase("admin")
                    && !newRole.equalsIgnoreCase("admin");

            boolean changingStatusFromActive =
                    currentStatus.equalsIgnoreCase("active")
                    && !newStatus.equalsIgnoreCase("active");

            if (changingRoleFromAdmin || changingStatusFromActive) {
                JOptionPane.showMessageDialog(
                    this,
                    "The last active admin cannot be changed to employee or inactive."
                );
                return true;
            }
        }

        /*
         * Optional but recommended:
         * prevent the currently logged-in user from deactivating their own account.
         */
        if (currentUsername.equalsIgnoreCase(loggedUsername)
                && newStatus.equalsIgnoreCase("inactive")) {

            JOptionPane.showMessageDialog(
                this,
                "You cannot deactivate your own account while logged in."
            );
            return true;
        }

        Connection conn = DBConnection.getConnection();

        PreparedStatement checkPs = conn.prepareStatement(
            "SELECT COUNT(*) FROM users WHERE username=? AND id<>?"
        );

        checkPs.setString(1, username);
        checkPs.setInt(2, id);

        ResultSet checkRs = checkPs.executeQuery();

        if (checkRs.next() && checkRs.getInt(1) > 0) {
            JOptionPane.showMessageDialog(this, "Username is already taken.");
            return true;
        }

        PreparedStatement ps;

        if (password.isEmpty()) {
            ps = conn.prepareStatement(
                "UPDATE users SET name=?, username=?, role=?, status=? WHERE id=?"
            );

            ps.setString(1, name);
            ps.setString(2, username);
            ps.setString(3, newRole);
            ps.setString(4, newStatus);
            ps.setInt(5, id);
        } else {
            ps = conn.prepareStatement(
                "UPDATE users SET name=?, username=?, password=?, role=?, status=? WHERE id=?"
            );

            ps.setString(1, name);
            ps.setString(2, username);
            ps.setString(3, password);
            ps.setString(4, newRole);
            ps.setString(5, newStatus);
            ps.setInt(6, id);
        }

        ps.executeUpdate();

        if (currentUsername.equalsIgnoreCase(loggedUsername)) {
            loggedUsername = username;
            loggedRole = newRole;
        }

        addLog("EDIT_ACCOUNT", "Edited account: " + username);

        JOptionPane.showMessageDialog(this, "Account updated successfully.");

        loadAllUsers();
        loadCurrentUser();
        loadActivityUsers();

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to edit account.");
    }

    return true;
}


    private void changeUsernameAllAccountsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changeUsernameAllAccountsButtonActionPerformed
showEditAccountDialog();
    }//GEN-LAST:event_changeUsernameAllAccountsButtonActionPerformed

    private void ordersButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ordersButtonActionPerformed
            loadOrders();
    cl.show(mainPanel, "orders");
    }//GEN-LAST:event_ordersButtonActionPerformed

    private void dashboardCreateOrderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dashboardCreateOrderButtonActionPerformed
    cl.show(mainPanel, "orders");
    createOrderButton.doClick();
    }//GEN-LAST:event_dashboardCreateOrderButtonActionPerformed

    private void accountNameButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_accountNameButtonActionPerformed
    cl.show(mainPanel, "accounts");
    }//GEN-LAST:event_accountNameButtonActionPerformed

    private void generateLogButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateLogButtonActionPerformed
        loadActivityLogs();
    }//GEN-LAST:event_generateLogButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
    new LoginForm().setVisible(true);
});
        
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton accountNameButton;
    private javax.swing.JButton accountsButton;
    private javax.swing.JPanel accountsPanel;
    private com.toedter.calendar.JDateChooser activityEndDateChooser;
    private javax.swing.JButton activityLogButton;
    private javax.swing.JPanel activityLogPanel;
    private javax.swing.JTable activityLogTable;
    private com.toedter.calendar.JDateChooser activityStartDateChooser;
    private javax.swing.JLabel actualIdLabel;
    private javax.swing.JLabel actualNameLabel;
    private javax.swing.JLabel actualRoleLabel;
    private javax.swing.JLabel actualUsernameLabel;
    private javax.swing.JPanel actualsalesAnalysisPanel;
    private javax.swing.JButton addAccountButton;
    private javax.swing.JButton addProductServiceButton;
    private javax.swing.JTable allUsersTable;
    private javax.swing.JButton cancelOrderButton;
    private javax.swing.JButton catalogButton;
    private javax.swing.JPanel catalogPanel;
    private javax.swing.JTextArea catalogStockTextArea;
    private javax.swing.JTable catalogTable;
    private javax.swing.JButton changeUsernameAllAccountsButton;
    private javax.swing.JLabel completedLabel;
    private javax.swing.JLabel completedLabel1;
    private javax.swing.JLabel completedLabel2;
    private javax.swing.JLabel completedLabel3;
    private javax.swing.JLabel completedOrdersNumberLabel;
    private javax.swing.JPanel completedPanel;
    private javax.swing.JButton createOrderButton;
    private javax.swing.JButton dashboardButton;
    private javax.swing.JButton dashboardCreateOrderButton;
    private javax.swing.JPanel dashboardPanel;
    private javax.swing.JButton editOrderButton;
    private javax.swing.JButton editProductServiceButton;
    private javax.swing.JButton generateLogButton;
    private javax.swing.JPanel graphContainerPanel;
    private javax.swing.JPanel graphContainerPanel1;
    private javax.swing.JLabel idLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTable jTable1;
    private javax.swing.JButton logOutButton;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JPanel optionPanel;
    private javax.swing.JButton ordersButton;
    private javax.swing.JPanel ordersPanel;
    private javax.swing.JLabel pendingLabel;
    private javax.swing.JLabel pendingOrdersNumberLabel;
    private javax.swing.JPanel pendingPanel;
    private javax.swing.JLabel processingLabel;
    private javax.swing.JLabel processingOrdersNumberLabel;
    private javax.swing.JPanel processingPanel;
    private javax.swing.JButton resetLogButton;
    private javax.swing.JLabel roleLabel;
    private javax.swing.JButton salesReportButton;
    private javax.swing.JPanel salesReportPanel;
    private javax.swing.JLabel totalOrdersLabel4;
    private javax.swing.JLabel totalOrdersLabel5;
    private javax.swing.JLabel totalOrdersNumberLabel4;
    private javax.swing.JPanel totalOrdersPanel4;
    private javax.swing.JButton updateStatusButton;
    private javax.swing.JComboBox<String> userComboBox;
    private javax.swing.JLabel usernameLabel;
    private javax.swing.JButton viewDetailsButton;
    // End of variables declaration//GEN-END:variables
}
