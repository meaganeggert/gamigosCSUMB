package com.example.gamigosjava.ui.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.Image;
import com.example.gamigosjava.data.model.MatchSummary;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder>{
    private final List<Image> images = new ArrayList<>();
    FirebaseFirestore db = FirebaseFirestore.getInstance();


    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
//        TextView title, players, playtime;
//        Button deleteMatchButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image_eventPhoto);
//            title = itemView.findViewById(R.id.textTitle);
//            players = itemView.findViewById(R.id.textPlayers);
//            playtime = itemView.findViewById(R.id.textPlaytime);
//            deleteMatchButton = itemView.findViewById(R.id.button_deleteMatch);
        }
    }

    @NonNull
    @Override
    public ImageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.image_item, parent, false);
        return new ImageAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageAdapter.ViewHolder holder, int position) {
        Image image = images.get(position);
        android.util.Log.d("ImageAdapter", "Binding: " + image.imageUrl);

        if (image.imageUrl != null && !image.imageUrl.isEmpty()) {
            Picasso.get()
                    .load(image.imageUrl)
                    .placeholder(R.drawable.ic_launcher_background) // optional placeholder
                    .error(R.drawable.ic_launcher_foreground)       // fallback if broken
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.ic_launcher_background);
        }
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public void setItems(List<Image> newImages) {
        images.clear();
        if (newImages != null) {
            images.addAll(newImages);
        }
        notifyDataSetChanged(); // tells RecyclerView to refresh
    }
}
