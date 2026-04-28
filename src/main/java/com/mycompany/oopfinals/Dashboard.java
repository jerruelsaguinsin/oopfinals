import javax.swing.*;

public class Dashboard extends JFrame {

    public Dashboard() {
        setTitle("Dashboard");
        setSize(400,300);

        JButton orderBtn = new JButton("Create Order");

        orderBtn.addActionListener(e -> {
            OrderService os = new OrderService();
            PlatformOrder order = new PlatformOrder(1, 5, "Shopee");
            os.createOrder(order, 100);
        });

        add(orderBtn);
        setVisible(true);
    }
}   