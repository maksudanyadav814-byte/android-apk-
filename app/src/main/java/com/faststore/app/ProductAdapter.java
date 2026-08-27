package com.faststore.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private Context context;
    private List<Product> productList;

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

        String currency = (product.getCurrencyId() != null && product.getCurrencyId().equalsIgnoreCase("INR")) ? "₹" : "$";
        holder.txtPrice.setText(currency + product.getPrice());

        Glide.with(context)
                .load(product.getPicture())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.imgThumbnail);

        holder.btnFavorite.setImageResource(
                CartManager.isFavorite(context, product.getId())
                        ? R.drawable.ic_heart_filled
                        : R.drawable.ic_heart_outline
        );

        holder.btnFavorite.setOnClickListener(v -> {
            CartManager.toggleFavorite(context, product.getId());
            holder.btnFavorite.setImageResource(
                    CartManager.isFavorite(context, product.getId())
                            ? R.drawable.ic_heart_filled
                            : R.drawable.ic_heart_outline
            );
        });

        holder.btnAddToCart.setOnClickListener(v -> {
            CartManager.addToCart(context, product);
            Toast.makeText(context, "Added to Cart! 🛒", Toast.LENGTH_SHORT).show();
        });

        holder.btnBuyNow.setOnClickListener(v -> openBuyNow(product));

        holder.itemView.setOnClickListener(v -> openProductDetail(product));
    }

    private void openProductDetail(Product product) {
        Intent intent = new Intent(context, ProductDetailActivity.class);
        intent.putExtra("product", product);
        context.startActivity(intent);
    }

    private void openBuyNow(Product product) {
        String url = "https://powderblue-sparrow-788374.hostingersite.com/android/go?id=" + product.getId();
        Intent intent = new Intent(context, WebViewActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("title", product.getName());
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail;
        ImageButton btnFavorite;
        TextView txtName, txtPrice;
        Button btnAddToCart, btnBuyNow;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.imgProduct);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            txtName = itemView.findViewById(R.id.txtProductName);
            txtPrice = itemView.findViewById(R.id.txtProductPrice);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
            btnBuyNow = itemView.findViewById(R.id.btnBuyNow);
        }
    }
}
