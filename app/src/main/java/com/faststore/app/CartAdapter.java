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
    private boolean likedMode;

    public CartAdapter(Context context, List<Product> items, OnCartChange onCartChange, boolean likedMode) {
        this.context = context;
        this.items = items;
        this.onCartChange = onCartChange;
        this.likedMode = likedMode;
    }

    public void setLikedMode(boolean likedMode) {
        this.likedMode = likedMode;
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

        if (p.getDiscountPercent() > 0 && p.getOldPrice() != null) {
            holder.oldPrice.setVisibility(View.VISIBLE);
            holder.oldPrice.setText(currency + p.getOldPrice());
            holder.oldPrice.setPaintFlags(holder.oldPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.oldPrice.setVisibility(View.GONE);
        }

        holder.buyNow.setOnClickListener(v -> {
            String url = "https://powderblue-sparrow-788374.hostingersite.com/android/go?id=" + p.getId();
            Intent intent = new Intent(context, WebViewActivity.class);
            intent.putExtra("url", url);
            intent.putExtra("title", p.getName());
            context.startActivity(intent);
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("product", p);
            context.startActivity(intent);
        });

        holder.remove.setOnClickListener(v -> {
            if (likedMode) {
                CartManager.toggleFavorite(context, p);
            } else {
                CartManager.removeFromCart(context, p.getId());
            }
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
        TextView name, price, oldPrice;
        ImageButton remove;
        android.widget.Button buyNow;

        public VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgCartProduct);
            name = itemView.findViewById(R.id.txtCartName);
            price = itemView.findViewById(R.id.txtCartPrice);
            oldPrice = itemView.findViewById(R.id.txtCartOldPrice);
            remove = itemView.findViewById(R.id.btnRemoveFromCart);
            buyNow = itemView.findViewById(R.id.btnCartBuyNow);
        }
    }
}
