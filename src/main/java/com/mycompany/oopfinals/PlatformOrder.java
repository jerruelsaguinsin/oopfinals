package com.mycompany.oopfinals;

public class PlatformOrder extends Order {
    private String platform;

    public PlatformOrder(int productId, int quantity, String platform) {
        this.productId = productId;
        this.quantity = quantity;
        this.platform = platform;
    }

    @Override
    public double calculateTotal(double price) {
        return quantity * price;
    }

    public String getPlatform() {
        return platform;
    }
}