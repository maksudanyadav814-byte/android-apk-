package com.faststore.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class CartFragment extends Fragment {

    private RecyclerView cartRecycler;
    private TextView txtEmpty;
    private TabLayout tabLayout;
    private CartAdapter adapter;
    private final List<Product> items = new ArrayList<>();

    private boolean showingLiked = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cartRecycler = view.findViewById(R.id.cartRecycler);
        txtEmpty = view.findViewById(R.id.txtCartEmpty);
        tabLayout = view.findViewById(R.id.cartTabLayout);

        adapter = new CartAdapter(requireContext(), items, this::refreshEmptyState, showingLiked);
        cartRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        cartRecycler.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showingLiked = tab.getPosition() == 1;
                adapter.setLikedMode(showingLiked);
                loadItems();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadItems();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadItems();
    }

    private void loadItems() {
        items.clear();
        if (showingLiked) {
            items.addAll(CartManager.getFavoriteItems(requireContext()));
        } else {
            items.addAll(CartManager.getCartItems(requireContext()));
        }
        adapter.notifyDataSetChanged();
        refreshEmptyState();
    }

    private void refreshEmptyState() {
        if (items.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            txtEmpty.setText(showingLiked
                    ? "No liked products yet ❤️\nTap the heart icon on any product!"
                    : "Your cart is empty 🛍️\nGo add some cool stuff!");
        } else {
            txtEmpty.setVisibility(View.GONE);
        }
    }
}
