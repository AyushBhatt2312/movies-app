package com.bhatt.movies.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bhatt.movies.Activity.DetailActivity;
import com.bhatt.movies.Domain.ListFilm;
import com.bhatt.movies.Domain.Datum;
import com.bhatt.movies.R;
import com.bumptech.glide.Glide;

public class FilmListAdapter extends RecyclerView.Adapter<FilmListAdapter.ViewHolder> {
    ListFilm items;
    Context context;

    public FilmListAdapter(ListFilm items){
        this.items = items;
    }

    @NonNull
    @Override
    public FilmListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_film,parent,false);
        context = parent.getContext();
        return new ViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull FilmListAdapter.ViewHolder holder, int position) {
        Datum item = items.getData().get(position);
        holder.titleTxt.setText(item.getTitle());
        holder.ScoreTxt.setText(String.valueOf(item.getImdbRating()));
        
        Glide.with(holder.itemView.getContext())
             .load(item.getPoster())
             .placeholder(R.drawable.placeholder_poster)
             .error(R.drawable.error_poster)
             .into(holder.pic);

        holder.itemView.setOnClickListener(v -> {
            if (item.getId() > 0) {
                Intent intent = new Intent(context, DetailActivity.class);
                intent.putExtra("id", item.getId());
                context.startActivity(intent);
            } else {
                android.widget.Toast.makeText(context, "Invalid movie ID", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null && items.getData() != null ? items.getData().size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTxt,ScoreTxt;
        ImageView pic;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTxt = itemView.findViewById(R.id.titleTxt);
            ScoreTxt = itemView.findViewById(R.id.scoreTxt);
            pic = itemView.findViewById(R.id.pic);
        }
    }
}
