package com.example.penyakitan;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DetectionResultActivity extends AppCompatActivity {

    ImageView imgResult;
    Button btnClose;
    TextView txtStatus;

    private static DetectionResultActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection_result);

        instance = this;

        imgResult = findViewById(R.id.imgResult);
        btnClose = findViewById(R.id.btnClose);
        txtStatus = findViewById(R.id.txtStatus);

        String imageUriString = getIntent().getStringExtra("image_uri");

        if (imageUriString != null && !imageUriString.trim().isEmpty()) {
            Uri imageUri = Uri.parse(imageUriString);
            imgResult.setImageURI(imageUri);
        } else {
            Bitmap image = getIntent().getParcelableExtra("image");

            if (image != null) {
                imgResult.setImageBitmap(image);
            }
        }

        int responseCode = getIntent().getIntExtra("responseCode", -1);
        String responseMessage = getIntent().getStringExtra("responseMessage");

        if (responseCode == 200 || responseCode == 201) {
            txtStatus.setVisibility(View.VISIBLE);
            txtStatus.setText("Upload Success");
        } else if (responseCode != -1) {
            txtStatus.setVisibility(View.VISIBLE);
            txtStatus.setText("Upload Failed: " + responseMessage);
        } else {
            txtStatus.setVisibility(View.GONE);
        }

        btnClose.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (instance == this) {
            instance = null;
        }
    }

    public static void showUploadError(String message) {
        if (instance == null) return;

        instance.runOnUiThread(() -> {
            instance.txtStatus.setVisibility(View.VISIBLE);
            instance.txtStatus.setText("Upload Failed: " + message);
        });
    }

    public static void showUploadSuccess(String message) {
        if (instance == null) return;

        instance.runOnUiThread(() -> {
            instance.txtStatus.setVisibility(View.VISIBLE);
            instance.txtStatus.setText("Upload Success: " + message);
        });
    }
}