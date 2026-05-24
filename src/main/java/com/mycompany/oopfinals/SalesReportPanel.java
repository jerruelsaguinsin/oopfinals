
package com.mycompany.oopfinals;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.*;

public class SalesReportPanel extends javax.swing.JPanel {

    /**
     * Creates new form SalesReportPanel
     */
    public SalesReportPanel() {
        initComponents();
        styleFlatComponents(this);
        lockReportTableSize();
        resetSalesButton.addActionListener(this::resetSalesButtonActionPerformed);
        aiFeedbackArea.setLineWrap(true);
        aiFeedbackArea.setWrapStyleWord(true);
        aiFeedbackArea.setEditable(false);
        java.util.Calendar cal = java.util.Calendar.getInstance();
    endDateChooser.setDate(cal.getTime());

    cal.add(java.util.Calendar.MONTH, -1);
    startDateChooser.setDate(cal.getTime());
    
    salesTable.getTableHeader().setFont(
    new java.awt.Font("Poppins", java.awt.Font.BOLD, 12)
);

    loadSalesReport();
    }

private String formatOrderCode(int orderId) {
    return String.format("ORD-%06d", orderId);
}

private void resetSalesReport() {
    java.util.Calendar cal = java.util.Calendar.getInstance();
    endDateChooser.setDate(cal.getTime());

    cal.add(java.util.Calendar.MONTH, -1);
    startDateChooser.setDate(cal.getTime());

    loadSalesReport();
}

private void lockReportTableSize() {
    salesTable.setRowHeight(24);
    salesTable.setFillsViewportHeight(true);
    salesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    jScrollPane2.setPreferredSize(new java.awt.Dimension(600, 320));
    jScrollPane2.setMinimumSize(new java.awt.Dimension(600, 320));
}

private void styleFlatComponents(java.awt.Container container) {
    for (java.awt.Component component : container.getComponents()) {
        if (component instanceof JButton button) {
            java.awt.Color background = button.getBackground();
            button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            button.setFocusPainted(false);
            button.setBackground(background);

            if (button.isContentAreaFilled() && background != null
                    && background.getRed() > 220 && background.getGreen() > 220 && background.getBlue() > 220) {
                button.setBackground(java.awt.Color.WHITE);
                button.setOpaque(true);
                button.setBorderPainted(true);
            }
        } else if (component instanceof JScrollPane scrollPane) {
            scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI());
            scrollPane.getHorizontalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI());
        }

