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

import java.util.ArrayList;
import java.util.List;

public class CartFragment extends Fragment {

    private RecyclerView cartRecycler;
    private TextView txtEmpty;
    private CartAdapter adapter;
    private final List<Product> cartItems = new ArrayList<>();

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

        adapter = new CartAdapter(requireContext(), cartItems, this::refreshEmptyState);
        cartRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        cartRecycler.setAdapter(adapter);

        loadCart();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCart();
    }

    private void loadCart() {
        cartItems.clear();
        cartItems.addAll(CartManager.getCartItems(requireContext()));
        adapter.notifyDataSetChanged();
        refreshEmptyState();
    }

    private void refreshEmptyState() {
        txtEmpty.setVisibility(cartItems.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
