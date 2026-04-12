package com.kelompokhama.penyakitan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CameraAdapter extends RecyclerView.Adapter<CameraAdapter.ViewHolder> {

    List<CameraImage> images;

    public CameraAdapter(List<CameraImage> images){
        this.images = images;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        ImageView imgCamera;

        public ViewHolder(View v){
            super(v);

            imgCamera = v.findViewById(R.id.imgCamera);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,int viewType){

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_camera_image,parent,false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder,int position){

        CameraImage img = images.get(position);

        holder.imgCamera.setImageResource(R.drawable.ic_launcher_background);

    }

    @Override
    public int getItemCount(){
        return images.size();
    }
}