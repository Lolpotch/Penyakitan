package com.example.penyakitan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CameraCaptureActivity extends AppCompatActivity {

    private final String functionUrl =
            "https://mobile-camera-upload-picture-990423897913.europe-west1.run.app";

    private final String firebaseDbUrl =
            "https://hama-99a97-default-rtdb.asia-southeast1.firebasedatabase.app";

    private final String bucketPublicUrl =
            "https://storage.googleapis.com/camerahama-test/mobile-captures/";

    private ImageView imgUploadPreview;
    private ProgressBar progressUpload;
    private TextView tvUploadStatus;

    private ActivityResultLauncher<String> permissionLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(
                                this,
                                "Izin kamera ditolak",
                                Toast.LENGTH_SHORT
                        ).show();
                        finish();
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();

                        if (extras != null) {
                            Bitmap imageBitmap = (Bitmap) extras.get("data");

                            if (imageBitmap != null) {
                                showUploadScreen(imageBitmap);
                                uploadToCloud(imageBitmap);
                            } else {
                                Toast.makeText(
                                        this,
                                        "Gambar gagal diambil",
                                        Toast.LENGTH_SHORT
                                ).show();
                                finish();
                            }
                        } else {
                            Toast.makeText(
                                    this,
                                    "Data gambar kosong",
                                    Toast.LENGTH_SHORT
                            ).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(
                                this,
                                "Capture dibatalkan",
                                Toast.LENGTH_SHORT
                        ).show();
                        finish();
                    }
                }
        );

        checkCameraPermission();
    }

    private void showUploadScreen(Bitmap bitmap) {
        setContentView(R.layout.activity_camera_capture);

        imgUploadPreview = findViewById(R.id.imgUploadPreview);
        progressUpload = findViewById(R.id.progressUpload);
        tvUploadStatus = findViewById(R.id.tvUploadStatus);

        imgUploadPreview.setImageBitmap(bitmap);
        progressUpload.setIndeterminate(true);
        tvUploadStatus.setText("Upload sedang diproses...");
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(
                    this,
                    "Aplikasi kamera tidak ditemukan",
                    Toast.LENGTH_LONG
            ).show();
            finish();
        }
    }

    private void uploadToCloud(Bitmap bitmap) {
        new Thread(() -> {
            HttpURLConnection conn = null;

            try {
                runOnUiThread(() -> {
                    if (tvUploadStatus != null) {
                        tvUploadStatus.setText("Mengirim foto ke server...");
                    }
                });

                long uploadedAt = System.currentTimeMillis() / 1000;
                String localFilename = "HP_" + uploadedAt + ".jpg";

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, baos);
                byte[] imageBytes = baos.toByteArray();

                String boundary = "----Boundary123456789";
                URL url = new URL(functionUrl);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                conn.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=" + boundary
                );

                OutputStream os = conn.getOutputStream();

                os.write(("--" + boundary + "\r\n").getBytes());

                os.write((
                        "Content-Disposition: form-data; " +
                                "name=\"file\"; " +
                                "filename=\"" + localFilename + "\"\r\n"
                ).getBytes());

                os.write("Content-Type: image/jpeg\r\n\r\n".getBytes());
                os.write(imageBytes);
                os.write("\r\n".getBytes());

                os.write(("--" + boundary + "--\r\n").getBytes());

                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                String responseMessage = readResponse(conn, responseCode);

                Log.d("UPLOAD_RESPONSE", "Code: " + responseCode);
                Log.d("UPLOAD_RESPONSE", "Response: " + responseMessage);

                if (responseCode == 200 || responseCode == 201) {
                    String serverFilename = extractFilenameFromResponse(responseMessage);

                    if (serverFilename == null || serverFilename.trim().isEmpty()) {
                        serverFilename = localFilename;
                    }

                    String uploadedImageUrl = extractImageUrlFromResponse(responseMessage);

                    if (uploadedImageUrl == null || uploadedImageUrl.trim().isEmpty()) {
                        uploadedImageUrl = bucketPublicUrl + serverFilename;
                    }

                    saveHpCaptureToFirebase(serverFilename, uploadedImageUrl, uploadedAt);
                }

                int finalResponseCode = responseCode;
                String finalResponseMessage = responseMessage;

                runOnUiThread(() -> {
                    if (tvUploadStatus != null) {
                        if (finalResponseCode == 200 || finalResponseCode == 201) {
                            tvUploadStatus.setText("Upload berhasil, membuka hasil...");
                        } else {
                            tvUploadStatus.setText("Upload gagal, membuka hasil...");
                        }
                    }

                    openDetectionResult(bitmap, finalResponseCode, finalResponseMessage);
                });

            } catch (Exception e) {
                Log.e("UPLOAD_ERROR", e.toString());

                runOnUiThread(() -> {
                    if (tvUploadStatus != null) {
                        tvUploadStatus.setText("Terjadi error, membuka hasil...");
                    }

                    openDetectionResult(bitmap, 0, e.getMessage());
                });

            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private String readResponse(HttpURLConnection conn, int responseCode) {
        try {
            InputStream inputStream;

            if (responseCode >= 200 && responseCode < 300) {
                inputStream = conn.getInputStream();
            } else {
                inputStream = conn.getErrorStream();
            }

            if (inputStream == null) {
                return "";
            }

            byte[] buffer = new byte[1024];
            StringBuilder builder = new StringBuilder();
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                builder.append(new String(buffer, 0, length));
            }

            inputStream.close();
            return builder.toString();

        } catch (Exception e) {
            Log.e("READ_RESPONSE", "Gagal baca response: " + e.getMessage());
            return "";
        }
    }

    private String extractFilenameFromResponse(String responseMessage) {
        try {
            if (responseMessage == null || responseMessage.trim().isEmpty()) {
                return "";
            }

            JSONObject json = new JSONObject(responseMessage);

            if (json.has("filename")) {
                return json.optString("filename", "");
            }

        } catch (Exception e) {
            Log.e("PARSE_RESPONSE", "Gagal ambil filename: " + e.getMessage());
        }

        return "";
    }

    private String extractImageUrlFromResponse(String responseMessage) {
        try {
            if (responseMessage == null || responseMessage.trim().isEmpty()) {
                return "";
            }

            JSONObject json = new JSONObject(responseMessage);

            if (json.has("image")) {
                return json.optString("image", "");
            }

            if (json.has("image_url")) {
                return json.optString("image_url", "");
            }

            if (json.has("url")) {
                return json.optString("url", "");
            }

            if (json.has("public_url")) {
                return json.optString("public_url", "");
            }

            if (json.has("mediaLink")) {
                return json.optString("mediaLink", "");
            }

        } catch (Exception e) {
            Log.e("PARSE_RESPONSE", "Gagal parse image url: " + e.getMessage());
        }

        return "";
    }

    private void saveHpCaptureToFirebase(String filename, String imageUrl, long uploadedAt) {
        DatabaseReference ref = FirebaseDatabase
                .getInstance(firebaseDbUrl)
                .getReference("camera_captures");

        String key = ref.push().getKey();

        if (key == null) {
            Log.e("FIREBASE_SAVE", "Key Firebase null");
            return;
        }

        String timeText = new SimpleDateFormat(
                "dd MMM yyyy - HH:mm:ss",
                new Locale("id", "ID")
        ).format(new Date(uploadedAt * 1000));

        Map<String, Object> data = new HashMap<>();
        data.put("filename", filename);
        data.put("image", imageUrl);
        data.put("label", "HP");
        data.put("time", timeText);
        data.put("uploaded_at", uploadedAt);

        Log.d("FIREBASE_SAVE", "Menyimpan data: " + data.toString());

        ref.child(key).setValue(data)
                .addOnSuccessListener(unused -> {
                    Log.d("FIREBASE_SAVE", "Berhasil simpan ke Firebase: " + key);

                    runOnUiThread(() ->
                            Toast.makeText(
                                    CameraCaptureActivity.this,
                                    "Data HP tersimpan ke Firebase",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_SAVE", "Gagal simpan Firebase: " + e.getMessage());

                    runOnUiThread(() ->
                            Toast.makeText(
                                    CameraCaptureActivity.this,
                                    "Gagal simpan Firebase: " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show()
                    );
                });
    }

    private void openDetectionResult(Bitmap bitmap, int responseCode, String responseMessage) {
        Intent intent = new Intent(
                CameraCaptureActivity.this,
                DetectionResultActivity.class
        );

        intent.putExtra("image", bitmap);
        intent.putExtra("responseCode", responseCode);
        intent.putExtra("responseMessage", responseMessage);

        startActivity(intent);
        finish();
    }
}