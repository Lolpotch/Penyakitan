package com.example.penyakitan;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DetectionResultActivity extends AppCompatActivity {

    private ImageView imgResult;
    private Button btnClose;
    private TextView txtStatus;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection_result);

        imgResult = findViewById(R.id.imgResult);
        btnClose = findViewById(R.id.btnClose);
        txtStatus = findViewById(R.id.txtStatus);

        Bitmap image = getIntent().getParcelableExtra("image");
        int responseCode = getIntent().getIntExtra("responseCode", 0);
        String responseMessage = getIntent().getStringExtra("responseMessage");

        if (image != null) {
            imgResult.setImageBitmap(image);
        }

        showUploadStatus(responseCode, responseMessage);

        btnClose.setOnClickListener(v -> finish());
    }

    private void showUploadStatus(int responseCode, String responseMessage) {
        txtStatus.setVisibility(View.VISIBLE);

        if (responseCode == 200 || responseCode == 201) {
            txtStatus.setText("Upload berhasil");
            txtStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (responseCode == 0) {
            txtStatus.setText("Upload gagal: " + safeMessage(responseMessage));
            txtStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            txtStatus.setText("Upload gagal: " + responseCode);
            txtStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private String safeMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Tidak ada response dari server";
        }
        return message;
    }
}