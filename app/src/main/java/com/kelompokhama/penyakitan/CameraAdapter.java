package com.kelompokhama.penyakitan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class CameraAdapter extends RecyclerView.Adapter<CameraAdapter.ViewHolder> {

    List<CameraImage> images;

    public CameraAdapter(List<CameraImage> images){
        this.images = images;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgCamera;

        public ViewHolder(View v){
            super(v);
            imgCamera = v.findViewById(R.id.imgCamera);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_camera_image, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position){

        CameraImage img = images.get(position);

        Glide.with(holder.itemView.getContext())
                .load(img.imageURL)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_foreground)
                .centerCrop()
                .into(holder.imgCamera);
    }

    @Override
    public int getItemCount(){
        return images.size();
    }
}