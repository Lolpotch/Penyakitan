package com.example.penyakitan;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvTemperature, tvHumidity;
    private TextView tvTemperatureStatus, tvHumidityStatus;
    private TextView tvActiveCamera, tvPlantPoint, tvPlantCondition;

    private TextView tvSeeAll;
    private TextView tvSeeAllDetection;
    private TextView tvCarouselLabel;

    private ImageButton btnOpenCamera;
    private Button btnCaptureNow;

    private LineChart temperatureGraph, humidityGraph;
    private LinearLayout alertContainer;

    private ViewPager2 viewPagerLatestImages;
    private LatestImageAdapter latestImageAdapter;

    private final List<String> latestImageUrls = new ArrayList<>();
    private final List<String> latestImageLabels = new ArrayList<>();
    private final List<AlertPanel> alertList = new ArrayList<>();

    private DatabaseReference sensorLatestRef;
    private DatabaseReference sensorHistoryRef;
    private DatabaseReference cameraCapturesRef;
    private DatabaseReference diseaseResultRef;

    private ImageView imgPlantLeft, imgPlantRight, imgPlantHp;
    private TextView tvPlantLeftTime, tvPlantRightTime, tvPlantHpTime;
    private TextView tvPlantLeftStatus, tvPlantRightStatus, tvPlantHpStatus;
    private LinearLayout cardPlantLeft, cardPlantRight, cardPlantHp;

    private final Handler carouselHandler = new Handler(Looper.getMainLooper());

    private final Runnable carouselRunnable = new Runnable() {
        @Override
        public void run() {
            if (latestImageUrls.size() > 1 && viewPagerLatestImages != null) {
                int currentItem = viewPagerLatestImages.getCurrentItem();
                int nextItem = currentItem + 1;

                if (nextItem >= latestImageUrls.size()) {
                    nextItem = 0;
                }

                viewPagerLatestImages.setCurrentItem(nextItem, true);
            }

            carouselHandler.postDelayed(this, 3000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initView();
        initFirebase();
        setupButton();

        loadLatestCarouselImages();
        loadPlantPointCards();
        loadLatestSensorData();
        loadTemperatureGraph();
        loadHumidityGraph();
        loadLatestDiseaseDetections();
    }

    private void initView() {
        tvTemperature = findViewById(R.id.tvTemperature);
        tvHumidity = findViewById(R.id.tvHumidity);

        tvTemperatureStatus = findViewById(R.id.tvTemperatureStatus);
        tvHumidityStatus = findViewById(R.id.tvHumidityStatus);

        tvActiveCamera = findViewById(R.id.tvActiveCamera);
        tvPlantPoint = findViewById(R.id.tvPlantPoint);
        tvPlantCondition = findViewById(R.id.tvPlantCondition);

        tvSeeAll = findViewById(R.id.tvSeeAll);
        tvSeeAllDetection = findViewById(R.id.tvSeeAllDetection);
        tvCarouselLabel = findViewById(R.id.tvCarouselLabel);

        btnOpenCamera = findViewById(R.id.btnOpenCamera);
        btnCaptureNow = findViewById(R.id.btnCaptureNow);

        temperatureGraph = findViewById(R.id.temperatureGraph);
        humidityGraph = findViewById(R.id.humidityGraph);

        alertContainer = findViewById(R.id.alertContainer);

        viewPagerLatestImages = findViewById(R.id.viewPagerLatestImages);
        latestImageAdapter = new LatestImageAdapter(latestImageUrls);
        viewPagerLatestImages.setAdapter(latestImageAdapter);

        viewPagerLatestImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                if (position >= 0 && position < latestImageLabels.size()) {
                    tvCarouselLabel.setText(latestImageLabels.get(position));
                }
            }
        });

        imgPlantLeft = findViewById(R.id.imgPlantLeft);
        imgPlantRight = findViewById(R.id.imgPlantRight);
        imgPlantHp = findViewById(R.id.imgPlantHp);

        tvPlantLeftTime = findViewById(R.id.tvPlantLeftTime);
        tvPlantRightTime = findViewById(R.id.tvPlantRightTime);
        tvPlantHpTime = findViewById(R.id.tvPlantHpTime);

        tvPlantLeftStatus = findViewById(R.id.tvPlantLeftStatus);
        tvPlantRightStatus = findViewById(R.id.tvPlantRightStatus);
        tvPlantHpStatus = findViewById(R.id.tvPlantHpStatus);

        cardPlantLeft = findViewById(R.id.cardPlantLeft);
        cardPlantRight = findViewById(R.id.cardPlantRight);
        cardPlantHp = findViewById(R.id.cardPlantHp);
    }

    private void initFirebase() {
        sensorLatestRef = FirebaseDatabase.getInstance()
                .getReference("sensor")
                .child("dht22")
                .child("latest");

        sensorHistoryRef = FirebaseDatabase.getInstance()
                .getReference("sensor")
                .child("dht22")
                .child("history");

        cameraCapturesRef = FirebaseDatabase.getInstance()
                .getReference("camera_captures");

        diseaseResultRef = FirebaseDatabase.getInstance()
                .getReference("inference_result")
                .child("disease");
    }

    private void setupButton() {
        btnOpenCamera.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CameraCaptureActivity.class);
            startActivity(intent);
        });

        btnCaptureNow.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CameraCaptureActivity.class);
            startActivity(intent);
        });

        tvSeeAll.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CameraHistoryActivity.class);
            startActivity(intent);
        });

        tvSeeAllDetection.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, DetectionHistoryActivity.class);
            startActivity(intent);
        });

        cardPlantLeft.setOnClickListener(v -> openHistoryByLabel("A"));
        cardPlantRight.setOnClickListener(v -> openHistoryByLabel("B"));
        cardPlantHp.setOnClickListener(v -> openHistoryByLabel("HP"));
    }

    private void openHistoryByLabel(String label) {
        Intent intent = new Intent(DashboardActivity.this, CameraHistoryActivity.class);
        intent.putExtra("label_filter", label);
        startActivity(intent);
    }

    private void loadLatestCarouselImages() {
        Query query = cameraCapturesRef
                .orderByChild("uploaded_at")
                .limitToLast(4);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<LatestPhoto> latestPhotos = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    String imageUrl = data.child("image").getValue(String.class);
                    String label = data.child("label").getValue(String.class);

                    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                        if (label == null || label.trim().isEmpty()) {
                            label = "Foto Kamera";
                        }

                        latestPhotos.add(new LatestPhoto(imageUrl, label));
                    }
                }

                Collections.reverse(latestPhotos);

                latestImageUrls.clear();
                latestImageLabels.clear();

                for (LatestPhoto photo : latestPhotos) {
                    latestImageUrls.add(photo.imageUrl);
                    latestImageLabels.add(photo.label);
                }

                latestImageAdapter.notifyDataSetChanged();

                if (!latestImageLabels.isEmpty()) {
                    tvCarouselLabel.setText(latestImageLabels.get(0));
                    viewPagerLatestImages.setCurrentItem(0, false);
                } else {
                    tvCarouselLabel.setText("Belum Ada Foto");
                }

                startAutoCarousel();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                latestImageUrls.clear();
                latestImageLabels.clear();
                latestImageAdapter.notifyDataSetChanged();

                tvCarouselLabel.setText("Gagal Memuat Foto");

                carouselHandler.removeCallbacks(carouselRunnable);
            }
        });
    }

    private void startAutoCarousel() {
        carouselHandler.removeCallbacks(carouselRunnable);

        if (latestImageUrls.size() > 1) {
            carouselHandler.postDelayed(carouselRunnable, 3000);
        }
    }

    private void loadPlantPointCards() {
        Query query = cameraCapturesRef.orderByChild("uploaded_at");

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                LatestPhoto leftPhoto = null;
                LatestPhoto rightPhoto = null;
                LatestPhoto hpPhoto = null;

                for (DataSnapshot data : snapshot.getChildren()) {
                    String imageUrl = data.child("image").getValue(String.class);
                    String label = data.child("label").getValue(String.class);
                    String time = data.child("time").getValue(String.class);

                    if (imageUrl == null || imageUrl.trim().isEmpty()) {
                        continue;
                    }

                    if (label == null) {
                        label = "";
                    }

                    if (time == null || time.trim().isEmpty()) {
                        time = "-";
                    }

                    LatestPhoto photo = new LatestPhoto(imageUrl, label, time);

                    if (label.equalsIgnoreCase("A")) {
                        leftPhoto = photo;
                    } else if (label.equalsIgnoreCase("B")) {
                        rightPhoto = photo;
                    } else if (label.equalsIgnoreCase("HP")) {
                        hpPhoto = photo;
                    }
                }

                updatePlantCard(leftPhoto, imgPlantLeft, tvPlantLeftTime, tvPlantLeftStatus, "Normal");
                updatePlantCard(rightPhoto, imgPlantRight, tvPlantRightTime, tvPlantRightStatus, "Normal");
                updatePlantCard(hpPhoto, imgPlantHp, tvPlantHpTime, tvPlantHpStatus, "Mobile");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void updatePlantCard(
            LatestPhoto photo,
            ImageView imageView,
            TextView timeView,
            TextView statusView,
            String defaultStatus
    ) {
        if (photo == null) {
            timeView.setText("Update: -");
            statusView.setText("Belum Ada");
            statusView.setTextColor(Color.parseColor("#667085"));
            return;
        }

        Glide.with(this)
                .load(photo.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.plant)
                .error(R.drawable.plant)
                .into(imageView);

        timeView.setText("Update: " + photo.time);

        if (photo.label.equalsIgnoreCase("HP")) {
            statusView.setText("Mobile");
            statusView.setTextColor(Color.parseColor("#2563EB"));
        } else {
            statusView.setText(defaultStatus);
            statusView.setTextColor(Color.parseColor("#0B7A2A"));
        }
    }

    private void loadLatestDiseaseDetections() {
        Query query = diseaseResultRef.orderByChild("timestamp");

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                alertContainer.removeAllViews();
                alertList.clear();

                List<DetectionAlertItem> tempAlerts = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    String detectionKey = data.getKey();

                    Boolean handled = data.child("handled").getValue(Boolean.class);
                    String status = data.child("status").getValue(String.class);

                    // Lewati data yang sudah ditangani
                    if (handled != null && handled) {
                        continue;
                    }

                    if (status != null && status.equalsIgnoreCase("handled")) {
                        continue;
                    }

                    String className = data.child("class_name").getValue(String.class);
                    String imageUrl = data.child("image_url").getValue(String.class);
                    String mode = data.child("mode").getValue(String.class);
                    String recommendation = data.child("recommendation").getValue(String.class);
                    String source = data.child("source").getValue(String.class);
                    String timestamp = data.child("timestamp").getValue(String.class);

                    Double confidenceValue = data.child("confidence").getValue(Double.class);

                    if (className == null || className.trim().isEmpty()) {
                        className = "Tidak Diketahui";
                    }

                    if (imageUrl == null || imageUrl.trim().isEmpty()) {
                        imageUrl = "placeholder";
                    }

                    if (mode == null || mode.trim().isEmpty()) {
                        mode = "disease";
                    }

                    if (source == null || source.trim().isEmpty()) {
                        source = "kamera";
                    }

                    if (timestamp == null || timestamp.trim().isEmpty()) {
                        timestamp = "-";
                    }

                    if (recommendation == null || recommendation.trim().isEmpty()) {
                        recommendation = "Belum ada rekomendasi penanganan.";
                    }

                    double confidencePercent = 0;

                    if (confidenceValue != null) {
                        confidencePercent = confidenceValue * 100;
                    }

                    String displayName = formatClassName(className);

                    String description =
                            "Mode: " + mode +
                                    "\nSource: " + source +
                                    "\nConfidence: " + String.format(Locale.US, "%.2f", confidencePercent) + "%";

                    AlertPanel alert = new AlertPanel(
                            source,
                            displayName,
                            imageUrl,
                            timestamp,
                            description,
                            recommendation
                    );

                    tempAlerts.add(new DetectionAlertItem(detectionKey, alert));
                }

                // Urutkan terbaru di depan
                Collections.reverse(tempAlerts);

                // Tampilkan SEMUA yang belum handled
                for (DetectionAlertItem item : tempAlerts) {
                    alertList.add(item.alert);
                    addAlertPanel(item.alert, item.detectionKey);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                alertContainer.removeAllViews();

                AlertPanel errorAlert = new AlertPanel(
                        "Firebase",
                        "Gagal Memuat Deteksi",
                        "placeholder",
                        "-",
                        "Data deteksi gagal diambil dari Firebase.",
                        error.getMessage()
                );

                addAlertPanel(errorAlert, null);
            }
        });
    }

    private String formatClassName(String className) {
        if (className == null || className.trim().isEmpty()) {
            return "Tidak Diketahui";
        }

        String cleaned = className.replace("_", " ");
        String[] words = cleaned.split(" ");

        StringBuilder builder = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                builder.append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return builder.toString().trim();
    }

    private void loadLatestSensorData() {
        sensorLatestRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double suhu = snapshot.child("temperature").getValue(Double.class);
                Double humidity = snapshot.child("humidity").getValue(Double.class);

                if (suhu != null) {
                    tvTemperature.setText(formatNumber(suhu) + "°C");
                    setTemperatureStatus(suhu);
                } else {
                    tvTemperature.setText("--°C");
                    tvTemperatureStatus.setText("Tidak ada data");
                }

                if (humidity != null) {
                    tvHumidity.setText(formatNumber(humidity) + "%");
                    setHumidityStatus(humidity);
                } else {
                    tvHumidity.setText("--%");
                    tvHumidityStatus.setText("Tidak ada data");
                }

                tvActiveCamera.setText("1 Online");
                tvPlantPoint.setText("2 Titik + HP");
                tvPlantCondition.setText("Baik");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvTemperature.setText("--°C");
                tvHumidity.setText("--%");
                tvTemperatureStatus.setText("Error");
                tvHumidityStatus.setText("Error");
            }
        });
    }

    private void setTemperatureStatus(double suhu) {
        if (suhu < 20) {
            tvTemperatureStatus.setText("Rendah");
            tvTemperatureStatus.setTextColor(Color.parseColor("#2563EB"));
        } else if (suhu <= 32) {
            tvTemperatureStatus.setText("Normal");
            tvTemperatureStatus.setTextColor(Color.parseColor("#0B7A2A"));
        } else {
            tvTemperatureStatus.setText("Tinggi");
            tvTemperatureStatus.setTextColor(Color.parseColor("#D92D20"));
        }
    }

    private void setHumidityStatus(double humidity) {
        if (humidity < 50) {
            tvHumidityStatus.setText("Rendah");
            tvHumidityStatus.setTextColor(Color.parseColor("#C4320A"));
        } else if (humidity <= 85) {
            tvHumidityStatus.setText("Normal");
            tvHumidityStatus.setTextColor(Color.parseColor("#0B7A2A"));
        } else {
            tvHumidityStatus.setText("Tinggi");
            tvHumidityStatus.setTextColor(Color.parseColor("#2563EB"));
        }
    }

    private void loadTemperatureGraph() {
        Query query = sensorHistoryRef.limitToLast(10);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Entry> entries = new ArrayList<>();
                int index = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Double temp = data.child("temperature").getValue(Double.class);

                    if (temp != null) {
                        entries.add(new Entry(index, temp.floatValue()));
                        index++;
                    }
                }

                setupLineChart(
                        temperatureGraph,
                        entries,
                        "Suhu (°C)",
                        Color.parseColor("#FF6B1A")
                );
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadHumidityGraph() {
        Query query = sensorHistoryRef.limitToLast(10);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Entry> entries = new ArrayList<>();
                int index = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Double humidity = data.child("humidity").getValue(Double.class);

                    if (humidity != null) {
                        entries.add(new Entry(index, humidity.floatValue()));
                        index++;
                    }
                }

                setupLineChart(
                        humidityGraph,
                        entries,
                        "Kelembaban (%)",
                        Color.parseColor("#1E88E5")
                );
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void setupLineChart(LineChart chart, List<Entry> entries, String label, int color) {
        LineDataSet dataSet = new LineDataSet(entries, label);

        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);

        chart.setData(lineData);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(true);

        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);

        chart.getAxisRight().setEnabled(false);

        chart.getAxisLeft().setTextColor(Color.parseColor("#667085"));
        chart.getAxisLeft().setGridColor(Color.parseColor("#EEEEEE"));

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.parseColor("#667085"));
        xAxis.setGridColor(Color.parseColor("#EEEEEE"));
        xAxis.setGranularity(1f);

        chart.invalidate();
    }

    private String formatNumber(double value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        } else {
            return String.format(Locale.US, "%.1f", value);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void addAlertPanel(AlertPanel alert, String detectionKey) {
        AlertView view = new AlertView(this);

        view.setData(
                alert.imageUrl,
                alert.date,
                alert.diseaseName,
                alert.description,
                alert.solution
        );

        view.setSolveButtonVisible(true);

        view.setOnSolveClick(() -> {
            if (detectionKey == null || detectionKey.trim().isEmpty()) {
                Toast.makeText(
                        DashboardActivity.this,
                        "ID deteksi tidak ditemukan",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            markDetectionAsHandled(detectionKey);
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(280),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        params.setMargins(0, 0, dpToPx(12), 0);

        view.setLayoutParams(params);
        alertContainer.addView(view);
    }

    private void markDetectionAsHandled(String detectionKey) {
        Map<String, Object> updates = new HashMap<>();

        updates.put("handled", true);
        updates.put("handled_at", getCurrentIsoTime());
        updates.put("status", "handled");

        diseaseResultRef.child(detectionKey)
                .updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            DashboardActivity.this,
                            "Deteksi ditandai selesai",
                            Toast.LENGTH_SHORT
                    ).show();

                    // Tidak perlu removeView manual.
                    // Listener Firebase akan refresh dan menghilangkan card otomatis.
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            DashboardActivity.this,
                            "Gagal update Firebase: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private String getCurrentIsoTime() {
        SimpleDateFormat sdf = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                Locale.US
        );

        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        return sdf.format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        carouselHandler.removeCallbacks(carouselRunnable);
    }

    private static class LatestPhoto {
        String imageUrl;
        String label;
        String time;

        LatestPhoto(String imageUrl, String label) {
            this.imageUrl = imageUrl;
            this.label = label;
            this.time = "-";
        }

        LatestPhoto(String imageUrl, String label, String time) {
            this.imageUrl = imageUrl;
            this.label = label;
            this.time = time;
        }
    }

    private static class DetectionAlertItem {
        String detectionKey;
        AlertPanel alert;

        DetectionAlertItem(String detectionKey, AlertPanel alert) {
            this.detectionKey = detectionKey;
            this.alert = alert;
        }
    }
}