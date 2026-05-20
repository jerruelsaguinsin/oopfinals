package com.mycompany.oopfinals;


import javax.swing.*;
import java.sql.*;

public class ServiceService {

    public static void createServiceOrder(int serviceId, int quantity) {

        try {

            Connection conn = DBConnection.getConnection();
            if (conn == null) return;

            PreparedStatement ps1 = conn.prepareStatement(
                    "SELECT price FROM services WHERE id=?"
            );

            ps1.setInt(1, serviceId);
            ResultSet rs = ps1.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(null, "Service not found!");
                return;
            }

            double price = rs.getDouble("price");
            double total = price * quantity;

            PreparedStatement ps2 = conn.prepareStatement(
                    "INSERT INTO service_orders(service_id, quantity, total) VALUES (?,?,?)"
            );

            ps2.setInt(1, serviceId);
            ps2.setInt(2, quantity);
            ps2.setDouble(3, total);

            ps2.executeUpdate();

            JOptionPane.showMessageDialog(null,
                    "Service Order Created!\nTotal: ₱" + total
            );

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }
}