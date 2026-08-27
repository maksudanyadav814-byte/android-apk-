package com.faststore.app;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Simple in-memory cache of products loaded so far in this app session.
 * Used to power Related Products and local Search without extra API calls.
 * NOTE: if your backend API supports ?category= or ?q= search params,
 * you can swap the client-side filtering in SearchFragment for a real
 * server-side call for better results across your full catalog.
 */
public class DataCache {

    public static final List<Product> allProducts = new ArrayList<>();

    public static void addAll(List<Product> products) {
        for (Product p : products) {
            boolean exists = false;
            for (Product existing : allProducts) {
                if (existing.getId().equals(p.getId())) { exists = true; break; }
            }
            if (!exists) allProducts.add(p);
        }
    }

    public static void clear() {
        allProducts.clear();
    }

    public static List<String> getCategories() {
        Set<String> set = new LinkedHashSet<>();
        set.add("All");
        for (Product p : allProducts) {
            set.add(p.getCategory());
        }
        return new ArrayList<>(set);
    }

    public static List<Product> getRelated(Product current, int limit) {
        List<Product> related = new ArrayList<>();
        for (Product p : allProducts) {
            if (p.getId().equals(current.getId())) continue;
            if (p.getCategory().equalsIgnoreCase(current.getCategory())) {
                related.add(p);
                if (related.size() >= limit) break;
            }
        }
        // Fallback: if not enough in same category, fill with any other products
        if (related.size() < limit) {
            for (Product p : allProducts) {
                if (p.getId().equals(current.getId())) continue;
                if (!related.contains(p)) {
                    related.add(p);
                    if (related.size() >= limit) break;
                }
            }
        }
        return related;
    }

    public static Product findById(String id) {
        for (Product p : allProducts) {
            if (p.getId().equals(id)) return p;
        }
        return null;
    }
}
