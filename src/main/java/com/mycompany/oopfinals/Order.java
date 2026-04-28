package com.mycompany.oopfinals;

public abstract class Order {
    protected int productId;
    protected int quantity;

    public abstract double calculateTotal(double price);
}