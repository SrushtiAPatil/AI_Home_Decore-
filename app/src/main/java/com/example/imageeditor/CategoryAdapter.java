package com.example.imageeditor;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private Context context;
    private List<ObjectCategory> categories;
    private OnCategoryClickListener listener;
    private int selectedPosition = 0;

    // Category colors for visual distinction
    private final int[] categoryColors = {
            0xFF6200EE, // Hall - Purple
            0xFFFF6B6B, // Kitchen - Red
            0xFF03DAC5, // Bathroom - Teal
            0xFFFFA726, // Office - Orange
            0xFF9C27B0, // Class - Deep Purple
            0xFF4CAF50  // Ground - Green
    };

    public interface OnCategoryClickListener {
        void onCategoryClick(ObjectCategory category, int position);
    }

    public CategoryAdapter(Context context, List<ObjectCategory> categories, OnCategoryClickListener listener) {
        this.context = context;
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        ObjectCategory category = categories.get(position);

        holder.tvCategoryName.setText(category.getName());
        holder.ivCategoryIcon.setImageResource(category.getIconResId());

        // Get category color
        int categoryColor = categoryColors[position % categoryColors.length];

        View selectionGlow = holder.itemView.findViewById(R.id.selectionGlow);

        // Apply selection state with smooth animation
        if (selectedPosition == position) {
            // Selected state - vibrant and elevated
            animateCardSelection(holder.cardView, true);
            holder.cardView.setCardElevation(16f);

            // Show glow effect
            if (selectionGlow != null) {
                selectionGlow.setVisibility(View.VISIBLE);
                selectionGlow.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start();
            }

            // Colorize icon
            holder.ivCategoryIcon.setColorFilter(categoryColor);
            holder.tvCategoryName.setTextColor(categoryColor);

            // Scale animation
            holder.itemView.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(200)
                    .start();

        } else {
            // Unselected state - subtle
            animateCardSelection(holder.cardView, false);
            holder.cardView.setCardElevation(6f);

            // Hide glow effect
            if (selectionGlow != null) {
                selectionGlow.setVisibility(View.INVISIBLE);
            }

            // Gray out icon
            holder.ivCategoryIcon.setColorFilter(0xFF999999);
            holder.tvCategoryName.setTextColor(0xFF666666);

            // Reset scale
            holder.itemView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start();
        }

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // Haptic feedback
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);

            // Ripple effect animation
            animateRipple(holder.itemView);

            // Update both items
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);

            // Callback
            listener.onCategoryClick(category, selectedPosition);
        });
    }

    private void animateCardSelection(CardView card, boolean selected) {
        int fromColor = selected ? 0xFFFFFFFF : card.getCardBackgroundColor().getDefaultColor();
        int toColor = selected ? 0xFFFFFFFF : 0xFFF8F9FA;

        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
        colorAnimation.setDuration(300);
        colorAnimation.addUpdateListener(animator -> card.setCardBackgroundColor((int) animator.getAnimatedValue()));
        colorAnimation.start();
    }

    private void animateRipple(View view) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1.05f)
                            .scaleY(1.05f)
                            .setDuration(100)
                            .setInterpolator(new android.view.animation.BounceInterpolator())
                            .start();
                })
                .start();
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivCategoryIcon;
        TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}