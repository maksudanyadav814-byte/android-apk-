package com.faststore.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {

    public interface OnCartChange {
        void onChanged();
    }

    private Context context;
    private List<Product> items;
    private OnCartChange onCartChange;

    public CartAdapter(Context context, List<Product> items, OnCartChange onCartChange) {
        this.context = context;
        this.items = items;
        this.onCartChange = onCartChange;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Product p = items.get(position);
        holder.name.setText(p.getName());
        String currency = (p.getCurrencyId() != null && p.getCurrencyId().equalsIgnoreCase("INR")) ? "₹" : "$";
        holder.price.setText(currency + p.getPrice());
        Glide.with(context).load(p.getPicture()).placeholder(android.R.drawable.ic_menu_gallery).into(holder.img);

        holder.buyNow.setOnClickListener(v -> {
            String url = "https://powderblue-sparrow-788374.hostingersite.com/android/go?id=" + p.getId();
            Intent intent = new Intent(context, WebViewActivity.class);
            intent.putExtra("url", url);
            intent.putExtra("title", p.getName());
            context.startActivity(intent);
        });

        holder.remove.setOnClickListener(v -> {
            CartManager.removeFromCart(context, p.getId());
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                items.remove(pos);
                notifyItemRemoved(pos);
                if (onCartChange != null) onCartChange.onChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView name, price;
        ImageButton remove;
        android.widget.Button buyNow;

        public VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgCartProduct);
            name = itemView.findViewById(R.id.txtCartName);
            price = itemView.findViewById(R.id.txtCartPrice);
            remove = itemView.findViewById(R.id.btnRemoveFromCart);
            buyNow = itemView.findViewById(R.id.btnCartBuyNow);
        }
    }
}
