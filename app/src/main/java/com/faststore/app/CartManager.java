package com.faststore.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles local (no-login) Cart + Wishlist(favorites) storage using SharedPreferences.
 */
public class CartManager {

    private static final String PREF_CART = "LocalCart";
    private static final String PREF_FAV = "LocalFavorites";
    private static final String KEY_CART_ITEMS = "cart_items";
    private static final String KEY_FAV_IDS = "fav_ids";

    // ---------- CART ----------

    public static void addToCart(Context ctx, Product p) {
        try {
            JSONArray arr = getCartArray(ctx);
            // avoid duplicate entries
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getJSONObject(i).getString("id").equals(p.getId())) return;
            }
            arr.put(toJson(p));
            saveCartArray(ctx, arr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void removeFromCart(Context ctx, String productId) {
        try {
            JSONArray arr = getCartArray(ctx);
            JSONArray newArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (!obj.getString("id").equals(productId)) newArr.put(obj);
            }
            saveCartArray(ctx, newArr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static boolean isInCart(Context ctx, String productId) {
        try {
            JSONArray arr = getCartArray(ctx);
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getJSONObject(i).getString("id").equals(productId)) return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static List<Product> getCartItems(Context ctx) {
        List<Product> list = new ArrayList<>();
        try {
            JSONArray arr = getCartArray(ctx);
            for (int i = 0; i < arr.length(); i++) {
                list.add(fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static JSONArray getCartArray(Context ctx) throws JSONException {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_CART, Context.MODE_PRIVATE);
        return new JSONArray(prefs.getString(KEY_CART_ITEMS, "[]"));
    }

    private static void saveCartArray(Context ctx, JSONArray arr) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_CART, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CART_ITEMS, arr.toString()).apply();
    }

    // ---------- FAVORITES / WISHLIST ----------

    public static void toggleFavorite(Context ctx, String productId) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_FAV, Context.MODE_PRIVATE);
        java.util.Set<String> favs = new java.util.HashSet<>(prefs.getStringSet(KEY_FAV_IDS, new java.util.HashSet<>()));
        if (favs.contains(productId)) {
            favs.remove(productId);
        } else {
            favs.add(productId);
        }
        prefs.edit().putStringSet(KEY_FAV_IDS, favs).apply();
    }

    public static boolean isFavorite(Context ctx, String productId) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_FAV, Context.MODE_PRIVATE);
        return prefs.getStringSet(KEY_FAV_IDS, new java.util.HashSet<>()).contains(productId);
    }

    // ---------- JSON helpers ----------

    private static JSONObject toJson(Product p) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", p.getId());
        obj.put("name", p.getName());
        obj.put("price", p.getPrice());
        obj.put("picture", p.getPicture());
        obj.put("currencyId", p.getCurrencyId());
        obj.put("category", p.getCategory());
        obj.put("description", p.getDescription());
        return obj;
    }

    private static Product fromJson(JSONObject obj) throws JSONException {
        return new Product(
                obj.getString("id"),
                obj.getString("name"),
                obj.optString("price", "0.00"),
                obj.optString("picture", ""),
                obj.optString("currencyId", "$"),
                obj.optString("category", "General"),
                obj.optString("description", "")
        );
    }
}
