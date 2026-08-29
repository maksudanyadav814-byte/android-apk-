package com.faststore.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * FastStore - Data layer file (1 of 2 Java files in this project).
 * Contains: Product model, API response parser, local Cart/Liked storage,
 * and all RecyclerView adapters. Screens (Activities/Fragments) live in MainActivity.java.
 */
public class AppData {

    private AppData() { }

    // Point this at your live api.php - update if your domain/path ever changes.
    public static final String API_BASE = "https://powderblue-sparrow-788374.hostingersite.com/android/api.php";

    // =========================================================
    // PRODUCT MODEL - matches columns returned by api.php exactly
    // (id, name, price, oldprice, currencyId, picture, param, url [, category on detail])
    // =========================================================
    public static class Product implements Serializable {
        private String id, name, price, oldPrice, currencyId, picture, param, url, category;

        public Product(String id, String name, String price, String oldPrice, String currencyId,
                       String picture, String param, String url, String category) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.oldPrice = oldPrice;
            this.currencyId = currencyId;
            this.picture = picture;
            this.param = param;
            this.url = url;
            this.category = (category == null || category.trim().isEmpty()) ? "" : category;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getPrice() { return price; }
        public String getOldPrice() { return oldPrice; }
        public String getCurrencyId() { return currencyId; }
        public String getPicture() { return picture; }
        public String getParam() { return param; }
        public String getUrl() { return url; }
        public String getCategory() { return category; }

        /** Returns discount % (e.g. 25) if oldPrice > price, otherwise -1. */
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

        public String currencySymbol() {
            return (currencyId != null && currencyId.equalsIgnoreCase("INR")) ? "₹" : "$";
        }

        /** Turns the raw "Label|Value;Label2|Value2" param string into readable multi-line text. */
        public String getSpecsText() {
            if (param == null || param.trim().isEmpty()) return "No additional details available for this product.";
            StringBuilder sb = new StringBuilder();
            for (String item : param.split(";")) {
                if (item.trim().isEmpty()) continue;
                String[] parts = item.split("\\|", 2);
                if (parts.length == 2) {
                    sb.append("• ").append(parts[0].trim()).append(": ").append(parts[1].trim()).append("\n");
                } else if (parts.length == 1 && !parts[0].trim().isEmpty()) {
                    sb.append("• ").append(parts[0].trim()).append("\n");
                }
            }
            return sb.length() == 0 ? "No additional details available for this product." : sb.toString().trim();
        }
    }

