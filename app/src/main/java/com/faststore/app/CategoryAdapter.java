package com.faststore.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

    public interface OnCategoryClick {
        void onClick(String category);
    }

    private List<String> categories;
    private String selected;
    private OnCategoryClick listener;

    public CategoryAdapter(List<String> categories, String selected, OnCategoryClick listener) {
        this.categories = categories;
        this.selected = selected;
        this.listener = listener;
    }

    public void updateSelected(String newSelected) {
        this.selected = newSelected;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_chip, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String cat = categories.get(position);
        holder.txt.setText(cat);

        boolean isSelected = cat.equalsIgnoreCase(selected);
        holder.txt.setBackgroundResource(isSelected ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        holder.txt.setTextColor(isSelected ? 0xFFFFFFFF : 0xFF1E293B);

        holder.txt.setOnClickListener(v -> {
            String old = selected;
            selected = cat;
            notifyDataSetChanged();
            if (listener != null) listener.onClick(cat);
        });
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    public static class VH extends RecyclerView.ViewHolder {
        TextView txt;

        public VH(@NonNull View itemView) {
            super(itemView);
            txt = itemView.findViewById(R.id.txtChip);
        }
    }
}
