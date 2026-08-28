package com.faststore.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles local (no-login) Cart + Wishlist(Liked) storage using SharedPreferences.
 */
public class CartManager {

    private static final String PREF_CART = "LocalCart";
    private static final String PREF_FAV = "LocalFavorites";
    private static final String KEY_CART_ITEMS = "cart_items";
    private static final String KEY_FAV_ITEMS = "fav_items";

    // ---------- CART ----------

    public static void addToCart(Context ctx, Product p) {
        try {
            JSONArray arr = getArray(ctx, PREF_CART, KEY_CART_ITEMS);
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getJSONObject(i).getString("id").equals(p.getId())) return;
            }
            arr.put(toJson(p));
            saveArray(ctx, PREF_CART, KEY_CART_ITEMS, arr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void removeFromCart(Context ctx, String productId) {
        removeFromArray(ctx, PREF_CART, KEY_CART_ITEMS, productId);
    }

    public static boolean isInCart(Context ctx, String productId) {
        return containsId(ctx, PREF_CART, KEY_CART_ITEMS, productId);
    }

    public static List<Product> getCartItems(Context ctx) {
        return getItems(ctx, PREF_CART, KEY_CART_ITEMS);
    }

    // ---------- FAVORITES / LIKED ----------

    public static void toggleFavorite(Context ctx, Product p) {
        if (isFavorite(ctx, p.getId())) {
            removeFromArray(ctx, PREF_FAV, KEY_FAV_ITEMS, p.getId());
        } else {
            try {
                JSONArray arr = getArray(ctx, PREF_FAV, KEY_FAV_ITEMS);
                arr.put(toJson(p));
                saveArray(ctx, PREF_FAV, KEY_FAV_ITEMS, arr);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isFavorite(Context ctx, String productId) {
        return containsId(ctx, PREF_FAV, KEY_FAV_ITEMS, productId);
    }

    public static List<Product> getFavoriteItems(Context ctx) {
        return getItems(ctx, PREF_FAV, KEY_FAV_ITEMS);
    }

    // ---------- shared helpers ----------

    private static boolean containsId(Context ctx, String prefName, String key, String productId) {
        try {
            JSONArray arr = getArray(ctx, prefName, key);
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getJSONObject(i).getString("id").equals(productId)) return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void removeFromArray(Context ctx, String prefName, String key, String productId) {
        try {
            JSONArray arr = getArray(ctx, prefName, key);
            JSONArray newArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (!obj.getString("id").equals(productId)) newArr.put(obj);
            }
            saveArray(ctx, prefName, key, newArr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static List<Product> getItems(Context ctx, String prefName, String key) {
        List<Product> list = new ArrayList<>();
        try {
            JSONArray arr = getArray(ctx, prefName, key);
            for (int i = 0; i < arr.length(); i++) {
                list.add(fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static JSONArray getArray(Context ctx, String prefName, String key) throws JSONException {
        SharedPreferences prefs = ctx.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        return new JSONArray(prefs.getString(key, "[]"));
    }

    private static void saveArray(Context ctx, String prefName, String key, JSONArray arr) {
        SharedPreferences prefs = ctx.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        prefs.edit().putString(key, arr.toString()).apply();
    }

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