    // =========================================================
    // API RESPONSE PARSER
    // =========================================================
    public static class ProductParser {
        public static Product parse(JSONObject obj) throws JSONException {
            return new Product(
                    obj.optString("id", ""),
                    obj.optString("name", ""),
                    obj.optString("price", "0.00"),
                    obj.has("oldprice") ? obj.optString("oldprice", null) : null,
                    obj.optString("currencyId", "$"),
                    obj.optString("picture", ""),
                    obj.optString("param", ""),
                    obj.optString("url", ""),
                    obj.optString("category", "")
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

    // =========================================================
    // LOCAL CART + LIKED (WISHLIST) STORAGE
    // =========================================================
    public static class CartManager {
        private static final String PREF_CART = "LocalCart";
        private static final String PREF_FAV = "LocalFavorites";
        private static final String KEY_ITEMS = "items";

        public static void addToCart(Context ctx, Product p) {
            try {
                JSONArray arr = getArray(ctx, PREF_CART);
                for (int i = 0; i < arr.length(); i++) {
                    if (arr.getJSONObject(i).getString("id").equals(p.getId())) return;
                }
                arr.put(toJson(p));
                saveArray(ctx, PREF_CART, arr);
            } catch (JSONException e) { e.printStackTrace(); }
        }

        public static void removeFromCart(Context ctx, String id) { removeFromArray(ctx, PREF_CART, id); }
        public static boolean isInCart(Context ctx, String id) { return containsId(ctx, PREF_CART, id); }
        public static List<Product> getCartItems(Context ctx) { return getItems(ctx, PREF_CART); }

        public static void toggleFavorite(Context ctx, Product p) {
            if (isFavorite(ctx, p.getId())) {
                removeFromArray(ctx, PREF_FAV, p.getId());
            } else {
                try {
                    JSONArray arr = getArray(ctx, PREF_FAV);
                    arr.put(toJson(p));
                    saveArray(ctx, PREF_FAV, arr);
                } catch (JSONException e) { e.printStackTrace(); }
            }
        }

        public static boolean isFavorite(Context ctx, String id) { return containsId(ctx, PREF_FAV, id); }
        public static List<Product> getFavoriteItems(Context ctx) { return getItems(ctx, PREF_FAV); }

        private static boolean containsId(Context ctx, String pref, String id) {
            try {
                JSONArray arr = getArray(ctx, pref);
                for (int i = 0; i < arr.length(); i++) {
                    if (arr.getJSONObject(i).getString("id").equals(id)) return true;
                }
            } catch (JSONException e) { e.printStackTrace(); }
            return false;
        }

        private static void removeFromArray(Context ctx, String pref, String id) {
            try {
                JSONArray arr = getArray(ctx, pref);
                JSONArray newArr = new JSONArray();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    if (!obj.getString("id").equals(id)) newArr.put(obj);
                }
                saveArray(ctx, pref, newArr);
            } catch (JSONException e) { e.printStackTrace(); }
        }

        private static List<Product> getItems(Context ctx, String pref) {
            List<Product> list = new ArrayList<>();
            try {
                JSONArray arr = getArray(ctx, pref);
                for (int i = 0; i < arr.length(); i++) list.add(fromJson(arr.getJSONObject(i)));
            } catch (JSONException e) { e.printStackTrace(); }
            return list;
        }

        private static JSONArray getArray(Context ctx, String pref) throws JSONException {
            SharedPreferences prefs = ctx.getSharedPreferences(pref, Context.MODE_PRIVATE);
            return new JSONArray(prefs.getString(KEY_ITEMS, "[]"));
        }

        private static void saveArray(Context ctx, String pref, JSONArray arr) {
            ctx.getSharedPreferences(pref, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, arr.toString()).apply();
        }

        private static JSONObject toJson(Product p) throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("id", p.getId());
            obj.put("name", p.getName());
            obj.put("price", p.getPrice());
            obj.put("oldprice", p.getOldPrice());
            obj.put("currencyId", p.getCurrencyId());
            obj.put("picture", p.getPicture());
            obj.put("param", p.getParam());
            obj.put("url", p.getUrl());
            obj.put("category", p.getCategory());
            return obj;
        }

        private static Product fromJson(JSONObject obj) throws JSONException {
            return new Product(
                    obj.getString("id"), obj.getString("name"), obj.optString("price", "0.00"),
                    obj.isNull("oldprice") ? null : obj.optString("oldprice", null),
                    obj.optString("currencyId", "$"), obj.optString("picture", ""),
                    obj.optString("param", ""), obj.optString("url", ""), obj.optString("category", "")
            );
        }
    }

    // =========================================================
    // ADAPTER: product grid (Home / Search results)
    // =========================================================
    public static class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {
        private Context context;
        private List<Product> list;

