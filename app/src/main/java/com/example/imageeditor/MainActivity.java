package com.example.imageeditor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;

    private Uri photoUri;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> storagePermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnSelectImage = findViewById(R.id.btnSelectImage);
        Button btnClickImage = findViewById(R.id.btnClickImage);

        // Initialize activity result launchers
        initializeLaunchers();

        // Apply entrance animations
        applyEntranceAnimations();

        // Select image from gallery
        btnSelectImage.setOnClickListener(v -> {
            animateButtonPress(v);
            if (checkStoragePermission()) {
                openGallery();
            } else {
                requestStoragePermission();
            }
        });

        // Capture image from camera
        btnClickImage.setOnClickListener(v -> {
            animateButtonPress(v);
            if (checkCameraPermission()) {
                openCamera();
            } else {
                requestCameraPermission();
            }
        });
    }

    private void initializeLaunchers() {
        // Gallery launcher
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            Bitmap bitmap = uriToBitmap(imageUri);
                            if (bitmap != null) {
                                openEditor(bitmap);
                            } else {
                                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        // Camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (photoUri != null) {
                            Bitmap bitmap = uriToBitmap(photoUri);
                            if (bitmap != null) {
                                openEditor(bitmap);
                            } else {
                                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        // Camera permission launcher
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                    }
                });

        // Storage permission launcher
        storagePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openGallery();
                    } else {
                        Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyEntranceAnimations() {
        View mainCard = findViewById(R.id.mainCard);
        View logoCard = findViewById(R.id.logoCard);
        View tvTitle = findViewById(R.id.tvTitle);
        View tvSubtitle = findViewById(R.id.tvSubtitle);
        View btnSelectImage = findViewById(R.id.btnSelectImage);
        View btnClickImage = findViewById(R.id.btnClickImage);
        View featureLayout = findViewById(R.id.featureLayout);

        // FIX: Use particle1 and particle2 (matching the XML IDs)
        View circle1 = findViewById(R.id.particle1);
        View circle2 = findViewById(R.id.particle2);

        // Animate background circles (only if views exist)
        if (circle1 != null) animateFloatingCircles(circle1, 3000, 50f);
        if (circle2 != null) animateFloatingCircles(circle2, 4000, 30f);

        // Card entrance animation
        mainCard.setAlpha(0f);
        mainCard.setScaleX(0.8f);
        mainCard.setScaleY(0.8f);
        mainCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setInterpolator(new android.view.animation.OvershootInterpolator())
                .start();

        // Logo pulse animation
        logoCard.setScaleX(0f);
        logoCard.setScaleY(0f);
        logoCard.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setStartDelay(200)
                .setInterpolator(new android.view.animation.BounceInterpolator())
                .start();

        // Title slide in
        tvTitle.setTranslationY(-50f);
        tvTitle.setAlpha(0f);
        tvTitle.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(400)
                .start();

        // Subtitle fade in
        tvSubtitle.setAlpha(0f);
        tvSubtitle.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(600)
                .start();

        // Buttons slide up
        animateSlideUp(btnSelectImage, 700, 50f);
        animateSlideUp(btnClickImage, 800, 50f);
        animateSlideUp(featureLayout, 900, 30f);
    }

    private void animateFloatingCircles(View view, int duration, float distance) {
        view.animate()
                .translationYBy(distance)
                .setDuration(duration)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    view.animate()
                            .translationYBy(-distance)
                            .setDuration(duration)
                            .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                            .withEndAction(() -> animateFloatingCircles(view, duration, distance))
                            .start();
                })
                .start();
    }

    private void animateSlideUp(View view, long delay, float distance) {
        view.setTranslationY(distance);
        view.setAlpha(0f);
        view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(delay)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    private void animateButtonPress(View view) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private boolean checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            storagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File photoFile = createImageFile();
        if (photoFile != null) {
            photoUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraLauncher.launch(intent);
        } else {
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp;
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);

        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap uriToBitmap(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }

            int maxSize = 2048;
            if (bitmap != null && (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize)) {
                float scale = Math.min(
                        (float) maxSize / bitmap.getWidth(),
                        (float) maxSize / bitmap.getHeight()
                );
                int newWidth = Math.round(bitmap.getWidth() * scale);
                int newHeight = Math.round(bitmap.getHeight() * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }

            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openEditor(Bitmap bitmap) {
        File tempFile = new File(getCacheDir(), "temp_image.jpg");
        try {
            FileOutputStream fos = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            Intent intent = new Intent(this, EditorActivity.class);
            intent.putExtra("imagePath", tempFile.getAbsolutePath());
            startActivity(intent);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }
}