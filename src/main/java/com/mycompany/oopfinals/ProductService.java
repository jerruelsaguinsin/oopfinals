package com.mycompany.oopfinals;

import java.sql.*;

public class ProductService {

    public int getStock(int productId) {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT stock FROM products WHERE id=?"
            );
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getInt("stock");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}