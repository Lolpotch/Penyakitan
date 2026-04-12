package com.kelompokhama.penyakitan;


import com.google.firebase.database.*;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.components.XAxis;


public class DashboardActivity extends AppCompatActivity {

    Button btnCameraHistory;

    Button btnOpenCamera;

    LinearLayout alertContainer;

    List<AlertPanel> alertList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        btnCameraHistory = findViewById(R.id.btnCameraHistory);
        alertContainer = findViewById(R.id.alertContainer);

        btnCameraHistory.setOnClickListener(v -> {

            Intent i = new Intent(this,CameraHistoryActivity.class);
            startActivity(i);

        });

        loadTemperature();
        loadTemperatureGraph();
        checkDiseaseDetection();

        btnOpenCamera = findViewById(R.id.btnOpenCamera);

        btnOpenCamera.setOnClickListener(v -> {

            Intent i = new Intent(this, CameraCaptureActivity.class);
            startActivity(i);

        });

    }

    //DATA SUHU
    private void loadTemperature(){
        TextView tvTemperature = findViewById(R.id.tvTemperature);

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("sensor")
                .child("dht22")
                .child("latest")
                .child("temperature");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                if(snapshot.exists()){

                    Double suhu = snapshot.getValue(Double.class);

                    if(suhu != null){
                        tvTemperature.setText("Suhu Saat Ini : " + suhu + " °C");
                    }

                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvTemperature.setText("Error: " + error.getMessage());
            }
        });
//        float suhu = getTemperatureFromCloud();
//
//        TextView tvTemperature = findViewById(R.id.tvTemperature);
//        tvTemperature.setText("Suhu Saat Ini : " + suhu + " °C");

    }

    //GRAPH SUHU
    private void loadTemperatureGraph(){

        LineChart chart = findViewById(R.id.temperatureGraph);

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("sensor")
                .child("dht22")
                .child("history");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                List<Entry> entries = new ArrayList<>();
                int index = 0;

                for(DataSnapshot data : snapshot.getChildren()){

                    Double temp = data.child("temperature").getValue(Double.class);

                    if(temp != null){
                        entries.add(new Entry(index++, temp.floatValue()));
                    }

                }

                LineDataSet dataSet = new LineDataSet(entries,"Suhu");
                LineData lineData = new LineData(dataSet);

                chart.setData(lineData);
                chart.getDescription().setEnabled(false);

                XAxis xAxis = chart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                chart.invalidate();
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });

//        LineChart chart = findViewById(R.id.temperatureGraph);
//
//        List<TemperatureData> history = getTemperatureHistory();
//
//        List<Entry> entries = new ArrayList<>();
//
//        for(int i=0;i<history.size();i++){
//
//            entries.add(new Entry(i, history.get(i).temperature));
//
//        }
//
//        LineDataSet dataSet = new LineDataSet(entries,"Suhu");
//
//        LineData lineData = new LineData(dataSet);
//
//        chart.setData(lineData);
//
//        chart.getDescription().setEnabled(false);
//
//        XAxis xAxis = chart.getXAxis();
//        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
//
//        chart.invalidate();

    }

    //DETEKSI PENYAKIT
    private void checkDiseaseDetection(){

        List<AlertPanel> newAlerts = getDiseaseFromServer();

        for(AlertPanel alert : newAlerts){

            if(!alertList.contains(alert)){

                alertList.add(alert);
                addAlertPanel(alert);

            }

        }

    }

    //TAMBAH PANEL ALERT
    private void addAlertPanel(AlertPanel alert){

        AlertView view = new AlertView(this);

        view.setData(
                alert.imageUrl,
                alert.date,
                alert.diseaseName,
                alert.description,
                alert.solution
        );

        view.setOnSolveClick(() -> {

            alertContainer.removeView(view);
            alertList.remove(alert);

        });

        alertContainer.addView(view);

    }


    //PLACEHOLDER CLOUD FUNCTION
    private float getTemperatureFromCloud(){


        return 28.5f;

    }

    private List<TemperatureData> getTemperatureHistory(){

        List<TemperatureData> data = new ArrayList<>();

        data.add(new TemperatureData(27.5f,"08:00"));
        data.add(new TemperatureData(28.1f,"09:00"));
        data.add(new TemperatureData(29.0f,"10:00"));
        data.add(new TemperatureData(30.2f,"11:00"));
        data.add(new TemperatureData(31.1f,"12:00"));
        data.add(new TemperatureData(30.4f,"13:00"));
        data.add(new TemperatureData(29.6f,"14:00"));

        return data;

    }

    private List<AlertPanel> getDiseaseFromServer(){

        List<AlertPanel> alerts = new ArrayList<>();

        AlertPanel alert1 = new AlertPanel(
                "CAM1",
                "Leaf Blight",
                "placeholder",
                "2026-03-10 10:15",
                "Penyakit ini menyebabkan daun menguning dan muncul bercak coklat.",
                "Gunakan fungisida dan potong daun yang terinfeksi."
        );

        AlertPanel alert2 = new AlertPanel(
                "CAM2",
                "Powdery Mildew",
                "placeholder",
                "2026-03-10 11:20",
                "Jamur putih muncul pada permukaan daun tanaman.",
                "Semprot dengan larutan baking soda atau fungisida khusus."
        );

        alerts.add(alert1);
        alerts.add(alert2);

        return alerts;

    }

}