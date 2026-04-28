package com.mycompany.oopfinals;

public class Product {
    private int id;
    private String name;
    private int stock;
    private double price;

    public Product(int id, String name, int stock, double price) {
        this.id = id;
        this.name = name;
        this.stock = stock;
        this.price = price;
    }

    public int getId() { return id; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
}