import java.sql.*;

public class OrderService {

    public void createOrder(PlatformOrder order, double price) {

        try {
            Connection conn = DBConnection.getConnection();

            // CHECK STOCK
            PreparedStatement check = conn.prepareStatement(
                "SELECT stock FROM products WHERE id=?"
            );
            check.setInt(1, order.productId);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                int stock = rs.getInt("stock");

                if (stock < order.quantity) {
                    System.out.println("Not enough stock!");
                    return;
                }
            }

            // INSERT ORDER
            PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO orders (product_id, platform, quantity, total_price) VALUES (?, ?, ?, ?)"
            );
            insert.setInt(1, order.productId);
            insert.setString(2, order.getPlatform());
            insert.setInt(3, order.quantity);
            insert.setDouble(4, order.calculateTotal(price));
            insert.executeUpdate();

            // UPDATE STOCK
            PreparedStatement update = conn.prepareStatement(
                "UPDATE products SET stock = stock - ? WHERE id=?"
            );
            update.setInt(1, order.quantity);
            update.setInt(2, order.productId);
            update.executeUpdate();

            System.out.println("Order success!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}