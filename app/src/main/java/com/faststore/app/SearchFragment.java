package com.faststore.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private EditText editSearch;
    private RecyclerView searchRecycler;
    private TextView txtEmpty;
    private ProductAdapter adapter;
    private final List<Product> results = new ArrayList<>();

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

        adapter = new ProductAdapter(requireContext(), results);
        searchRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        searchRecycler.setAdapter(adapter);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch(String query) {
        results.clear();
        if (query == null || query.trim().isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            txtEmpty.setText("Type something to find great deals ✨");
        } else {
            String q = query.trim().toLowerCase();
            for (Product p : DataCache.allProducts) {
                if (p.getName().toLowerCase().contains(q) || p.getCategory().toLowerCase().contains(q)) {
                    results.add(p);
                }
            }
            if (results.isEmpty()) {
                txtEmpty.setVisibility(View.VISIBLE);
                txtEmpty.setText("No products found for \"" + query + "\" 😕");
            } else {
                txtEmpty.setVisibility(View.GONE);
            }
        }
        adapter.notifyDataSetChanged();
    }
}
