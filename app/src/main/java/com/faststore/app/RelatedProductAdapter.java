package com.faststore.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class RelatedProductAdapter extends RecyclerView.Adapter<RelatedProductAdapter.VH> {

    private Context context;
    private List<Product> items;

    public RelatedProductAdapter(Context context, List<Product> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_related_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Product p = items.get(position);
        holder.name.setText(p.getName());
        String currency = (p.getCurrencyId() != null && p.getCurrencyId().equalsIgnoreCase("INR")) ? "₹" : "$";
        holder.price.setText(currency + p.getPrice());
        Glide.with(context).load(p.getPicture()).placeholder(android.R.drawable.ic_menu_gallery).into(holder.img);

        int discount = p.getDiscountPercent();
        if (discount > 0) {
            holder.discount.setVisibility(View.VISIBLE);
            holder.discount.setText(discount + "% OFF");
        } else {
            holder.discount.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("product", p);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView name, price, discount;

        public VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgRelated);
            name = itemView.findViewById(R.id.txtRelatedName);
            price = itemView.findViewById(R.id.txtRelatedPrice);
            discount = itemView.findViewById(R.id.txtRelatedDiscount);
        }
    }
}
