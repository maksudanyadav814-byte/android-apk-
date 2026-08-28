package com.faststore.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecyclerView categoryRecycler;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;

    private ProductAdapter adapter;
    private CategoryAdapter categoryAdapter;

    private final List<Product> allLoaded = new ArrayList<>();
    private final List<Product> filteredList = new ArrayList<>();

    private RequestQueue requestQueue;

    // Live Hostinger Server API Endpoint
    private static final String API_URL = "https://powderblue-sparrow-788374.hostingersite.com/android/api.php?action=products";
    private int currentPage = 1;
    private boolean isLoading = false;
    private String selectedCategory = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerView);
        categoryRecycler = view.findViewById(R.id.categoryRecycler);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        progressBar = view.findViewById(R.id.progressBar);

        adapter = new ProductAdapter(requireContext(), filteredList);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        categoryAdapter = new CategoryAdapter(new ArrayList<>(java.util.Collections.singletonList("All")), selectedCategory, category -> {
            selectedCategory = category;
            applyCategoryFilter();
        });
        categoryRecycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        categoryRecycler.setAdapter(categoryAdapter);

        requestQueue = Volley.newRequestQueue(requireContext());

        // SwipeRefreshLayout only triggers when the RecyclerView is scrolled to the top
        // (androidx handles this automatically via canChildScrollUp()).
        swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            allLoaded.clear();
            DataCache.clear();
            loadProducts(true);
        });

        setupAutoLoadMore(layoutManager);
        loadProducts(false);
    }

    private void setupAutoLoadMore(GridLayoutManager layoutManager) {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && selectedCategory.equals("All")) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 2) {
                            loadProducts(false);
                        }
                    }
                }
            }
        });
    }

    private void loadProducts(boolean isRefresh) {
        isLoading = true;
        if (!isRefresh) progressBar.setVisibility(View.VISIBLE);

        String url = API_URL + "&page=" + currentPage;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.has("products")) {
                            JSONArray jsonArray = response.getJSONArray("products");
                            allLoaded.addAll(ProductParser.parseList(jsonArray));

                            DataCache.addAll(allLoaded);
                            categoryAdapter = new CategoryAdapter(DataCache.getCategories(), selectedCategory, category -> {
                                selectedCategory = category;
                                applyCategoryFilter();
                            });
                            categoryRecycler.setAdapter(categoryAdapter);

                            applyCategoryFilter();
                            currentPage++;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                },
                error -> {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load live data", Toast.LENGTH_SHORT).show();
                    }
                });

        requestQueue.add(request);
    }

    private void applyCategoryFilter() {
        filteredList.clear();
        if (selectedCategory.equals("All")) {
            filteredList.addAll(allLoaded);
        } else {
            for (Product p : allLoaded) {
                if (p.getCategory().equalsIgnoreCase(selectedCategory)) filteredList.add(p);
            }
        }
        adapter.notifyDataSetChanged();
    }
}
