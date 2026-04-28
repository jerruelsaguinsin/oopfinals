package com.mycompany.oopfinals;

import java.sql.*;

public class AIService {

    public void bestSellingProduct() {
        try {
            Connection conn = DBConnection.getConnection();

            String query = "SELECT product_id, SUM(quantity) as total " +
                           "FROM orders GROUP BY product_id ORDER BY total DESC LIMIT 1";

            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("Best selling product ID: " + rs.getInt("product_id"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}