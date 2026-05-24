package com.mycompany.oopfinals;

import java.awt.*;
import java.sql.*;
import java.util.*;
import javax.swing.*;

public class SalesGraphPanel extends JPanel {

    private java.util.List<String> labels = new ArrayList<>();
    private java.util.List<Double> values = new ArrayList<>();

    public SalesGraphPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(440, 200));
        setMinimumSize(new Dimension(320, 170));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(440, 200);
    }

    public void loadGraphData() {
        labels.clear();
        values.clear();

        try {
            Connection conn = DBConnection.getConnection();

            PreparedStatement ps = conn.prepareStatement(
                "SELECT DATE(COALESCE(date_completed, order_date)) AS sales_date, SUM(total_price) AS total_sales " +
                "FROM orders " +
                "WHERE TRIM(status)='Completed' " +
                "AND DATE(COALESCE(date_completed, order_date)) >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
                "GROUP BY DATE(COALESCE(date_completed, order_date)) " +
                "ORDER BY sales_date"
            );

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                labels.add(rs.getString("sales_date"));
                values.add(rs.getDouble("total_sales"));
            }

            repaint();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (values.isEmpty()) {
            g.setColor(Color.GRAY);
            g.setFont(new Font("Poppins", Font.PLAIN, 12));
            String message = "No completed sales for the last 7 days.";
            FontMetrics metrics = g.getFontMetrics();
            int x = Math.max(12, (getWidth() - metrics.stringWidth(message)) / 2);
            int y = Math.max(35, getHeight() / 2);
            g.drawString(message, x, y);
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(new Font("Poppins", Font.PLAIN, 11));

        int height = getHeight();
        int width = getWidth();
        int leftPadding = 46;
        int rightPadding = 16;
        int topPadding = 16;
        int bottomPadding = 34;
        int chartWidth = width - leftPadding - rightPadding;
        int chartHeight = height - topPadding - bottomPadding;

        if (chartWidth < 120 || chartHeight < 80) {
            g2.setColor(new Color(60, 60, 60));
            g2.setFont(new Font("Poppins", Font.PLAIN, 12));
            g2.drawString("Expand dashboard to view sales graph.", 12, Math.max(24, height / 2));
            return;
        }

        double maxValue = Collections.max(values);
        if (maxValue <= 0) {
            maxValue = 1;
        }

        int barCount = values.size();
        int barGap = 8;
        int barWidth = Math.max(16, (chartWidth / barCount) - barGap);

        g2.setColor(new Color(245, 245, 245));
        g2.fillRect(leftPadding, topPadding, chartWidth, chartHeight);

        g2.setColor(new Color(220, 220, 220));
        for (int i = 0; i <= 4; i++) {
            int y = topPadding + (chartHeight * i / 4);
            double markerValue = maxValue - (maxValue * i / 4);

            g2.drawLine(leftPadding, y, width - rightPadding, y);
            g2.setColor(new Color(90, 90, 90));
            g2.drawString(String.format("%.0f", markerValue), 8, y + 4);
            g2.setColor(new Color(220, 220, 220));
        }

        g2.setColor(new Color(80, 80, 80));
        g2.drawLine(leftPadding, topPadding, leftPadding, topPadding + chartHeight);
        g2.drawLine(leftPadding, topPadding + chartHeight, width - rightPadding, topPadding + chartHeight);

        for (int i = 0; i < barCount; i++) {
            double value = values.get(i);

            int barHeight = Math.max(2, (int) ((value / maxValue) * chartHeight));
            int x = leftPadding + barGap / 2 + i * (barWidth + barGap);
            int y = topPadding + chartHeight - barHeight;

            g2.setColor(new Color(27, 25, 24));
            g2.fillRoundRect(x, y, barWidth, barHeight, 8, 8);
            g2.setColor(Color.BLACK);
            g2.drawRoundRect(x, y, barWidth, barHeight, 8, 8);

            if (barWidth >= 32 && chartHeight >= 130) {
                g2.setColor(new Color(35, 35, 35));
                g2.drawString(String.format("%.0f", value), x, Math.max(14, y - 6));
            }

            String dateLabel = labels.get(i);
            if (dateLabel.length() >= 10) {
                dateLabel = dateLabel.substring(5);
            }

            g2.drawString(dateLabel, x, height - 18);
        }
    }
}
