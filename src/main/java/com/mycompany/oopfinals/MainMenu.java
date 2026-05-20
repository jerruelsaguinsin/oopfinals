

package com.mycompany.oopfinals;
import java.awt.CardLayout;
import java.awt.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.*;
import java.security.MessageDigest;

public class MainMenu extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MainMenu.class.getName());
 CardLayout cl;

 private String loggedUsername;
private String loggedRole;

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

private String hashPassword(String password) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] bytes = md.digest(password.getBytes("UTF-8"));

    StringBuilder sb = new StringBuilder();

    for (byte b : bytes) {
        sb.append(String.format("%02x", b));
    }

    return sb.toString();
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
    labelComponent.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    labelComponent.setForeground(new Color(120, 120, 120));

    JLabel valueComponent = new JLabel(value == null ? "-" : value.toString());
    valueComponent.setFont(new Font("Segoe UI", Font.BOLD, 14));
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

private void loadActivityLogs() {
    try {
        Connection conn = DBConnection.getConnection();

        PreparedStatement ps = conn.prepareStatement(
            "SELECT id, username, role, action, details, created_at FROM activity_logs ORDER BY id DESC"
        );

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
    titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));

    JLabel actionLabel = new JLabel(String.valueOf(activityLogTable.getValueAt(row, 3)));
    actionLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
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
            "SELECT id, name, role, status FROM users"
        );

        ResultSet rs = ps.executeQuery();

        DefaultTableModel model = (DefaultTableModel) allUsersTable.getModel();
        model.setRowCount(0);

        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getInt("id"),
                rs.getString("name"),
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
    rs.getInt("id"),
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
            "SELECT id, name, username, password, role FROM users WHERE username=?"
        );

        ps.setString(1, loggedUsername);

        ResultSet rs = ps.executeQuery();

        DefaultTableModel model = (DefaultTableModel) currentUserTable.getModel();
        model.setRowCount(0);

        if (rs.next()) {
            model.addRow(new Object[]{
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("role")
            });
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}
   public MainMenu(String username, String role) {
    initComponents();

    applyOrderStatusColors();
    this.loggedUsername = username;
    this.loggedRole = role;
    addLog("LOGIN", "User logged in");

    cl = (CardLayout) mainPanel.getLayout();

loadCurrentUser();
loadAllUsers();
loadOrders();
loadCatalog();
loadActivityLogs();
addTableDoubleClickActions();

 if (!isAdmin()) {
    accountsButton.setVisible(false);
    activityLogButton.setVisible(false);

    changeRoleButton.setEnabled(false);
    changeStatusButton.setEnabled(false);
    changeUsernameAllAccountsButton.setEnabled(false);
    changePasswordAllAccountsButton.setEnabled(false);
}   


    cl.show(mainPanel, "dashboard");

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
        servicesButton = new javax.swing.JButton();
        activityLogButton = new javax.swing.JButton();
        mainPanel = new javax.swing.JPanel();
        salesReportPanel = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        accountsPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        currentUserTable = new javax.swing.JTable();
        jLabel4 = new javax.swing.JLabel();
        changePasswordButton = new javax.swing.JButton();
        changeUsernameButton = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        allUsersTable = new javax.swing.JTable();
        changeRoleButton = new javax.swing.JButton();
        changeStatusButton = new javax.swing.JButton();
        addAccountButton = new javax.swing.JButton();
        changePasswordAllAccountsButton = new javax.swing.JButton();
        changeUsernameAllAccountsButton = new javax.swing.JButton();
        dashboardPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        createOrderButton = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        updateStatusButton = new javax.swing.JButton();
        cancelOrderButton = new javax.swing.JButton();
        viewDetailsButton = new javax.swing.JButton();
        editOrderButton = new javax.swing.JButton();
        servicesPanel = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        catalogTable = new javax.swing.JTable();
        addProductServiceButton = new javax.swing.JButton();
        editProductServiceButton = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        activityLogPanel = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        activityLogTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        optionPanel.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setIcon(new javax.swing.ImageIcon("C:\\Users\\admin\\Downloads\\Untitled design - 2026-04-29T125423.618.png")); // NOI18N

        salesReportButton.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        salesReportButton.setText("SALES REPORT");
        salesReportButton.addActionListener(this::salesReportButtonActionPerformed);

        dashboardButton.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        dashboardButton.setText("DASHBOARD");
        dashboardButton.addActionListener(this::dashboardButtonActionPerformed);

        accountsButton.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        accountsButton.setText("ACCOUNTS");
        accountsButton.addActionListener(this::accountsButtonActionPerformed);

        logOutButton.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        logOutButton.setText("LOG OUT");
        logOutButton.addActionListener(this::logOutButtonActionPerformed);

        servicesButton.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        servicesButton.setText("SERVICES");
        servicesButton.addActionListener(this::servicesButtonActionPerformed);

        activityLogButton.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        activityLogButton.setText("ACTIVITY LOG");
        activityLogButton.addActionListener(this::activityLogButtonActionPerformed);

        javax.swing.GroupLayout optionPanelLayout = new javax.swing.GroupLayout(optionPanel);
        optionPanel.setLayout(optionPanelLayout);
        optionPanelLayout.setHorizontalGroup(
            optionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optionPanelLayout.createSequentialGroup()
                .addGroup(optionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(optionPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(dashboardButton, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, optionPanelLayout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addGroup(optionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(logOutButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, optionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(salesReportButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(servicesButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(accountsButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(activityLogButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                .addContainerGap(25, Short.MAX_VALUE))
        );
        optionPanelLayout.setVerticalGroup(
            optionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optionPanelLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(34, 34, 34)
                .addComponent(dashboardButton, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(servicesButton)
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

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel3.setText("SALES REPORT");

        javax.swing.GroupLayout salesReportPanelLayout = new javax.swing.GroupLayout(salesReportPanel);
        salesReportPanel.setLayout(salesReportPanelLayout);
        salesReportPanelLayout.setHorizontalGroup(
            salesReportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(salesReportPanelLayout.createSequentialGroup()
                .addGap(200, 200, 200)
                .addComponent(jLabel3)
                .addContainerGap(525, Short.MAX_VALUE))
        );
        salesReportPanelLayout.setVerticalGroup(
            salesReportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(salesReportPanelLayout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addComponent(jLabel3)
                .addContainerGap(622, Short.MAX_VALUE))
        );

        mainPanel.add(salesReportPanel, "salesReport");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel2.setText("ACCOUNTS");

        currentUserTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null}
            },
            new String [] {
                "ID", "Name", "Username", "Password", "Role"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
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
        jScrollPane2.setViewportView(currentUserTable);

        jLabel4.setText("CURRENT ACCOUNT");

        changePasswordButton.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        changePasswordButton.setText("CHANGE PASSWORD");
        changePasswordButton.addActionListener(this::changePasswordButtonActionPerformed);

        changeUsernameButton.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        changeUsernameButton.setText("CHANGE USERNAME");
        changeUsernameButton.addActionListener(this::changeUsernameButtonActionPerformed);

        jLabel5.setText("ALL ACCOUNTS");

        allUsersTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "ID", "Name", "Role", "Status"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane3.setViewportView(allUsersTable);

        changeRoleButton.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        changeRoleButton.setText("CHANGE ROLE");
        changeRoleButton.addActionListener(this::changeRoleButtonActionPerformed);

        changeStatusButton.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        changeStatusButton.setText("CHANGE STATUS");
        changeStatusButton.addActionListener(this::changeStatusButtonActionPerformed);

        addAccountButton.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        addAccountButton.setText("ADD ACCOUNT");
        addAccountButton.addActionListener(this::addAccountButtonActionPerformed);

        changePasswordAllAccountsButton.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        changePasswordAllAccountsButton.setText("CHANGE PASSWORD");
        changePasswordAllAccountsButton.addActionListener(this::changePasswordAllAccountsButtonActionPerformed);

        changeUsernameAllAccountsButton.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        changeUsernameAllAccountsButton.setText("CHANGE USERNAME");
        changeUsernameAllAccountsButton.addActionListener(this::changeUsernameAllAccountsButtonActionPerformed);

        javax.swing.GroupLayout accountsPanelLayout = new javax.swing.GroupLayout(accountsPanel);
        accountsPanel.setLayout(accountsPanelLayout);
        accountsPanelLayout.setHorizontalGroup(
            accountsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(accountsPanelLayout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addGroup(accountsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel4)
                    .addGroup(accountsPanelLayout.createSequentialGroup()
                        .addComponent(changeUsernameButton, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(changePasswordButton, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel5)
                    .addComponent(jLabel2)
                    .addGroup(accountsPanelLayout.createSequentialGroup()
                        .addComponent(addAccountButton, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(changeRoleButton, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(changeStatusButton, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(changeUsernameAllAccountsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(changePasswordAllAccountsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 819, Short.MAX_VALUE)
                    .addComponent(jScrollPane3))
                .addContainerGap(37, Short.MAX_VALUE))
        );
        accountsPanelLayout.setVerticalGroup(
            accountsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(accountsPanelLayout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(accountsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(changeUsernameButton)
                    .addComponent(changePasswordButton))
                .addGap(34, 34, 34)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(accountsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addAccountButton)
                    .addComponent(changeRoleButton)
                    .addComponent(changeStatusButton)
                    .addComponent(changePasswordAllAccountsButton)
                    .addComponent(changeUsernameAllAccountsButton))
                .addGap(34, 34, 34))
        );

        mainPanel.add(accountsPanel, "accounts");

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

        createOrderButton.setText("CREATE ORDER");
        createOrderButton.addActionListener(this::createOrderButtonActionPerformed);

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel6.setText("DASHBOARD");

        updateStatusButton.setText("UPDATE STATUS");
        updateStatusButton.addActionListener(this::updateStatusButtonActionPerformed);

        cancelOrderButton.setText("CANCEL ORDER");
        cancelOrderButton.addActionListener(this::cancelOrderButtonActionPerformed);

        viewDetailsButton.setText("VIEW DETAILS");
        viewDetailsButton.addActionListener(this::viewDetailsButtonActionPerformed);

        editOrderButton.setText("EDIT ORDER");
        editOrderButton.addActionListener(this::editOrderButtonActionPerformed);

        javax.swing.GroupLayout dashboardPanelLayout = new javax.swing.GroupLayout(dashboardPanel);
        dashboardPanel.setLayout(dashboardPanelLayout);
        dashboardPanelLayout.setHorizontalGroup(
            dashboardPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dashboardPanelLayout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(dashboardPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 853, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(dashboardPanelLayout.createSequentialGroup()
                        .addGroup(dashboardPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(updateStatusButton, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
                            .addComponent(createOrderButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(dashboardPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(editOrderButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(viewDetailsButton, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cancelOrderButton, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        dashboardPanelLayout.setVerticalGroup(
            dashboardPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, dashboardPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(dashboardPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(createOrderButton)
                    .addComponent(cancelOrderButton)
                    .addComponent(editOrderButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(dashboardPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(updateStatusButton)
                    .addComponent(viewDetailsButton))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 524, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(17, Short.MAX_VALUE))
        );

        mainPanel.add(dashboardPanel, "dashboard");

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

        addProductServiceButton.setText("ADD PRODUCT/SERVICE");
        addProductServiceButton.addActionListener(this::addProductServiceButtonActionPerformed);

        editProductServiceButton.setText("EDIT PRODUCT/SERVICE");
        editProductServiceButton.addActionListener(this::editProductServiceButtonActionPerformed);

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel7.setText("SERVICES");

        javax.swing.GroupLayout servicesPanelLayout = new javax.swing.GroupLayout(servicesPanel);
        servicesPanel.setLayout(servicesPanelLayout);
        servicesPanelLayout.setHorizontalGroup(
            servicesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(servicesPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(servicesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 845, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(servicesPanelLayout.createSequentialGroup()
                        .addComponent(addProductServiceButton, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(editProductServiceButton, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(26, Short.MAX_VALUE))
        );
        servicesPanelLayout.setVerticalGroup(
            servicesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(servicesPanelLayout.createSequentialGroup()
                .addContainerGap(23, Short.MAX_VALUE)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(servicesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addProductServiceButton)
                    .addComponent(editProductServiceButton))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 548, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26))
        );

        mainPanel.add(servicesPanel, "card5");

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel8.setText("ACTIVITY LOG");

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

        javax.swing.GroupLayout activityLogPanelLayout = new javax.swing.GroupLayout(activityLogPanel);
        activityLogPanel.setLayout(activityLogPanelLayout);
        activityLogPanelLayout.setHorizontalGroup(
            activityLogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(activityLogPanelLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(jLabel8)
                .addContainerGap(708, Short.MAX_VALUE))
            .addGroup(activityLogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(activityLogPanelLayout.createSequentialGroup()
                    .addGap(23, 23, 23)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 845, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(24, Short.MAX_VALUE)))
        );
        activityLogPanelLayout.setVerticalGroup(
            activityLogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(activityLogPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(jLabel8)
                .addContainerGap(629, Short.MAX_VALUE))
            .addGroup(activityLogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(activityLogPanelLayout.createSequentialGroup()
                    .addGap(67, 67, 67)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 589, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(26, Short.MAX_VALUE)))
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
 loadOrders();
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
titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));

JLabel subtitleLabel = new JLabel("Add customer details, delivery, payment, and order items.");
subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
subtitleLabel.setForeground(new Color(100, 100, 100));

JPanel titlePanel = new JPanel(new GridLayout(0, 1));
titlePanel.setBackground(Color.WHITE);
titlePanel.add(titleLabel);
titlePanel.add(subtitleLabel);

headerPanel.add(titlePanel, BorderLayout.WEST);

JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 8));
formPanel.setBackground(Color.WHITE);
formPanel.setBorder(BorderFactory.createEmptyBorder(0, 22, 14, 22));

JTextField customerNameField = new JTextField();

String[] platforms = {"Facebook", "Shopee", "Lazada", "TikTok"};
JComboBox<String> platformBox = new JComboBox<>(platforms);

String[] payments = {"Cash", "GCash", "Bank Transfer", "COD"};
JComboBox<String> paymentBox = new JComboBox<>(payments);

String[] deliveries = {"Pickup", "Meetup", "J&T", "Flash", "LBC", "Shopee", "Lazada", "TikTok"};
JComboBox<String> deliveryBox = new JComboBox<>(deliveries);

String[] statuses = {"Pending", "Processing", "Completed", "Cancelled"};
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

formPanel.add(new JLabel("Customer Name"));
formPanel.add(customerNameField);
formPanel.add(new JLabel("Platform"));
formPanel.add(platformBox);
formPanel.add(new JLabel("Payment Method"));
formPanel.add(paymentBox);
formPanel.add(new JLabel("Delivery Method"));
formPanel.add(deliveryBox);
formPanel.add(new JLabel("Status"));
formPanel.add(statusBox);

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
JLabel totalLabel = new JLabel("Total: PHP 0.00");
totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

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
leftFooter.add(addItemButton);

JPanel rightFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
rightFooter.setBackground(Color.WHITE);
rightFooter.add(totalLabel);
rightFooter.add(cancelButton);
rightFooter.add(saveButton);

footerPanel.add(leftFooter, BorderLayout.WEST);
footerPanel.add(rightFooter, BorderLayout.EAST);

JPanel centerPanel = new JPanel(new BorderLayout());
centerPanel.setBackground(Color.WHITE);
centerPanel.add(formPanel, BorderLayout.NORTH);
centerPanel.add(itemScroll, BorderLayout.CENTER);

dialog.add(headerPanel, BorderLayout.NORTH);
dialog.add(centerPanel, BorderLayout.CENTER);
dialog.add(footerPanel, BorderLayout.SOUTH);

dialog.setVisible(true);
    }//GEN-LAST:event_createOrderButtonActionPerformed

    private void changePasswordButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changePasswordButtonActionPerformed
    try {
        String newPass = JOptionPane.showInputDialog("Enter new password:");
        if (newPass == null || newPass.trim().isEmpty()) return;

        Connection conn = DBConnection.getConnection();

        PreparedStatement ps = conn.prepareStatement(
            "UPDATE users SET password=? WHERE username=?"
        );

        ps.setString(1, hashPassword(newPass));
        ps.setString(2, loggedUsername);

        ps.executeUpdate();
        
        addLog("CHANGE_PASSWORD", "Changed own password");

        loadCurrentUser();

    } catch (Exception e) {
        e.printStackTrace();
    }
    }//GEN-LAST:event_changePasswordButtonActionPerformed

    private void changeUsernameButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changeUsernameButtonActionPerformed
        try {
        String newUsername = JOptionPane.showInputDialog("Enter new username:");
        if (newUsername == null || newUsername.trim().isEmpty()) return;

        Connection conn = DBConnection.getConnection();

        PreparedStatement ps = conn.prepareStatement(
            "UPDATE users SET username=? WHERE username=?"
        );

        ps.setString(1, newUsername);
        ps.setString(2, loggedUsername);
String oldUsername = loggedUsername;
        ps.executeUpdate();
addLog("CHANGE_USERNAME", "Changed username from " + oldUsername + " to " + newUsername);
        loggedUsername = newUsername;
        loadCurrentUser();

    } catch (Exception e) {
        e.printStackTrace();
    }
    }//GEN-LAST:event_changeUsernameButtonActionPerformed

    private void changeRoleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changeRoleButtonActionPerformed
 if (!isAdmin()) return;

    int row = allUsersTable.getSelectedRow();
    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Select a user first.");
        return;
    }

    int id = (int) allUsersTable.getValueAt(row, 0);
    String currentRole = allUsersTable.getValueAt(row, 2).toString();

    String[] roles = {"admin", "employee"};
    JComboBox<String> roleBox = new JComboBox<>(roles);

    int choice = JOptionPane.showConfirmDialog(
            this,
            roleBox,
            "Select New Role",
            JOptionPane.OK_CANCEL_OPTION
    );

    if (choice != JOptionPane.OK_OPTION) return;

    String newRole = (String) roleBox.getSelectedItem();

    // ❗ prevent last admin from being demoted
    if (currentRole.equalsIgnoreCase("admin")
            && newRole.equalsIgnoreCase("employee")
            && !hasMoreThanOneActiveAdmin()) {

        JOptionPane.showMessageDialog(this, "Cannot demote the last active admin.");
        return;
    }

    try {
        Connection conn = DBConnection.getConnection();

        PreparedStatement ps = conn.prepareStatement(
            "UPDATE users SET role=? WHERE id=?"
        );

        ps.setString(1, newRole);
        ps.setInt(2, id);

        ps.executeUpdate();
        addLog("CHANGE_USER_ROLE", "Changed user ID " + id + " role from " + currentRole + " to " + newRole);

        loadAllUsers();

    } catch (Exception e) {
        e.printStackTrace();
    }
    }//GEN-LAST:event_changeRoleButtonActionPerformed

    private void changeStatusButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changeStatusButtonActionPerformed
if (!isAdmin()) return;

    int row = allUsersTable.getSelectedRow();
    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Select a user first.");
        return;
    }

    int id = (int) allUsersTable.getValueAt(row, 0);
    String role = allUsersTable.getValueAt(row, 2).toString();
    String currentStatus = allUsersTable.getValueAt(row, 3).toString();

    String[] statuses = {"active", "inactive"};
    JComboBox<String> statusBox = new JComboBox<>(statuses);

    int choice = JOptionPane.showConfirmDialog(
            this,
            statusBox,
            "Select New Status",
            JOptionPane.OK_CANCEL_OPTION
    );

    if (choice != JOptionPane.OK_OPTION) return;

    String newStatus = (String) statusBox.getSelectedItem();

    // ❗ prevent last active admin from being deactivated
    if (role.equalsIgnoreCase("admin")
            && currentStatus.equalsIgnoreCase("active")
            && newStatus.equalsIgnoreCase("inactive")
            && !hasMoreThanOneActiveAdmin()) {

        JOptionPane.showMessageDialog(this, "Cannot deactivate the last active admin.");
        return;
    }

    try {
        Connection conn = DBConnection.getConnection();

        PreparedStatement ps = conn.prepareStatement(
            "UPDATE users SET status=? WHERE id=?"
        );

        ps.setString(1, newStatus);
        ps.setInt(2, id);

        ps.executeUpdate();
        addLog("CHANGE_USER_STATUS", "Changed user ID " + id + " status from " + currentStatus + " to " + newStatus);

        loadAllUsers();

    } catch (Exception e) {
        e.printStackTrace();
    }
    }//GEN-LAST:event_changeStatusButtonActionPerformed

    private void addAccountButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAccountButtonActionPerformed
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

    private void servicesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_servicesButtonActionPerformed
        loadCatalog();
    cl.show(mainPanel, "card5");
    }//GEN-LAST:event_servicesButtonActionPerformed

    private void updateStatusButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateStatusButtonActionPerformed
int row = jTable1.getSelectedRow();

    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Select an order first.");
        return;
    }

    int orderId = Integer.parseInt(jTable1.getValueAt(row, 0).toString());

    String[] statuses = {"Pending", "Processing", "Completed", "Cancelled"};
    JComboBox<String> statusBox = new JComboBox<>(statuses);

    Object currentStatus = jTable1.getValueAt(row, 7);
    if (currentStatus != null) {
        statusBox.setSelectedItem(currentStatus.toString());
    }

    int choice = JOptionPane.showConfirmDialog(
        this,
        statusBox,
        "Update Order Status",
        JOptionPane.OK_CANCEL_OPTION
    );

    if (choice != JOptionPane.OK_OPTION) return;

    String newStatus = (String) statusBox.getSelectedItem();

    try {
        Connection conn = DBConnection.getConnection();

        PreparedStatement ps = conn.prepareStatement(
            "UPDATE orders SET status=?, date_completed = CASE WHEN ? = 'Completed' THEN CURRENT_TIMESTAMP ELSE NULL END WHERE id=?"
        );

        ps.setString(1, newStatus);
        ps.setString(2, newStatus);
        ps.setInt(3, orderId);

        ps.executeUpdate();
        
        addLog("UPDATE_ORDER_STATUS", "Updated order #" + orderId + " to " + newStatus);

        JOptionPane.showMessageDialog(this, "Order status updated.");
        loadOrders();

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to update status.");
    }
    }//GEN-LAST:event_updateStatusButtonActionPerformed

    private void cancelOrderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelOrderButtonActionPerformed
    int row = jTable1.getSelectedRow();

    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Select an order first.");
        return;
    }

    int orderId = Integer.parseInt(jTable1.getValueAt(row, 0).toString());

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
        
        addLog("CANCEL_ORDER", "Cancelled order #" + orderId + " and returned stock");

        JOptionPane.showMessageDialog(this, "Order cancelled and stock returned.");
        loadOrders();
        loadCatalog();

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

    int orderId = Integer.parseInt(jTable1.getValueAt(row, 0).toString());
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

    JLabel titleLabel = new JLabel("Order #" + orderId);
    titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));

    JLabel statusLabel = new JLabel(status);
    statusLabel.setOpaque(true);
    statusLabel.setBackground(statusBg);
    statusLabel.setForeground(statusFg);
    statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
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

    footerPanel.add(closeButton);

    dialog.add(headerPanel, BorderLayout.NORTH);
    dialog.add(centerPanel, BorderLayout.CENTER);
    dialog.add(footerPanel, BorderLayout.SOUTH);

    dialog.setVisible(true);
    }//GEN-LAST:event_viewDetailsButtonActionPerformed

    private void editOrderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editOrderButtonActionPerformed
int row = jTable1.getSelectedRow();

    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Select an order first.");
        return;
    }

    int orderId = Integer.parseInt(jTable1.getValueAt(row, 0).toString());
    java.util.List<OrderItem> items = loadOrderItems();

    JDialog dialog = new JDialog(this, "Edit Order", true);
    dialog.setSize(800, 560);
    dialog.setLocationRelativeTo(this);
    dialog.setLayout(new BorderLayout());
    dialog.getContentPane().setBackground(Color.WHITE);

    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setBackground(Color.WHITE);
    headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 22, 14, 22));

    JLabel titleLabel = new JLabel("Edit Order #" + orderId);
    titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));

    JLabel subtitleLabel = new JLabel("Update customer details, delivery, payment, and order items.");
    subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    subtitleLabel.setForeground(new Color(100, 100, 100));

    JPanel titlePanel = new JPanel(new GridLayout(0, 1));
    titlePanel.setBackground(Color.WHITE);
    titlePanel.add(titleLabel);
    titlePanel.add(subtitleLabel);

    headerPanel.add(titlePanel, BorderLayout.WEST);

    JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 8));
    formPanel.setBackground(Color.WHITE);
    formPanel.setBorder(BorderFactory.createEmptyBorder(0, 22, 14, 22));

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

    formPanel.add(new JLabel("Customer Name"));
    formPanel.add(customerNameField);
    formPanel.add(new JLabel("Platform"));
    formPanel.add(platformBox);
    formPanel.add(new JLabel("Payment Method"));
    formPanel.add(paymentBox);
    formPanel.add(new JLabel("Delivery Method"));
    formPanel.add(deliveryBox);
    formPanel.add(new JLabel("Status"));
    formPanel.add(statusBox);

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
    JLabel totalLabel = new JLabel("Total: PHP 0.00");
    totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

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
            
            addLog("EDIT_ORDER", "Edited order #" + orderId + " for " + customerName);

            JOptionPane.showMessageDialog(dialog, "Order updated.");

            dialog.dispose();

            loadOrders();
            loadCatalog();

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
    leftFooter.add(addItemButton);

    JPanel rightFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    rightFooter.setBackground(Color.WHITE);
    rightFooter.add(totalLabel);
    rightFooter.add(cancelButton);
    rightFooter.add(saveButton);

    footerPanel.add(leftFooter, BorderLayout.WEST);
    footerPanel.add(rightFooter, BorderLayout.EAST);

    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.setBackground(Color.WHITE);
    centerPanel.add(formPanel, BorderLayout.NORTH);
    centerPanel.add(itemScroll, BorderLayout.CENTER);

    dialog.add(headerPanel, BorderLayout.NORTH);
    dialog.add(centerPanel, BorderLayout.CENTER);
    dialog.add(footerPanel, BorderLayout.SOUTH);

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
            loadActivityLogs();
    cl.show(mainPanel, "card6");
    }//GEN-LAST:event_activityLogButtonActionPerformed

    private void changePasswordAllAccountsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changePasswordAllAccountsButtonActionPerformed
  if (!isAdmin()) {
        JOptionPane.showMessageDialog(this, "Only admins can reset passwords.");
        return;
    }

    int row = allUsersTable.getSelectedRow();

    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Select a user first.");
        return;
    }

    int id = Integer.parseInt(allUsersTable.getValueAt(row, 0).toString());
    String name = allUsersTable.getValueAt(row, 1).toString();
    String role = allUsersTable.getValueAt(row, 2).toString();

    JPasswordField passwordField = new JPasswordField();
    JPasswordField confirmPasswordField = new JPasswordField();

    JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
    panel.add(new JLabel("New password for " + name + ":"));
    panel.add(passwordField);
    panel.add(new JLabel("Confirm new password:"));
    panel.add(confirmPasswordField);

    int result = JOptionPane.showConfirmDialog(
        this,
        panel,
        "Reset User Password",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE
    );

    if (result != JOptionPane.OK_OPTION) return;

    String newPassword = new String(passwordField.getPassword()).trim();
    String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

    if (newPassword.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Password cannot be empty.");
        return;
    }

    if (!newPassword.equals(confirmPassword)) {
        JOptionPane.showMessageDialog(this, "Passwords do not match.");
        return;
    }

    int confirm = JOptionPane.showConfirmDialog(
        this,
        "Reset password for " + name + "?",
        "Confirm Password Reset",
        JOptionPane.YES_NO_OPTION
    );

    if (confirm != JOptionPane.YES_OPTION) return;

    try {
        Connection conn = DBConnection.getConnection();

        PreparedStatement ps = conn.prepareStatement(
            "UPDATE users SET password=? WHERE id=?"
        );

        ps.setString(1, hashPassword(newPassword));
        ps.setInt(2, id);

        ps.executeUpdate();

        addLog("RESET_PASSWORD", "Reset password for user ID " + id + " (" + name + ", " + role + ")");

        JOptionPane.showMessageDialog(this, "Password reset successfully.");

        loadAllUsers();

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to reset password.");
    }
    }//GEN-LAST:event_changePasswordAllAccountsButtonActionPerformed

    private void changeUsernameAllAccountsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changeUsernameAllAccountsButtonActionPerformed
        if (!isAdmin()) {
        JOptionPane.showMessageDialog(this, "Only admins can change usernames.");
        return;
    }

    int row = allUsersTable.getSelectedRow();

    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Select a user first.");
        return;
    }

    int id = Integer.parseInt(allUsersTable.getValueAt(row, 0).toString());
    String name = allUsersTable.getValueAt(row, 1).toString();

    String newUsername = JOptionPane.showInputDialog(
        this,
        "Enter new username for " + name + ":"
    );

    if (newUsername == null || newUsername.trim().isEmpty()) {
        return;
    }

    newUsername = newUsername.trim();

    try {
        Connection conn = DBConnection.getConnection();

        PreparedStatement checkPs = conn.prepareStatement(
            "SELECT COUNT(*) FROM users WHERE username=? AND id<>?"
        );

        checkPs.setString(1, newUsername);
        checkPs.setInt(2, id);

        ResultSet checkRs = checkPs.executeQuery();

        if (checkRs.next() && checkRs.getInt(1) > 0) {
            JOptionPane.showMessageDialog(this, "Username is already taken.");
            return;
        }

        PreparedStatement oldPs = conn.prepareStatement(
            "SELECT username FROM users WHERE id=?"
        );

        oldPs.setInt(1, id);

        ResultSet oldRs = oldPs.executeQuery();
        String oldUsername = "";

        if (oldRs.next()) {
            oldUsername = oldRs.getString("username");
        }

        PreparedStatement ps = conn.prepareStatement(
            "UPDATE users SET username=? WHERE id=?"
        );

        ps.setString(1, newUsername);
        ps.setInt(2, id);

        ps.executeUpdate();

        addLog(
            "CHANGE_USER_USERNAME",
            "Changed user ID " + id + " username from " + oldUsername + " to " + newUsername
        );

        JOptionPane.showMessageDialog(this, "Username changed successfully.");

        loadAllUsers();
        loadCurrentUser();

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to change username.");
    }
    }//GEN-LAST:event_changeUsernameAllAccountsButtonActionPerformed

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
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
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
    private javax.swing.JButton accountsButton;
    private javax.swing.JPanel accountsPanel;
    private javax.swing.JButton activityLogButton;
    private javax.swing.JPanel activityLogPanel;
    private javax.swing.JTable activityLogTable;
    private javax.swing.JButton addAccountButton;
    private javax.swing.JButton addProductServiceButton;
    private javax.swing.JTable allUsersTable;
    private javax.swing.JButton cancelOrderButton;
    private javax.swing.JTable catalogTable;
    private javax.swing.JButton changePasswordAllAccountsButton;
    private javax.swing.JButton changePasswordButton;
    private javax.swing.JButton changeRoleButton;
    private javax.swing.JButton changeStatusButton;
    private javax.swing.JButton changeUsernameAllAccountsButton;
    private javax.swing.JButton changeUsernameButton;
    private javax.swing.JButton createOrderButton;
    private javax.swing.JTable currentUserTable;
    private javax.swing.JButton dashboardButton;
    private javax.swing.JPanel dashboardPanel;
    private javax.swing.JButton editOrderButton;
    private javax.swing.JButton editProductServiceButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTable jTable1;
    private javax.swing.JButton logOutButton;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel optionPanel;
    private javax.swing.JButton salesReportButton;
    private javax.swing.JPanel salesReportPanel;
    private javax.swing.JButton servicesButton;
    private javax.swing.JPanel servicesPanel;
    private javax.swing.JButton updateStatusButton;
    private javax.swing.JButton viewDetailsButton;
    // End of variables declaration//GEN-END:variables
}
