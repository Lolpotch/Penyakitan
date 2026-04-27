package com.kelompokhama.penyakitan;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CameraHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CameraAdapter adapter;
    private List<CameraImage> allImages = new ArrayList<>();

    private final String bucketName = "camerahama-test";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_history);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        adapter = new CameraAdapter(allImages);
        recyclerView.setAdapter(adapter);

        loadImagesFromBucket();
    }

    private void loadImagesFromBucket() {

        new Thread(() -> {

            try {

                String apiUrl =
                        "https://storage.googleapis.com/storage/v1/b/"
                                + bucketName + "/o";

                URL url = new URL(apiUrl);

                HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        conn.getInputStream()
                                )
                        );

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();

                JSONObject jsonObject =
                        new JSONObject(result.toString());

                JSONArray items =
                        jsonObject.getJSONArray("items");

                allImages.clear();

                for (int i = 0; i < items.length(); i++) {

                    JSONObject file =
                            items.getJSONObject(i);

                    String fileName =
                            file.getString("name");

                    if (isImage(fileName)) {

                        String imageUrl =
                                file.getString("mediaLink");

                        String updated =
                                file.optString("updated", "");

                        allImages.add(
                                new CameraImage(
                                        fileName,
                                        imageUrl,
                                        updated
                                )
                        );
                    }
                }

                Collections.reverse(allImages);

                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();

                    Toast.makeText(
                            CameraHistoryActivity.this,
                            "Loaded " + allImages.size() + " images",
                            Toast.LENGTH_SHORT
                    ).show();
                });

            } catch (Exception e) {

                Log.e("GCP_ERROR", e.toString());

                runOnUiThread(() ->
                        Toast.makeText(
                                CameraHistoryActivity.this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }

        }).start();
    }

    private boolean isImage(String name) {

        name = name.toLowerCase();

        return name.endsWith(".jpg") ||
                name.endsWith(".jpeg") ||
                name.endsWith(".png") ||
                name.endsWith(".webp");
    }
}