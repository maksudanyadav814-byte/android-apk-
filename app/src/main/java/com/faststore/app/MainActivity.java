package com.faststore.app;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> productList;
    private ProgressBar progressBar;

    private RequestQueue requestQueue;

    // Live Hostinger Server API Endpoint
    private static final String API_URL = "https://powderblue-sparrow-788374.hostingersite.com/android/api.php?action=products";
    private int currentPage = 1;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);

        productList = new ArrayList<>();
        adapter = new ProductAdapter(this, productList);

        // Native 2-Column Grid Layout Setup
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        requestQueue = Volley.newRequestQueue(this);

        setupAutoLoadMore(layoutManager);
        loadProducts(); // Load initial page data
    }

    private void setupAutoLoadMore(GridLayoutManager layoutManager) {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) { // Scroll down check
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 2) {
                            loadProducts(); // Trigger next page load automatically
                        }
                    }
                }
            }
        });
    }

    private void loadProducts() {
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);

        String url = API_URL + "&page=" + currentPage;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.has("products")) {
                            JSONArray jsonArray = response.getJSONArray("products");

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject obj = jsonArray.getJSONObject(i);
                                productList.add(new Product(
                                        obj.getString("id"),
                                        obj.getString("name"),
                                        obj.optString("price", "0.00"),
                                        obj.optString("picture", ""),
                                        obj.optString("currencyId", "$")
                                ));
                            }

                            adapter.notifyDataSetChanged();
                            currentPage++; // Increment page for next load
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                },
                error -> {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Failed to load live data", Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }
}