        if (component instanceof java.awt.Container child) {
            styleFlatComponents(child);
        }
    }
}
private void loadSalesTable(java.sql.Date startDate, java.sql.Date endDate) throws Exception {
    Connection conn = DBConnection.getConnection();

    PreparedStatement ps = conn.prepareStatement(
        "SELECT o.id, o.date_completed, o.customer_name, o.platform, o.payment_method, " +
        "SUM(oi.quantity) AS items_sold, o.total_price " +
        "FROM orders o " +
        "LEFT JOIN order_items oi ON o.id = oi.order_id " +
        "WHERE TRIM(o.status)='Completed' " +
        "AND DATE(COALESCE(o.date_completed, o.order_date)) BETWEEN ? AND ? " +
        "GROUP BY o.id, o.date_completed, o.customer_name, o.platform, o.payment_method, o.total_price " +
        "ORDER BY COALESCE(o.date_completed, o.order_date) DESC"
    );

    ps.setDate(1, startDate);
    ps.setDate(2, endDate);

    ResultSet rs = ps.executeQuery();

    DefaultTableModel model = (DefaultTableModel) salesTable.getModel();
    model.setRowCount(0);

    while (rs.next()) {
        model.addRow(new Object[]{
            formatOrderCode(rs.getInt("id")),
            rs.getTimestamp("date_completed"),
            rs.getString("customer_name"),
            rs.getString("platform"),
            rs.getString("payment_method"),
            rs.getInt("items_sold"),
            "PHP " + String.format("%.2f", rs.getDouble("total_price"))
        });
    }
}
    
    
 private void loadSummary(java.sql.Date startDate, java.sql.Date endDate) throws Exception {
    Connection conn = DBConnection.getConnection();

    PreparedStatement ps = conn.prepareStatement(
    "SELECT " +
    "COALESCE(SUM(CASE WHEN TRIM(status)='Completed' THEN total_price ELSE 0 END), 0) AS total_sales, " +
    "COUNT(*) AS total_orders, " +
    "SUM(CASE WHEN TRIM(status)='Completed' THEN 1 ELSE 0 END) AS completed_orders, " +
    "SUM(CASE WHEN TRIM(status)='Cancelled' THEN 1 ELSE 0 END) AS cancelled_orders, " +
    "COALESCE(AVG(CASE WHEN TRIM(status)='Completed' THEN total_price END), 0) AS average_order_value " +
    "FROM orders " +
    "WHERE (TRIM(status)='Completed' AND DATE(COALESCE(date_completed, order_date)) BETWEEN ? AND ?) " +
    "OR (TRIM(status)='Cancelled' AND DATE(order_date) BETWEEN ? AND ?)"
);

ps.setDate(1, startDate);
ps.setDate(2, endDate);
ps.setDate(3, startDate);
ps.setDate(4, endDate);

    ResultSet rs = ps.executeQuery();

    if (rs.next()) {
        actualTotalSalesLabel.setText("PHP " + String.format("%.2f", rs.getDouble("total_sales")));
        actualNumberOfOrdersLabel.setText(String.valueOf(rs.getInt("total_orders")));
        actualCompletedOrdersLabel.setText(String.valueOf(rs.getInt("completed_orders")));
        actualCancelledOrdersLabe.setText(String.valueOf(rs.getInt("cancelled_orders")));
        actualAverageOrderValueLabel.setText("PHP " + String.format("%.2f", rs.getDouble("average_order_value")));
    }
}   
    
 private void loadTopSummary(java.sql.Date startDate, java.sql.Date endDate) throws Exception {
    String topPlatform = getSingleValue(
        "SELECT platform FROM orders " +
        "WHERE TRIM(status)='Completed' AND DATE(COALESCE(date_completed, order_date)) BETWEEN ? AND ? " +
        "GROUP BY platform ORDER BY SUM(total_price) DESC LIMIT 1",
        startDate,
        endDate
    );

    String bestSellingItem = getSingleValue(
        "SELECT oi.item_name FROM order_items oi " +
        "JOIN orders o ON oi.order_id = o.id " +
        "WHERE TRIM(o.status)='Completed' AND DATE(COALESCE(o.date_completed, o.order_date)) BETWEEN ? AND ? " +
        "GROUP BY oi.item_name ORDER BY SUM(oi.quantity) DESC LIMIT 1",
        startDate,
        endDate
    );

    String topPaymentMethod = getSingleValue(
        "SELECT payment_method FROM orders " +
        "WHERE TRIM(status)='Completed' AND DATE(COALESCE(date_completed, order_date)) BETWEEN ? AND ? " +
        "GROUP BY payment_method ORDER BY COUNT(*) DESC LIMIT 1",
        startDate,
        endDate
    );

    String bestRevenueItem = getSingleValue(
        "SELECT oi.item_name FROM order_items oi " +
        "JOIN orders o ON oi.order_id = o.id " +
        "WHERE TRIM(o.status)='Completed' AND DATE(COALESCE(o.date_completed, o.order_date)) BETWEEN ? AND ? " +
        "GROUP BY oi.item_name ORDER BY SUM(oi.subtotal) DESC LIMIT 1",
        startDate,
        endDate
    );

    actualTopPlatformLabel.setText(topPlatform);
    actualBestSellingItemLabel.setText(bestSellingItem);
    actualTopPaymentMethodLabel.setText(topPaymentMethod);
    actualBestRevenueItemLabel.setText(bestRevenueItem);
}
 
 private String getSingleValue(String sql, java.sql.Date startDate, java.sql.Date endDate) throws Exception {
    Connection conn = DBConnection.getConnection();

    PreparedStatement ps = conn.prepareStatement(sql);
    ps.setDate(1, startDate);
    ps.setDate(2, endDate);

    ResultSet rs = ps.executeQuery();

    if (rs.next()) {
        return rs.getString(1);
    }

    return "-";
}
 
    private void loadSalesReport() {
    try {
        java.util.Date startDate = startDateChooser.getDate();
        java.util.Date endDate = endDateChooser.getDate();

        if (startDate == null || endDate == null) {
            JOptionPane.showMessageDialog(this, "Please select start and end dates.");
            return;
        }

        if (startDate.after(endDate)) {
            JOptionPane.showMessageDialog(this, "Start date must be before end date.");
            return;
        }

        java.sql.Date sqlStartDate = new java.sql.Date(startDate.getTime());
        java.sql.Date sqlEndDate = new java.sql.Date(endDate.getTime());

        loadSummary(sqlStartDate, sqlEndDate);
        loadSalesTable(sqlStartDate, sqlEndDate);
        loadTopSummary(sqlStartDate, sqlEndDate);
        loadAiFeedback();

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to load sales report.");
    }
}

