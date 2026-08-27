package com.faststore.app;

import java.io.Serializable;

public class Product implements Serializable {
    private String id;
    private String name;
    private String price;
    private String picture;
    private String currencyId;
    private String category;
    private String description;

    public Product(String id, String name, String price, String picture, String currencyId) {
        this(id, name, price, picture, currencyId, "General", "");
    }

    public Product(String id, String name, String price, String picture, String currencyId, String category, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.picture = picture;
        this.currencyId = currencyId;
        this.category = category == null || category.trim().isEmpty() ? "General" : category;
        this.description = description == null ? "" : description;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPrice() { return price; }
    public String getPicture() { return picture; }
    public String getCurrencyId() { return currencyId; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
}
