package com.faststore.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ProductDetailActivity extends AppCompatActivity {

    private Product product;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        product = (Product) getIntent().getSerializableExtra("product");
        if (product == null) {
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbarDetail);
        toolbar.setNavigationOnClickListener(v -> finish());

        ImageView img = findViewById(R.id.imgDetail);
        TextView txtName = findViewById(R.id.txtDetailName);
        TextView txtCategory = findViewById(R.id.txtDetailCategory);
        TextView txtPrice = findViewById(R.id.txtDetailPrice);
        TextView txtDescription = findViewById(R.id.txtDetailDescription);
        ImageButton btnFavorite = findViewById(R.id.btnDetailFavorite);
        android.widget.Button btnAddToCart = findViewById(R.id.btnDetailAddToCart);
        android.widget.Button btnBuyNow = findViewById(R.id.btnDetailBuyNow);
        RecyclerView relatedRecycler = findViewById(R.id.relatedRecycler);

        Glide.with(this).load(product.getPicture()).placeholder(android.R.drawable.ic_menu_gallery).into(img);
        txtName.setText(product.getName());
        txtCategory.setText("Category: " + product.getCategory());

        String currency = (product.getCurrencyId() != null && product.getCurrencyId().equalsIgnoreCase("INR")) ? "₹" : "$";
        txtPrice.setText(currency + product.getPrice());

        String desc = product.getDescription();
        txtDescription.setText(desc == null || desc.trim().isEmpty()
                ? "No description available for this product yet."
                : desc);

        updateFavoriteIcon(btnFavorite);
        btnFavorite.setOnClickListener(v -> {
            CartManager.toggleFavorite(this, product.getId());
            updateFavoriteIcon(btnFavorite);
        });

        btnAddToCart.setOnClickListener(v -> {
            CartManager.addToCart(this, product);
            Toast.makeText(this, "Added to Cart! 🛒", Toast.LENGTH_SHORT).show();
        });

        btnBuyNow.setOnClickListener(v -> {
            String url = "https://powderblue-sparrow-788374.hostingersite.com/android/go?id=" + product.getId();
            Intent intent = new Intent(this, WebViewActivity.class);
            intent.putExtra("url", url);
            intent.putExtra("title", product.getName());
            startActivity(intent);
        });

        List<Product> related = DataCache.getRelated(product, 10);
        relatedRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        relatedRecycler.setAdapter(new RelatedProductAdapter(this, related));
    }

    private void updateFavoriteIcon(ImageButton btnFavorite) {
        btnFavorite.setImageResource(
                CartManager.isFavorite(this, product.getId())
                        ? R.drawable.ic_heart_filled
                        : R.drawable.ic_heart_outline
        );
    }
}
