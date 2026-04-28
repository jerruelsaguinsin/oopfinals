import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginForm extends JFrame {

    JTextField userField;
    JPasswordField passField;

    public LoginForm() {
        setTitle("Login");
        setSize(300,200);
        setLayout(new FlowLayout());

        userField = new JTextField(15);
        passField = new JPasswordField(15);

        JButton loginBtn = new JButton("Login");

        add(new JLabel("Username"));
        add(userField);
        add(new JLabel("Password"));
        add(passField);
        add(loginBtn);

        loginBtn.addActionListener(e -> login());

        setVisible(true);
    }

    private void login() {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM users WHERE username=? AND password=?"
            );

            ps.setString(1, userField.getText());
            ps.setString(2, new String(passField.getPassword()));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Login Success!");
                new Dashboard();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid login");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}