private void loadAiFeedback() {
    double totalSales = parseMoney(actualTotalSalesLabel.getText());
    double averageOrder = parseMoney(actualAverageOrderValueLabel.getText());
    int totalOrders = parseInt(actualNumberOfOrdersLabel.getText());
    int completedOrders = parseInt(actualCompletedOrdersLabel.getText());
    int cancelledOrders = parseInt(actualCancelledOrdersLabe.getText());

    StringBuilder feedback = new StringBuilder();

    if (completedOrders == 0) {
        feedback.append("No completed sales were found in this date range. Check pending/processing orders and follow up before stock sits too long.");
    } else {
        feedback.append("Sales reached PHP ").append(String.format("%.2f", totalSales))
                .append(" across ").append(completedOrders).append(" completed order(s). ");

        if (averageOrder > 0) {
            feedback.append("Average order value is PHP ").append(String.format("%.2f", averageOrder)).append(". ");
        }

        String platform = actualTopPlatformLabel.getText();
        if (!"-".equals(platform)) {
            feedback.append(platform).append(" is currently the strongest platform, so prioritize replies and promos there. ");
        }

        String bestItem = actualBestSellingItemLabel.getText();
        if (!"-".equals(bestItem)) {
            feedback.append(bestItem).append(" is the best seller; keep its stock/service availability ready. ");
        }

        String revenueItem = actualBestRevenueItemLabel.getText();
        if (!"-".equals(revenueItem) && !revenueItem.equals(bestItem)) {
            feedback.append(revenueItem).append(" brings the most revenue and may be worth featuring more often. ");
        }
    }

    if (cancelledOrders > 0) {
        double cancelRate = totalOrders == 0 ? 0 : (cancelledOrders * 100.0 / totalOrders);
        feedback.append("\n\nCancellation rate is ").append(String.format("%.1f", cancelRate))
                .append("%. Review cancelled orders for delivery, payment, or stock issues.");
    }

    aiFeedbackArea.setText(feedback.toString());
    aiFeedbackArea.setCaretPosition(0);
}

private double parseMoney(String value) {
    try {
        return Double.parseDouble(value.replace("PHP", "").replace(",", "").trim());
    } catch (Exception e) {
        return 0;
    }
}

