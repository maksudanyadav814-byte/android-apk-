package com.faststore.app;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class ZoomImageActivity extends AppCompatActivity {

    private List<String> images = new ArrayList<>();
    private int currentIndex = 0;
    private ZoomableImageView imageView;
    private TextView counter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zoom_image);

        images = (List<String>) getIntent().getSerializableExtra("images");
        currentIndex = getIntent().getIntExtra("index", 0);
        if (images == null) images = new ArrayList<>();

        imageView = findViewById(R.id.zoomImageView);
        counter = findViewById(R.id.txtZoomCounter);
        ImageButton btnClose = findViewById(R.id.btnCloseZoom);
        ImageButton btnPrev = findViewById(R.id.btnZoomPrev);
        ImageButton btnNext = findViewById(R.id.btnZoomNext);

        btnClose.setOnClickListener(v -> finish());
        btnPrev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                loadImage();
            }
        });
        btnNext.setOnClickListener(v -> {
            if (currentIndex < images.size() - 1) {
                currentIndex++;
                loadImage();
            }
        });

        if (images.size() <= 1) {
            btnPrev.setVisibility(View.GONE);
            btnNext.setVisibility(View.GONE);
        }

        loadImage();
    }

    private void loadImage() {
        imageView.resetZoom();
        if (!images.isEmpty()) {
            Glide.with(this).load(images.get(currentIndex)).into(imageView);
            counter.setText((currentIndex + 1) + " / " + images.size());
            counter.setVisibility(images.size() > 1 ? View.VISIBLE : View.GONE);
        }
    }
}
