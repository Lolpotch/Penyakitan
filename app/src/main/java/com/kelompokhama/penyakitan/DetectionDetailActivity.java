package com.example.penyakitan;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class DetectionDetailActivity extends AppCompatActivity {

    private ImageView imgDetail;
    private TextView tvTitle, tvDate, tvStatus;
    private TextView tvMode, tvSource, tvConfidence;
    private TextView tvDescription, tvSolution;
    private Button btnClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection_detail);

        imgDetail = findViewById(R.id.imgDetail);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvDate = findViewById(R.id.tvDetailDate);
        tvStatus = findViewById(R.id.tvDetailStatus);
        tvMode = findViewById(R.id.tvDetailMode);
        tvSource = findViewById(R.id.tvDetailSource);
        tvConfidence = findViewById(R.id.tvDetailConfidence);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvSolution = findViewById(R.id.tvDetailSolution);
        btnClose = findViewById(R.id.btnCloseDetail);

        String imageUrl = getIntent().getStringExtra("image_url");
        String diseaseName = getIntent().getStringExtra("disease_name");
        String date = getIntent().getStringExtra("date");
        String description = getIntent().getStringExtra("description");
        String solution = getIntent().getStringExtra("solution");
        boolean handled = getIntent().getBooleanExtra("handled", false);
        String mode = getIntent().getStringExtra("mode");
        String source = getIntent().getStringExtra("source");
        String confidence = getIntent().getStringExtra("confidence");

        if (imageUrl != null && !imageUrl.trim().isEmpty() && !imageUrl.equals("placeholder")) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.plant)
                    .error(R.drawable.plant)
                    .into(imgDetail);
        } else {
            imgDetail.setImageResource(R.drawable.plant);
        }

        tvTitle.setText(safeText(diseaseName, "Tidak Diketahui"));
        tvDate.setText(safeText(date, "-"));
        tvMode.setText("Mode: " + safeText(mode, "-"));
        tvSource.setText("Source: " + safeText(source, "-"));
        tvConfidence.setText("Confidence: " + safeText(confidence, "0") + "%");
        tvDescription.setText(safeText(description, "Tidak ada deskripsi."));
        tvSolution.setText(safeText(solution, "Belum ada rekomendasi penanganan."));

        if (handled) {
            tvStatus.setText("Sudah Ditangani");
            tvStatus.setTextColor(Color.parseColor("#667085"));
            tvStatus.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#EEF0F2"))
            );
        } else {
            tvStatus.setText("Belum Ditangani");
            tvStatus.setTextColor(Color.parseColor("#D92D20"));
            tvStatus.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#FEE4E2"))
            );
        }

        btnClose.setOnClickListener(v -> finish());
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value;
    }
}