private int parseInt(String value) {
    try {
        return Integer.parseInt(value.trim());
    } catch (Exception e) {
        return 0;
    }
}
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        startDateChooser = new com.toedter.calendar.JDateChooser();
        endDateChooser = new com.toedter.calendar.JDateChooser();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        generateButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        totalsalesLabel = new javax.swing.JLabel();
        numberOfOrdersLabel = new javax.swing.JLabel();
        completedOrdersLabel = new javax.swing.JLabel();
        cancelledOrdersLabel = new javax.swing.JLabel();
        averageOrderValue = new javax.swing.JLabel();
        topPlatformLabel = new javax.swing.JLabel();
        bestSellingItemLabel = new javax.swing.JLabel();
        topPaymentMethodLabel = new javax.swing.JLabel();
        bestRevenueItemLabel = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        salesTable = new javax.swing.JTable();
        actualTotalSalesLabel = new javax.swing.JLabel();
        actualNumberOfOrdersLabel = new javax.swing.JLabel();
        actualCompletedOrdersLabel = new javax.swing.JLabel();
        actualCancelledOrdersLabe = new javax.swing.JLabel();
        actualAverageOrderValueLabel = new javax.swing.JLabel();
        actualTopPlatformLabel = new javax.swing.JLabel();
        actualBestSellingItemLabel = new javax.swing.JLabel();
        actualTopPaymentMethodLabel = new javax.swing.JLabel();
        actualBestRevenueItemLabel = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        aiFeedbackArea = new javax.swing.JTextArea();
        resetSalesButton = new javax.swing.JButton();

        startDateChooser.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N

        endDateChooser.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N

        jLabel4.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel4.setText("START DATE");

        jLabel5.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel5.setText("END DATE");

        jLabel6.setFont(new java.awt.Font("Poppins", 1, 14)); // NOI18N
        jLabel6.setText("DATE FILTERS");

        generateButton.setBackground(new java.awt.Color(0, 0, 0));
        generateButton.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        generateButton.setForeground(new java.awt.Color(255, 255, 255));
        generateButton.setText("Generate Report");
        generateButton.addActionListener(this::generateButtonActionPerformed);

        jLabel3.setFont(new java.awt.Font("Poppins", 1, 24)); // NOI18N
        jLabel3.setText("SALES REPORT");

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));

        jLabel8.setFont(new java.awt.Font("Poppins", 1, 14)); // NOI18N
        jLabel8.setText("SUMMARY");

        totalsalesLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        totalsalesLabel.setText("Total Sales:");

        numberOfOrdersLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        numberOfOrdersLabel.setText("Number of Orders:");

        completedOrdersLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        completedOrdersLabel.setText("Completed Orders:");

        cancelledOrdersLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        cancelledOrdersLabel.setText("Cancelled Orders:");

        averageOrderValue.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        averageOrderValue.setText("Average Order Value:");

        topPlatformLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        topPlatformLabel.setText("Top Platform:");

        bestSellingItemLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        bestSellingItemLabel.setText("Best Selling Item:");

        topPaymentMethodLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        topPaymentMethodLabel.setText("Top Payment Method:");

        bestRevenueItemLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        bestRevenueItemLabel.setText("Best Revenue Item:");

        jLabel7.setFont(new java.awt.Font("Poppins", 1, 14)); // NOI18N
        jLabel7.setText("SALES TABLE");

        salesTable.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        salesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Order ID", "Date Completed", "Customer", "Platform", "Payment", "Items", "Gross Sales"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(salesTable);

        actualTotalSalesLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        actualTotalSalesLabel.setText("(total sales)");

        actualNumberOfOrdersLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        actualNumberOfOrdersLabel.setText("(no.  of orders)");

        actualCompletedOrdersLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        actualCompletedOrdersLabel.setText("(no. of completed orders)");

        actualCancelledOrdersLabe.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        actualCancelledOrdersLabe.setText("(no. of cancelled orders)");

        actualAverageOrderValueLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        actualAverageOrderValueLabel.setText("(avg. order value)");

        actualTopPlatformLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        actualTopPlatformLabel.setText("(top platform)");

        actualBestSellingItemLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        actualBestSellingItemLabel.setText("(best selling item)");

        actualTopPaymentMethodLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        actualTopPaymentMethodLabel.setText("(top payment method)");

        actualBestRevenueItemLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        actualBestRevenueItemLabel.setText("(best revenue item)");

        jLabel27.setFont(new java.awt.Font("Poppins", 1, 14)); // NOI18N
        jLabel27.setText("AI FEEDBACK");
        jLabel27.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        aiFeedbackArea.setColumns(20);
        aiFeedbackArea.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        aiFeedbackArea.setRows(5);
        jScrollPane1.setViewportView(aiFeedbackArea);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(numberOfOrdersLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(actualNumberOfOrdersLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(totalsalesLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(actualTotalSalesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(topPlatformLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(actualTopPlatformLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(averageOrderValue)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(actualAverageOrderValueLabel))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(completedOrdersLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(actualCompletedOrdersLabel))
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                    .addComponent(bestSellingItemLabel)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(actualBestSellingItemLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                    .addComponent(cancelledOrdersLabel)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(actualCancelledOrdersLabe)))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(topPaymentMethodLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(actualTopPaymentMethodLabel))
                            .addComponent(jLabel8)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(bestRevenueItemLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(actualBestRevenueItemLabel)))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel27)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 308, Short.MAX_VALUE)))
                    .addComponent(jScrollPane2))
                .addGap(25, 25, 25))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jLabel27))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(totalsalesLabel)
                            .addComponent(actualTotalSalesLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(numberOfOrdersLabel)
                            .addComponent(actualNumberOfOrdersLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(completedOrdersLabel)
                            .addComponent(actualCompletedOrdersLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cancelledOrdersLabel)
                            .addComponent(actualCancelledOrdersLabe))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(averageOrderValue)
                            .addComponent(actualAverageOrderValueLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(topPlatformLabel)
                            .addComponent(actualTopPlatformLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(bestSellingItemLabel)
                            .addComponent(actualBestSellingItemLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(topPaymentMethodLabel)
                            .addComponent(actualTopPaymentMethodLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(bestRevenueItemLabel)
                            .addComponent(actualBestRevenueItemLabel)))
                    .addComponent(jScrollPane1))
                .addGap(18, 18, 18)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 320, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(25, Short.MAX_VALUE))
        );

        resetSalesButton.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        resetSalesButton.setText("Reset");
        resetSalesButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(endDateChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(startDateChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(generateButton)
                    .addComponent(jLabel6)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(resetSalesButton))
                .addGap(21, 21, 21)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(22, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(28, 28, 28)
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4)
                                .addGap(25, 25, 25))
                            .addComponent(startDateChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(endDateChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(19, 19, 19)
                        .addComponent(generateButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(resetSalesButton))
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(15, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void generateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateButtonActionPerformed
        loadSalesReport();
    }//GEN-LAST:event_generateButtonActionPerformed

    private void resetSalesButtonActionPerformed(java.awt.event.ActionEvent evt) {
        resetSalesReport();
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel actualAverageOrderValueLabel;
    private javax.swing.JLabel actualBestRevenueItemLabel;
    private javax.swing.JLabel actualBestSellingItemLabel;
    private javax.swing.JLabel actualCancelledOrdersLabe;
    private javax.swing.JLabel actualCompletedOrdersLabel;
    private javax.swing.JLabel actualNumberOfOrdersLabel;
    private javax.swing.JLabel actualTopPaymentMethodLabel;
    private javax.swing.JLabel actualTopPlatformLabel;
    private javax.swing.JLabel actualTotalSalesLabel;
    private javax.swing.JTextArea aiFeedbackArea;
    private javax.swing.JLabel averageOrderValue;
    private javax.swing.JLabel bestRevenueItemLabel;
    private javax.swing.JLabel bestSellingItemLabel;
    private javax.swing.JLabel cancelledOrdersLabel;
    private javax.swing.JLabel completedOrdersLabel;
    private com.toedter.calendar.JDateChooser endDateChooser;
    private javax.swing.JButton generateButton;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel numberOfOrdersLabel;
    private javax.swing.JButton resetSalesButton;
    private javax.swing.JTable salesTable;
    private com.toedter.calendar.JDateChooser startDateChooser;
    private javax.swing.JLabel topPaymentMethodLabel;
    private javax.swing.JLabel topPlatformLabel;
    private javax.swing.JLabel totalsalesLabel;
    // End of variables declaration//GEN-END:variables
}
