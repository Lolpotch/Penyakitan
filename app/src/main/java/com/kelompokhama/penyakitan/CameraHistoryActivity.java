package com.kelompokhama.penyakitan;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CameraHistoryActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    CameraAdapter adapter;

    List<CameraImage> allImages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_history);

        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        loadPlaceholderImages();

        adapter = new CameraAdapter(allImages);
        recyclerView.setAdapter(adapter);
    }

    private void loadPlaceholderImages(){

        allImages.add(new CameraImage("CAM1","img","10:00"));
        allImages.add(new CameraImage("CAM1","img","10:10"));
        allImages.add(new CameraImage("CAM2","img","10:20"));
        allImages.add(new CameraImage("CAM2","img","10:30"));
        allImages.add(new CameraImage("CAM3","img","10:40"));
        allImages.add(new CameraImage("CAM3","img","10:50"));

    }

}