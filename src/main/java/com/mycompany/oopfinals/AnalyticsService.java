package com.mycompany.oopfinals;

import java.sql.*;

public class AnalyticsService {

    // BEST SELLING PRODUCT
    public static String getBestSellingProduct() {

        try {

            Connection conn = DBConnection.getConnection();

            String sql =
                    "SELECT p.name, SUM(o.quantity) AS total_sold " +
                    "FROM orders o " +
                    "JOIN products p ON o.product_id = p.id " +
                    "GROUP BY p.name " +
                    "ORDER BY total_sold DESC LIMIT 1";

            Statement st = conn.createStatement();

            ResultSet rs = st.executeQuery(sql);

            if (rs.next()) {

                return rs.getString("name") +
                        " (Sold: " +
                        rs.getInt("total_sold") + ")";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "No data";
    }

    // PLATFORM SALES STATS
    public static String getPlatformStats() {

        StringBuilder stats = new StringBuilder();

        try {

            Connection conn = DBConnection.getConnection();

            String sql =
                    "SELECT platform, COUNT(*) AS total_orders " +
                    "FROM orders " +
                    "GROUP BY platform";

            Statement st = conn.createStatement();

            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                stats.append(
                        rs.getString("platform")
                );

                stats.append(" : ");

                stats.append(
                        rs.getInt("total_orders")
                );

                stats.append(" orders\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stats.toString();
    }

    // BEST SELLING SERVICE
    public static String getBestSellingService() {

        try {

            Connection conn = DBConnection.getConnection();

            String sql =
                    "SELECT s.service_name, SUM(so.quantity) AS total_sold " +
                    "FROM service_orders so " +
                    "JOIN services s ON so.service_id = s.id " +
                    "GROUP BY s.service_name " +
                    "ORDER BY total_sold DESC LIMIT 1";

            Statement st = conn.createStatement();

            ResultSet rs = st.executeQuery(sql);

            if (rs.next()) {

                return rs.getString("service_name") +
                        " (Sold: " +
                        rs.getInt("total_sold") + ")";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "No data";
    }

    // TOTAL SERVICE SALES
    public static double getTotalServiceSales() {

        try {

            Connection conn = DBConnection.getConnection();

            String sql =
                    "SELECT SUM(total) AS total_sales FROM service_orders";

            Statement st = conn.createStatement();

            ResultSet rs = st.executeQuery(sql);

            if (rs.next()) {

                return rs.getDouble("total_sales");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}