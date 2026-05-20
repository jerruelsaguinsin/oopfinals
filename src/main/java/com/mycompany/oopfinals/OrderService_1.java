package com.mycompany.oopfinals;


import javax.swing.*;
import java.sql.*;

public class OrderService_1 {

    public static void createOrder(int productId, int quantity, String platform) {

        try {

            Connection conn = DBConnection.getConnection();
            if (conn == null) return;

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT price, stock FROM products WHERE id=?"
            );

            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(null, "Product not found!");
                return;
            }

            double price = rs.getDouble("price");
            int stock = rs.getInt("stock");

            if (quantity > stock) {
                JOptionPane.showMessageDialog(null, "Not enough stock!");
                return;
            }

            double totalPrice = price * quantity;

            PreparedStatement ps2 = conn.prepareStatement(
                    "INSERT INTO orders(product_id, platform, total_price, quantity) VALUES (?,?,?,?)"
            );

            ps2.setInt(1, productId);
            ps2.setString(2, platform);
            ps2.setDouble(3, totalPrice);
            ps2.setInt(4, quantity);

            ps2.executeUpdate();

            PreparedStatement ps3 = conn.prepareStatement(
                    "UPDATE products SET stock = stock - ? WHERE id=?"
            );

            ps3.setInt(1, quantity);
            ps3.setInt(2, productId);

            ps3.executeUpdate();

            JOptionPane.showMessageDialog(null,
                    "Order Created Successfully!\n\n" +
                    generateReceipt(productId, quantity, totalPrice, platform)
            );

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    public static String generateReceipt(int productId, int quantity, double total, String platform) {

        return "===== RECEIPT =====\n"
                + "Product ID: " + productId + "\n"
                + "Quantity: " + quantity + "\n"
                + "Platform: " + platform + "\n"
                + "Total: ₱" + total + "\n"
                + "===================";
    }
}