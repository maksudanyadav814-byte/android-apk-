package com.faststore.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private Context context;
    private List<Product> productList;

    // Direct Hostinger Affiliate Link Redirection URL
    private static final String REDIRECT_URL_BASE = "https://powderblue-sparrow-788374.hostingersite.com/android/go?id=";

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.txtName.setText(product.getName());

        // Default currency display (no conversion applied)
        String currency = (product.getCurrencyId() != null && product.getCurrencyId().equalsIgnoreCase("INR")) ? "₹" : "$";
        holder.txtPrice.setText(currency + product.getPrice());

        // Load image using Glide library
        Glide.with(context)
                .load(product.getPicture())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.imgThumbnail);

        // Save item to local device storage (Cart without login system)
        holder.btnAddToCart.setOnClickListener(v -> {
            SharedPreferences prefs = context.getSharedPreferences("LocalCart", Context.MODE_PRIVATE);
            String cartStr = prefs.getString("cart_items", "[]");
            try {
                JSONArray cartArr = new JSONArray(cartStr);
                cartArr.put(product.getId());
                prefs.edit().putString("cart_items", cartArr.toString()).apply();
                Toast.makeText(context, "Added to Cart!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Open external browser for affiliate link redirection
        holder.btnBuyNow.setOnClickListener(v -> {
            String finalUrl = REDIRECT_URL_BASE + Uri.encode(product.getId());
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
            browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(browserIntent);
        });
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail;
        TextView txtName, txtPrice;
        Button btnAddToCart, btnBuyNow;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.imgProduct);
            txtName = itemView.findViewById(R.id.txtProductName);
            txtPrice = itemView.findViewById(R.id.txtProductPrice);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
            btnBuyNow = itemView.findViewById(R.id.btnBuyNow);
        }
    }
}
