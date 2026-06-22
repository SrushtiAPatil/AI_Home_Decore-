package com.example.imageeditor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ObjectAdapter extends RecyclerView.Adapter<ObjectAdapter.ObjectViewHolder> {

    private Context context;
    private List<Integer> objectList;
    private OnObjectClickListener listener;
    private int selectedPosition = -1;

    public interface OnObjectClickListener {
        void onObjectClick(int objectResId);
    }

    public ObjectAdapter(Context context, List<Integer> objectList, OnObjectClickListener listener) {
        this.context = context;
        this.objectList = objectList;
        this.listener = listener;
    }

    public void updateObjects(List<Integer> newObjects) {
        this.objectList = newObjects;
        this.selectedPosition = -1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ObjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_object, parent, false);
        return new ObjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ObjectViewHolder holder, int position) {
        if (objectList == null || position >= objectList.size()) {
            return;
        }

        int objectResId = objectList.get(position);
        holder.imageView.setImageResource(objectResId);

        View selectionIndicator = holder.itemView.findViewById(R.id.selectionIndicator);
        View checkmark = holder.itemView.findViewById(R.id.ivCheck);

        // Highlight selected item with animation
        if (selectedPosition == position) {
            holder.cardView.setCardElevation(16f);
            if (selectionIndicator != null) {
                selectionIndicator.setVisibility(View.VISIBLE);
            }
            if (checkmark != null) {
                checkmark.setVisibility(View.VISIBLE);
            }

            // Pulse animation
            holder.imageView.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(200)
                    .start();
        } else {
            holder.cardView.setCardElevation(8f);
            if (selectionIndicator != null) {
                selectionIndicator.setVisibility(View.INVISIBLE);
            }
            if (checkmark != null) {
                checkmark.setVisibility(View.INVISIBLE);
            }

            holder.imageView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start();
        }

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // Animate selection with bounce
            holder.cardView.animate()
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        holder.cardView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .setInterpolator(new android.view.animation.BounceInterpolator())
                                .start();
                    })
                    .start();

            // Notify changes for smooth animation
            if (previousPosition != -1) {
                notifyItemChanged(previousPosition);
            }
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onObjectClick(objectResId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return objectList != null ? objectList.size() : 0;
    }

    static class ObjectViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView imageView;

        public ObjectViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            imageView = itemView.findViewById(R.id.ivObject);
        }
    }
}