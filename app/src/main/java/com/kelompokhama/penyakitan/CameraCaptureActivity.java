package com.kelompokhama.penyakitan;


import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class CameraCaptureActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    // Your Cloud Run / Cloud Function URL
    private final String functionUrl =
            "https://mobile-camera-upload-picture-990423897913.europe-west1.run.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent takePictureIntent =
                new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        startActivityForResult(
                takePictureIntent,
                REQUEST_IMAGE_CAPTURE
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == REQUEST_IMAGE_CAPTURE
                && resultCode == RESULT_OK
                && data != null
                && data.getExtras() != null) {

            Bitmap imageBitmap =
                    (Bitmap) data.getExtras().get("data");

            if (imageBitmap != null) {
                uploadToCloud(imageBitmap);
            } else {
                Toast.makeText(
                        this,
                        "Image capture failed",
                        Toast.LENGTH_LONG
                ).show();

                finish();
            }

        } else {
            finish();
        }
    }

    private void uploadToCloud(Bitmap bitmap) {

        new Thread(() -> {

            HttpURLConnection conn = null;

            try {

                ByteArrayOutputStream baos =
                        new ByteArrayOutputStream();

                bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        95,
                        baos
                );

                byte[] imageBytes =
                        baos.toByteArray();

                String boundary =
                        "----Boundary123456789";

                URL url = new URL(functionUrl);

                conn =
                        (HttpURLConnection)
                                url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                conn.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=" + boundary
                );

                OutputStream os =
                        conn.getOutputStream();

                os.write(
                        ("--" + boundary + "\r\n").getBytes()
                );

                os.write(
                        (
                                "Content-Disposition: form-data; " +
                                        "name=\"file\"; " +
                                        "filename=\"photo.jpg\"\r\n"
                        ).getBytes()
                );

                os.write(
                        "Content-Type: image/jpeg\r\n\r\n"
                                .getBytes()
                );

                os.write(imageBytes);

                os.write("\r\n".getBytes());

                os.write(
                        ("--" + boundary + "--\r\n")
                                .getBytes()
                );

                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                String responseMessage;

                if (responseCode >= 200 && responseCode < 300) {

                    responseMessage = "Upload Success";

                } else {

                    java.io.InputStream errorStream =
                            conn.getErrorStream();

                    if (errorStream == null) {
                        errorStream = conn.getInputStream();
                    }

                    java.io.BufferedReader reader =
                            new java.io.BufferedReader(
                                    new java.io.InputStreamReader(errorStream)
                            );

                    StringBuilder errorText =
                            new StringBuilder();

                    String line;

                    while ((line = reader.readLine()) != null) {
                        errorText.append(line);
                    }

                    reader.close();

                    responseMessage = errorText.toString();

                    // showUploadError(errorText.toString());
                }

                String finalResponseMessage = responseMessage;

                runOnUiThread(() -> {

                    Toast.makeText(
                            CameraCaptureActivity.this,
                            finalResponseMessage,
                            Toast.LENGTH_LONG
                    ).show();

                    openDetectionResult(bitmap);
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    Toast.makeText(
                            CameraCaptureActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                    openDetectionResult(bitmap);
                });

            } finally {

                if (conn != null) {
                    conn.disconnect();
                }
            }

        }).start();
    }

    private void openDetectionResult(Bitmap bitmap) {

        Intent i =
                new Intent(
                        CameraCaptureActivity.this,
                        DetectionResultActivity.class
                );

        i.putExtra("image", bitmap);

        startActivity(i);
        finish();
    }
}