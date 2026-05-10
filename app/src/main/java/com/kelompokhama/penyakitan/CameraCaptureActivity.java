package com.kelompokhama.penyakitan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.core.content.FileProvider;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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

    // Ubah angka ini kalau mau kualitas berbeda.
    // 90 = kualitas lebih tinggi, ukuran lebih besar.
    // 85 = seimbang.
    // 75 = ukuran lebih kecil.
    private static final int JPEG_UPLOAD_QUALITY = 85;

    private ImageView imgUploadPreview;
    private ProgressBar progressUpload;
    private TextView tvUploadStatus;

    private ActivityResultLauncher<String> permissionLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    private Uri capturedImageUri;
    private File capturedImageFile;
    private String localFilename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCameraFullResolution();
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
                    if (result.getResultCode() == RESULT_OK) {
                        if (capturedImageFile != null && capturedImageFile.exists()) {
                            showUploadScreen(capturedImageUri);
                            uploadToCloud(capturedImageFile);
                        } else {
                            Toast.makeText(
                                    this,
                                    "File gambar tidak ditemukan",
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

                        deleteTempImageIfExists();
                        finish();
                    }
                }
        );

        checkCameraPermission();
    }

    private void showUploadScreen(Uri imageUri) {
        setContentView(R.layout.activity_camera_capture);

        imgUploadPreview = findViewById(R.id.imgUploadPreview);
        progressUpload = findViewById(R.id.progressUpload);
        tvUploadStatus = findViewById(R.id.tvUploadStatus);

        imgUploadPreview.setImageURI(imageUri);
        progressUpload.setIndeterminate(true);
        tvUploadStatus.setText("Upload foto sedang diproses...");
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCameraFullResolution();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCameraFullResolution() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                capturedImageFile = createImageFile();

                capturedImageUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        capturedImageFile
                );

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                cameraLauncher.launch(takePictureIntent);

            } catch (Exception e) {
                Log.e("CAMERA_ERROR", "Gagal membuka kamera: " + e.getMessage());

                Toast.makeText(
                        this,
                        "Gagal membuka kamera: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();

                finish();
            }
        } else {
            Toast.makeText(
                    this,
                    "Aplikasi kamera tidak ditemukan",
                    Toast.LENGTH_LONG
            ).show();
            finish();
        }
    }

    private File createImageFile() {
        long uploadedAt = System.currentTimeMillis() / 1000;
        localFilename = "HP_" + uploadedAt + ".jpg";

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (storageDir == null) {
            storageDir = getFilesDir();
        }

        return new File(storageDir, localFilename);
    }

    private void uploadToCloud(File imageFile) {
        new Thread(() -> {
            HttpURLConnection conn = null;

            try {
                runOnUiThread(() -> {
                    if (tvUploadStatus != null) {
                        tvUploadStatus.setText("Mengompres foto untuk upload...");
                    }
                });

                long uploadedAt = System.currentTimeMillis() / 1000;

                if (localFilename == null || localFilename.trim().isEmpty()) {
                    localFilename = imageFile.getName();
                }

                long originalSize = imageFile.length();

                Log.d("IMAGE_COMPRESS", "Original filename: " + localFilename);
                Log.d("IMAGE_COMPRESS", "Original size bytes: " + originalSize);
                Log.d("IMAGE_COMPRESS", "Original size KB: " + (originalSize / 1024));

                compressImageFileInPlace(imageFile, JPEG_UPLOAD_QUALITY);

                long compressedSize = imageFile.length();

                Log.d("IMAGE_COMPRESS", "Compressed quality: " + JPEG_UPLOAD_QUALITY);
                Log.d("IMAGE_COMPRESS", "Compressed size bytes: " + compressedSize);
                Log.d("IMAGE_COMPRESS", "Compressed size KB: " + (compressedSize / 1024));

                runOnUiThread(() -> {
                    if (tvUploadStatus != null) {
                        tvUploadStatus.setText("Mengirim foto ke server...");
                    }
                });

                byte[] imageBytes = readFileBytes(imageFile);

                String boundary = "----Boundary123456789";
                URL url = new URL(functionUrl);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);

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
                Uri finalImageUri = capturedImageUri;

                runOnUiThread(() -> {
                    if (tvUploadStatus != null) {
                        if (finalResponseCode == 200 || finalResponseCode == 201) {
                            tvUploadStatus.setText("Upload berhasil, membuka hasil...");
                        } else {
                            tvUploadStatus.setText("Upload gagal, membuka hasil...");
                        }
                    }

                    openDetectionResult(finalImageUri, finalResponseCode, finalResponseMessage);
                });

            } catch (Exception e) {
                Log.e("UPLOAD_ERROR", e.toString());

                Uri finalImageUri = capturedImageUri;

                runOnUiThread(() -> {
                    if (tvUploadStatus != null) {
                        tvUploadStatus.setText("Terjadi error, membuka hasil...");
                    }

                    openDetectionResult(finalImageUri, 0, e.getMessage());
                });

            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private void compressImageFileInPlace(File imageFile, int quality) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bitmap = BitmapFactory.decodeFile(
                imageFile.getAbsolutePath(),
                options
        );

        if (bitmap == null) {
            throw new Exception("Gagal decode gambar untuk kompresi");
        }

        FileOutputStream fos = new FileOutputStream(imageFile, false);

        bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                quality,
                fos
        );

        fos.flush();
        fos.close();

        bitmap.recycle();
    }

    private byte[] readFileBytes(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);

        byte[] bytes = new byte[(int) file.length()];

        int totalRead = 0;
        int read;

        while (totalRead < bytes.length &&
                (read = fis.read(bytes, totalRead, bytes.length - totalRead)) != -1) {
            totalRead += read;
        }

        fis.close();

        return bytes;
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
        data.put("source", "HP");
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

    private void openDetectionResult(Uri imageUri, int responseCode, String responseMessage) {
        Intent intent = new Intent(
                CameraCaptureActivity.this,
                DetectionResultActivity.class
        );

        if (imageUri != null) {
            intent.putExtra("image_uri", imageUri.toString());
        }

        intent.putExtra("responseCode", responseCode);
        intent.putExtra("responseMessage", responseMessage);

        startActivity(intent);
        finish();
    }

    private void deleteTempImageIfExists() {
        try {
            if (capturedImageFile != null && capturedImageFile.exists()) {
                capturedImageFile.delete();
            }
        } catch (Exception e) {
            Log.e("DELETE_TEMP", "Gagal hapus file: " + e.getMessage());
        }
    }
}