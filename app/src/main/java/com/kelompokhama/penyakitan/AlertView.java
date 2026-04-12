package com.kelompokhama.penyakitan;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AlertView extends LinearLayout {

    ImageView imgDisease;
    TextView tvDiseaseName;
    TextView tvDate;
    TextView tvDescription;
    TextView tvSolution;
    Button btnSolved;

    public AlertView(Context context) {
        super(context);

        LayoutInflater.from(context).inflate(R.layout.item_alert_panel, this, true);

        imgDisease = findViewById(R.id.imgDisease);
        tvDiseaseName = findViewById(R.id.tvDiseaseName);
        tvDate = findViewById(R.id.tvDate);
        tvDescription = findViewById(R.id.tvDescription);
        tvSolution = findViewById(R.id.tvSolution);
        btnSolved = findViewById(R.id.btnSolved);
    }

    public void setData(String imageUrl,
                        String date,
                        String diseaseName,
                        String description,
                        String solution){

        tvDiseaseName.setText(diseaseName);
        tvDate.setText(date);
        tvDescription.setText(description);
        tvSolution.setText(solution);

        // Placeholder image
        imgDisease.setImageResource(R.drawable.ic_launcher_background);

        //TODO: nanti load image dari Cloud Storage
    }

    public void setOnSolveClick(Runnable action){

        btnSolved.setOnClickListener(v -> action.run());

    }

}
