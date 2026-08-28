package com.faststore.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Shared helper to turn a product JSON object (from api.php) into a Product. */
public class ProductParser {

    public static Product parse(JSONObject obj) throws JSONException {
        List<String> images = new ArrayList<>();
        if (obj.has("images")) {
            JSONArray imgArr = obj.optJSONArray("images");
            if (imgArr != null) {
                for (int j = 0; j < imgArr.length(); j++) {
                    images.add(imgArr.optString(j));
                }
            }
        }

        return new Product(
                obj.getString("id"),
                obj.getString("name"),
                obj.optString("price", "0.00"),
                obj.has("old_price") ? obj.optString("old_price", null) : obj.optString("oldPrice", null),
                obj.optString("picture", ""),
                obj.optString("currencyId", "$"),
                obj.optString("category", "General"),
                obj.optString("description", ""),
                images
        );
    }

    public static List<Product> parseList(JSONArray arr) throws JSONException {
        List<Product> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parse(arr.getJSONObject(i)));
        }
        return list;
    }
}