        public ProductAdapter(Context context, List<Product> list) {
            this.context = context;
            this.list = list;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(context).inflate(R.layout.item_product, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Product p = list.get(position);
            holder.txtName.setText(p.getName());
            holder.txtPrice.setText(p.currencySymbol() + p.getPrice());

            int discount = p.getDiscountPercent();
            if (discount > 0) {
                holder.txtOldPrice.setVisibility(View.VISIBLE);
                holder.txtOldPrice.setText(p.currencySymbol() + p.getOldPrice());
                holder.txtOldPrice.setPaintFlags(holder.txtOldPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                holder.txtDiscount.setVisibility(View.VISIBLE);
                holder.txtDiscount.setText(discount + "% OFF");
            } else {
                holder.txtOldPrice.setVisibility(View.GONE);
                holder.txtDiscount.setVisibility(View.GONE);
            }

            Glide.with(context).load(p.getPicture()).placeholder(android.R.drawable.ic_menu_gallery).into(holder.img);

            holder.btnFavorite.setImageResource(CartManager.isFavorite(context, p.getId()) ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            holder.btnFavorite.setOnClickListener(v -> {
                CartManager.toggleFavorite(context, p);
                holder.btnFavorite.setImageResource(CartManager.isFavorite(context, p.getId()) ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            });

            holder.btnAddToCart.setOnClickListener(v -> {
                CartManager.addToCart(context, p);
                Toast.makeText(context, "Added to Cart! 🛒", Toast.LENGTH_SHORT).show();
            });

            holder.btnBuyNow.setOnClickListener(v -> openBuyNow(context, p));
            holder.itemView.setOnClickListener(v -> openDetail(context, p.getId()));
        }

        @Override public int getItemCount() { return list != null ? list.size() : 0; }

        static class VH extends RecyclerView.ViewHolder {
            ImageView img; ImageButton btnFavorite; TextView txtName, txtPrice, txtOldPrice, txtDiscount;
            Button btnAddToCart, btnBuyNow;
            VH(@NonNull View v) {
                super(v);
                img = v.findViewById(R.id.imgProduct);
                btnFavorite = v.findViewById(R.id.btnFavorite);
                txtName = v.findViewById(R.id.txtProductName);
                txtPrice = v.findViewById(R.id.txtProductPrice);
                txtOldPrice = v.findViewById(R.id.txtOldPrice);
                txtDiscount = v.findViewById(R.id.txtDiscountBadge);
                btnAddToCart = v.findViewById(R.id.btnAddToCart);
                btnBuyNow = v.findViewById(R.id.btnBuyNow);
            }
        }
    }

    // =========================================================
    // ADAPTER: horizontal related-products row
    // =========================================================
    public static class RelatedProductAdapter extends RecyclerView.Adapter<RelatedProductAdapter.VH> {
        private Context context;
        private List<Product> list;

        public RelatedProductAdapter(Context context, List<Product> list) {
            this.context = context;
            this.list = list;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(context).inflate(R.layout.item_related_product, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Product p = list.get(position);
            holder.name.setText(p.getName());
            holder.price.setText(p.currencySymbol() + p.getPrice());
            int discount = p.getDiscountPercent();
            if (discount > 0) {
                holder.discount.setVisibility(View.VISIBLE);
                holder.discount.setText(discount + "% OFF");
            } else {
                holder.discount.setVisibility(View.GONE);
            }
            Glide.with(context).load(p.getPicture()).placeholder(android.R.drawable.ic_menu_gallery).into(holder.img);
            holder.itemView.setOnClickListener(v -> openDetail(context, p.getId()));
        }

        @Override public int getItemCount() { return list != null ? list.size() : 0; }

        static class VH extends RecyclerView.ViewHolder {
            ImageView img; TextView name, price, discount;
            VH(@NonNull View v) {
                super(v);
                img = v.findViewById(R.id.imgRelated);
                name = v.findViewById(R.id.txtRelatedName);
                price = v.findViewById(R.id.txtRelatedPrice);
                discount = v.findViewById(R.id.txtRelatedDiscount);
            }
        }
    }

    // =========================================================
    // ADAPTER: category filter chips
    // =========================================================
    public static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {
        public interface OnCategoryClick { void onClick(String category); }

        private List<String> categories;
        private String selected;
        private OnCategoryClick listener;

        public CategoryAdapter(List<String> categories, String selected, OnCategoryClick listener) {
            this.categories = categories;
            this.selected = selected;
            this.listener = listener;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_chip, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String cat = categories.get(position);
            holder.txt.setText(cat);
            boolean isSelected = cat.equalsIgnoreCase(selected);
            holder.txt.setBackgroundResource(isSelected ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
            holder.txt.setTextColor(isSelected ? 0xFFFFFFFF : 0xFF1E293B);
            holder.txt.setOnClickListener(v -> {
                selected = cat;
                notifyDataSetChanged();
                if (listener != null) listener.onClick(cat);
            });
        }

        @Override public int getItemCount() { return categories != null ? categories.size() : 0; }

        static class VH extends RecyclerView.ViewHolder {
            TextView txt;
            VH(@NonNull View v) { super(v); txt = v.findViewById(R.id.txtChip); }
        }
    }

    // =========================================================
    // ADAPTER: Cart / Liked list rows
    // =========================================================
    public static class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {
        public interface OnCartChange { void onChanged(); }

        private Context context;
        private List<Product> items;
        private OnCartChange onCartChange;
        private boolean likedMode;

        public CartAdapter(Context context, List<Product> items, OnCartChange onCartChange, boolean likedMode) {
            this.context = context;
            this.items = items;
            this.onCartChange = onCartChange;
            this.likedMode = likedMode;
        }

        public void setLikedMode(boolean likedMode) { this.likedMode = likedMode; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Product p = items.get(position);
            holder.name.setText(p.getName());
            holder.price.setText(p.currencySymbol() + p.getPrice());

            if (p.getDiscountPercent() > 0) {
                holder.oldPrice.setVisibility(View.VISIBLE);
                holder.oldPrice.setText(p.currencySymbol() + p.getOldPrice());
                holder.oldPrice.setPaintFlags(holder.oldPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                holder.oldPrice.setVisibility(View.GONE);
            }

            Glide.with(context).load(p.getPicture()).placeholder(android.R.drawable.ic_menu_gallery).into(holder.img);

            holder.buyNow.setOnClickListener(v -> openBuyNow(context, p));
            holder.itemView.setOnClickListener(v -> openDetail(context, p.getId()));

            holder.remove.setOnClickListener(v -> {
                if (likedMode) CartManager.toggleFavorite(context, p);
                else CartManager.removeFromCart(context, p.getId());
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    items.remove(pos);
                    notifyItemRemoved(pos);
                    if (onCartChange != null) onCartChange.onChanged();
                }
            });
        }

        @Override public int getItemCount() { return items != null ? items.size() : 0; }

        static class VH extends RecyclerView.ViewHolder {
            ImageView img; TextView name, price, oldPrice; ImageButton remove; Button buyNow;
            VH(@NonNull View v) {
                super(v);
                img = v.findViewById(R.id.imgCartProduct);
                name = v.findViewById(R.id.txtCartName);
                price = v.findViewById(R.id.txtCartPrice);
                oldPrice = v.findViewById(R.id.txtCartOldPrice);
                remove = v.findViewById(R.id.btnRemoveFromCart);
                buyNow = v.findViewById(R.id.btnCartBuyNow);
            }
        }
    }

    // =========================================================
    // ADAPTER: image gallery pager (product detail screen)
    // =========================================================
    public static class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.VH> {
        private Context context;
        private List<String> images;

        public ImagePagerAdapter(Context context, List<String> images) {
            this.context = context;
            this.images = images != null ? images : new ArrayList<>();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(context);
            iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iv.setBackgroundColor(0xFFF1F5F9);
            return new VH(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String url = images.get(position);
            Glide.with(context).load(url).placeholder(android.R.drawable.ic_menu_gallery).into(holder.imageView);
            holder.imageView.setOnClickListener(v -> {
                Intent intent = new Intent(context, MainActivity.ZoomImageActivity.class);
                intent.putExtra("images", new ArrayList<>(images));
                intent.putExtra("index", position);
                context.startActivity(intent);
            });
        }

        @Override public int getItemCount() { return images.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView imageView;
            VH(@NonNull View v) { super(v); imageView = (ImageView) v; }
        }
    }

    // =========================================================
    // Simple pinch-zoom + pan + double-tap ImageView (full screen viewer)
    // =========================================================
    public static class ZoomableImageView extends AppCompatImageView {
        private final android.graphics.Matrix matrix = new android.graphics.Matrix();
        private float scale = 1f;
        private static final float MIN_SCALE = 1f, MAX_SCALE = 5f;
        private float lastTouchX, lastTouchY;
        private int activePointerId = -1;
        private android.view.ScaleGestureDetector scaleDetector;
        private android.view.GestureDetector gestureDetector;

        public ZoomableImageView(Context context) { super(context); init(context); }
        public ZoomableImageView(Context context, android.util.AttributeSet attrs) { super(context, attrs); init(context); }

        private void init(Context context) {
            setScaleType(ScaleType.MATRIX);
            scaleDetector = new android.view.ScaleGestureDetector(context, new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(android.view.ScaleGestureDetector detector) {
                    float newScale = Math.max(MIN_SCALE, Math.min(scale * detector.getScaleFactor(), MAX_SCALE));
                    float factor = newScale / scale;
                    scale = newScale;
                    matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
                    setImageMatrix(matrix);
                    return true;
                }
            });
            gestureDetector = new android.view.GestureDetector(context, new android.view.GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(android.view.MotionEvent e) {
                    if (scale > MIN_SCALE) {
                        scale = MIN_SCALE;
                        matrix.reset();
                    } else {
                        scale = 2.5f;
                        matrix.postScale(scale, scale, e.getX(), e.getY());
                    }
                    setImageMatrix(matrix);
                    return true;
                }
            });
        }

        @Override
        public boolean onTouchEvent(android.view.MotionEvent event) {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getX(); lastTouchY = event.getY();
                    activePointerId = event.getPointerId(0);
                    break;
                case android.view.MotionEvent.ACTION_MOVE:
                    if (scale > MIN_SCALE) {
                        int idx = event.findPointerIndex(activePointerId);
                        if (idx != -1) {
                            float x = event.getX(idx), y = event.getY(idx);
                            if (!scaleDetector.isInProgress()) {
                                matrix.postTranslate(x - lastTouchX, y - lastTouchY);
                                setImageMatrix(matrix);
                            }
                            lastTouchX = x; lastTouchY = y;
                        }
                    }
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    activePointerId = -1;
                    break;
            }
            return true;
        }

        public void resetZoom() {
            scale = 1f;
            matrix.reset();
            setImageMatrix(matrix);
        }
    }

    // =========================================================
    // Shared navigation helpers
    // =========================================================
    public static void openDetail(Context context, String productId) {
        Intent intent = new Intent(context, MainActivity.ProductDetailActivity.class);
        intent.putExtra("id", productId);
        context.startActivity(intent);
    }

    public static void openBuyNow(Context context, Product p) {
        String url = (p.getUrl() != null && !p.getUrl().trim().isEmpty())
                ? p.getUrl()
                : API_BASE.replace("api.php", "") + "go?id=" + p.getId();
        Intent intent = new Intent(context, MainActivity.WebViewActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("title", p.getName());
        context.startActivity(intent);
    }
}
