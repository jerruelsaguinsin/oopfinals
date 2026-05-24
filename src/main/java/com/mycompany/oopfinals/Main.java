package com.mycompany.oopfinals;

public class Main {
   public static void main(String args[]) {
    try {
        javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getCrossPlatformLookAndFeelClassName());
    } catch (Exception e) {
        e.printStackTrace();
    }

    java.awt.EventQueue.invokeLater(() -> {
        new LoginForm();
    });
}
}
