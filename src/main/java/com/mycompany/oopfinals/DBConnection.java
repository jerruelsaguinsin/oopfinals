package com.mycompany.oopfinals;

import java.sql.*;

public class DBConnection {
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/barodiseno_db",
                "root",
                ""
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}