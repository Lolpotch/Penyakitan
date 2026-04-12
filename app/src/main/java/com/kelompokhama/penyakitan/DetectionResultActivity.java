package com.kelompokhama.penyakitan;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class DetectionResultActivity extends AppCompatActivity {

    ImageView imgResult;
    Button btnClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection_result);

        imgResult = findViewById(R.id.imgResult);
        btnClose = findViewById(R.id.btnClose);

        Bitmap image = getIntent().getParcelableExtra("image");

        if(image != null){
            imgResult.setImageBitmap(image);
        }

        btnClose.setOnClickListener(v -> finish());

    }
}