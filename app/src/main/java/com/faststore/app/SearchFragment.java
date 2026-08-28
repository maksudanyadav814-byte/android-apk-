package com.faststore.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    // Live Hostinger Server API Endpoint - server-side search action.
    // If your api.php does not yet support ?action=search&q=..., add it there;
    // this screen automatically falls back to searching already-loaded products otherwise.
    private static final String SEARCH_API_URL = "https://powderblue-sparrow-788374.hostingersite.com/android/api.php?action=search";

    private EditText editSearch;
    private RecyclerView searchRecycler;
    private TextView txtEmpty;
    private ProgressBar progressBar;
    private ProductAdapter adapter;
    private final List<Product> results = new ArrayList<>();
    private RequestQueue requestQueue;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editSearch = view.findViewById(R.id.editSearch);
        searchRecycler = view.findViewById(R.id.searchRecycler);
        txtEmpty = view.findViewById(R.id.txtSearchEmpty);
        progressBar = view.findViewById(R.id.searchProgress);

        requestQueue = Volley.newRequestQueue(requireContext());

        adapter = new ProductAdapter(requireContext(), results);
        searchRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        searchRecycler.setAdapter(adapter);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                debounceHandler.removeCallbacks(pendingSearch != null ? pendingSearch : () -> {});
                String query = s.toString();
                pendingSearch = () -> performSearch(query);
                debounceHandler.postDelayed(pendingSearch, 350);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            results.clear();
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
            txtEmpty.setVisibility(View.VISIBLE);
            txtEmpty.setText("Type something to find great deals ✨");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        txtEmpty.setVisibility(View.GONE);

        String encoded;
        try {
            encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            encoded = query.trim();
        }
        String url = SEARCH_API_URL + "&q=" + encoded;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        results.clear();
                        if (response.has("products")) {
                            results.addAll(ProductParser.parseList(response.getJSONArray("products")));
                        }
                        showResults(query);
                    } catch (Exception e) {
                        fallbackLocalSearch(query);
                    }
                },
                error -> fallbackLocalSearch(query));

        requestQueue.add(request);
    }

    /** Used if the server search endpoint isn't available yet - filters already-loaded products locally. */
    private void fallbackLocalSearch(String query) {
        results.clear();
        String q = query.trim().toLowerCase();
        for (Product p : DataCache.allProducts) {
            if (p.getName().toLowerCase().contains(q) || p.getCategory().toLowerCase().contains(q)) {
                results.add(p);
            }
        }
        showResults(query);
    }

    private void showResults(String query) {
        progressBar.setVisibility(View.GONE);
        if (results.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            txtEmpty.setText("No products found for \"" + query + "\" 😕");
        } else {
            txtEmpty.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }
}
