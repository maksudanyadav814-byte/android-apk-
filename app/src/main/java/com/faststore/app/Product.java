package com.faststore.app;

public class Product {
    private String id;
    private String name;
    private String price;
    private String picture;
    private String currencyId;

    public Product(String id, String name, String price, String picture, String currencyId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.picture = picture;
        this.currencyId = currencyId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPrice() { return price; }
    public String getPicture() { return picture; }
    public String getCurrencyId() { return currencyId; }
}
