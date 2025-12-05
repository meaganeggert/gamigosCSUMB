package com.example.gamigosjava.ui.adapter;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.Attendee;
import com.squareup.picasso.Picasso;

import java.util.List;

public class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.ViewHolder> {

        private final List<Attendee> attendees;

        public AttendeeAdapter(List<Attendee> attendees) {
            this.attendees = attendees;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView avatar;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.avatarImage);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.small_avatar_image, parent, false);
            return new ViewHolder(v);
        }


    @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Attendee attendee = attendees.get(position);

            Picasso.get()
                    .load(attendee.getAvatarUrl())
                    .placeholder(R.drawable.ic_person_24)
                    .error(R.drawable.ic_person_24)
                    .into(holder.avatar);
        }

        @Override
        public int getItemCount() {
            return attendees.size();
        }
}
