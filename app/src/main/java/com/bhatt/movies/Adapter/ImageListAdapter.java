package com.bhatt.movies.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bhatt.movies.R;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.ImageViewHolder> {

    private static final String TAG = "ImageListAdapter";
    private final List<String> imageUrls;
    private final Context context;

    public ImageListAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.viewholder_detail_images, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        try {
            String imageUrl = imageUrls.get(position);
            if (imageUrl != null && !imageUrl.isEmpty() && imageUrl.startsWith("http")) {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_poster)
                        .error(R.drawable.error_poster)
                        .into(holder.imageView);
            } else {
                Log.w(TAG, "Invalid image URL at position " + position + ": " + imageUrl);
                holder.imageView.setImageResource(R.drawable.error_poster);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading image at position " + position + ": " + e.getMessage());
            holder.imageView.setImageResource(R.drawable.error_poster);
        }
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.itemImages);
            if (imageView == null) {
                throw new IllegalStateException("Could not find ImageView with ID itemImages");
            }
        }
    }
}