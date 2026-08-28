package com.faststore.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Product implements Serializable {
    private String id;
    private String name;
    private String price;
    private String oldPrice;
    private String picture;
    private String currencyId;
    private String category;
    private String description;
    private List<String> images;

    public Product(String id, String name, String price, String picture, String currencyId) {
        this(id, name, price, null, picture, currencyId, "General", "", null);
    }

    public Product(String id, String name, String price, String picture, String currencyId, String category, String description) {
        this(id, name, price, null, picture, currencyId, category, description, null);
    }

    public Product(String id, String name, String price, String oldPrice, String picture, String currencyId, String category, String description, List<String> images) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.oldPrice = oldPrice;
        this.picture = picture;
        this.currencyId = currencyId;
        this.category = category == null || category.trim().isEmpty() ? "General" : category;
        this.description = description == null ? "" : description;

        this.images = new ArrayList<>();
        if (images != null) {
            for (String img : images) {
                if (img != null && !img.trim().isEmpty()) this.images.add(img);
            }
        }
        if (picture != null && !picture.trim().isEmpty() && !this.images.contains(picture)) {
            this.images.add(0, picture);
        }
        if (this.images.isEmpty() && picture != null) {
            this.images.add(picture);
        }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPrice() { return price; }
    public String getOldPrice() { return oldPrice; }
    public String getPicture() { return picture; }
    public String getCurrencyId() { return currencyId; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public List<String> getImages() { return images; }

    /** Returns discount % (e.g. 25) if oldPrice > price, otherwise -1 if not applicable. */
    public int getDiscountPercent() {
        try {
            if (oldPrice == null || oldPrice.trim().isEmpty()) return -1;
            double oldVal = Double.parseDouble(oldPrice.replaceAll("[^0-9.]", ""));
            double newVal = Double.parseDouble(price.replaceAll("[^0-9.]", ""));
            if (oldVal <= newVal || oldVal <= 0) return -1;
            return (int) Math.round(((oldVal - newVal) / oldVal) * 100);
        } catch (Exception e) {
            return -1;
        }
    }
}
