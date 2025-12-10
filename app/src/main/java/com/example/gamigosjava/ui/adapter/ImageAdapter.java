package com.example.gamigosjava.ui.adapter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.Image;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder>{
    private static final String TAG = "Image Adapter";
    private final List<Image> images = new ArrayList<>();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    boolean canEdit = false;


    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image_eventPhoto);
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
        int itemPostiion = position;
        Image image = images.get(position);
        android.util.Log.d(TAG, "Binding: " + image.imageUrl);

        if (image.imageUrl != null && !image.imageUrl.isEmpty()) {
            Picasso.get()
                    .load(image.imageUrl)
                    .placeholder(R.drawable.ic_launcher_background) // optional placeholder
                    .error(R.drawable.ic_launcher_foreground)       // fallback if broken
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.ic_launcher_background);
        }

        holder.image.setOnClickListener(v -> {
            View dialogView = View.inflate(v.getContext(), R.layout.dialog_image, null);

            AlertDialog dialogImage = new AlertDialog.Builder(v.getContext())
                    .setView(dialogView)
                    .create();

            ImageView dialogImageView = dialogView.findViewById(R.id.imageDialog_image);

            if (image.imageUrl != null && !image.imageUrl.isEmpty()) {
                Picasso.get()
                        .load(image.imageUrl)
                        .placeholder(R.drawable.ic_launcher_background) // optional placeholder
                        .error(R.drawable.ic_launcher_foreground)       // fallback if broken
                        .into(dialogImageView);
            } else {
                dialogImageView.setImageResource(R.drawable.ic_launcher_background);
            }

            Button closeDialog = dialogView.findViewById(R.id.button_closeImageDialog);
            if (closeDialog != null) {
                closeDialog.setOnClickListener(closeView -> {
                    dialogImage.dismiss();
                });

            }

            Button deleteImage = dialogView.findViewById(R.id.button_deleteImage);
            if (deleteImage != null) {
                deleteImage.setOnClickListener(deleteView -> {
                    new AlertDialog.Builder(deleteView.getContext())
                            .setTitle("Confirm Deletion")
                            .setMessage("Are you sure you want to delete this image? This action cannot be undone.")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //TODO: delete image from event, from current list,
                                    deleteImage(image, itemPostiion);
                                    dialog.dismiss();
                                    dialogImage.dismiss();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                });
            }

            if (canEdit) deleteImage.setVisibility(Button.VISIBLE);
            else deleteImage.setVisibility(Button.GONE);

            dialogImage.show();

        });
    }

    private void deleteImage(Image image, int position) {
        images.remove(image);
        notifyItemRemoved(position);

        if (image.eventId == null) {
            Log.d(TAG, "Failed to remove image from event: No eventId");
            return;
        }
        if (image.imageId == null) {
            Log.d(TAG, "Failed to remove image from event: No imageId");
            return;
        }

        db.collection("events").document(image.eventId).collection("images").document(image.imageId).delete()
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Successfully removed image from event: " + image.eventId);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove image from event: " + e.getMessage());
                });
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

    public void setEditable(boolean canEdit) {
        this.canEdit = canEdit;
        notifyDataSetChanged();
    }
}
