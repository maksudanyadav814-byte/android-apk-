package com.faststore.app;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

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

        ViewPager2 imagePager = findViewById(R.id.imagePager);
        LinearLayout dotsIndicator = findViewById(R.id.dotsIndicator);
        TextView txtName = findViewById(R.id.txtDetailName);
        TextView txtCategory = findViewById(R.id.txtDetailCategory);
        TextView txtPrice = findViewById(R.id.txtDetailPrice);
        TextView txtOldPrice = findViewById(R.id.txtDetailOldPrice);
        TextView txtDiscount = findViewById(R.id.txtDetailDiscount);
        TextView txtDescription = findViewById(R.id.txtDetailDescription);
        ImageButton btnFavorite = findViewById(R.id.btnDetailFavorite);
        android.widget.Button btnAddToCart = findViewById(R.id.btnDetailAddToCart);
        android.widget.Button btnBuyNow = findViewById(R.id.btnDetailBuyNow);
        RecyclerView relatedRecycler = findViewById(R.id.relatedRecycler);

        // Image gallery
        List<String> images = product.getImages();
        imagePager.setAdapter(new ImagePagerAdapter(this, images));
        setupDots(dotsIndicator, images.size(), 0);
        imagePager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setupDots(dotsIndicator, images.size(), position);
            }
        });

        txtName.setText(product.getName());
        txtCategory.setText("Category: " + product.getCategory());

        String currency = (product.getCurrencyId() != null && product.getCurrencyId().equalsIgnoreCase("INR")) ? "₹" : "$";
        txtPrice.setText(currency + product.getPrice());

        int discount = product.getDiscountPercent();
        if (discount > 0 && product.getOldPrice() != null) {
            txtOldPrice.setVisibility(android.view.View.VISIBLE);
            txtOldPrice.setText(currency + product.getOldPrice());
            txtOldPrice.setPaintFlags(txtOldPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);

            txtDiscount.setVisibility(android.view.View.VISIBLE);
            txtDiscount.setText(discount + "% OFF");
        } else {
            txtOldPrice.setVisibility(android.view.View.GONE);
            txtDiscount.setVisibility(android.view.View.GONE);
        }

        String desc = product.getDescription();
        txtDescription.setText(desc == null || desc.trim().isEmpty()
                ? "No description available for this product yet."
                : desc);

        updateFavoriteIcon(btnFavorite);
        btnFavorite.setOnClickListener(v -> {
            CartManager.toggleFavorite(this, product);
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

    private void setupDots(LinearLayout dotsIndicator, int count, int selected) {
        if (count <= 1) {
            dotsIndicator.setVisibility(android.view.View.GONE);
            return;
        }
        dotsIndicator.setVisibility(android.view.View.VISIBLE);
        dotsIndicator.removeAllViews();
        for (int i = 0; i < count; i++) {
            android.widget.ImageView dot = new android.widget.ImageView(this);
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setSize(16, 16);
            shape.setColor(i == selected ? 0xFFFF6B35 : 0x55FFFFFF);
            dot.setImageDrawable(shape);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(16, 16);
            params.setMargins(4, 0, 4, 0);
            dot.setLayoutParams(params);
            dotsIndicator.addView(dot);
        }
    }

    private void updateFavoriteIcon(ImageButton btnFavorite) {
        btnFavorite.setImageResource(
                CartManager.isFavorite(this, product.getId())
                        ? R.drawable.ic_heart_filled
                        : R.drawable.ic_heart_outline
        );
    }
}
