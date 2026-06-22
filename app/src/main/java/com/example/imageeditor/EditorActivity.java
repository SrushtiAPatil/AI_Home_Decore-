package com.example.imageeditor;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditorActivity extends AppCompatActivity implements ObjectAdapter.OnObjectClickListener {

    private PhotoEditorView photoEditorView;
    private RecyclerView rvObjects, rvCategories;
    private Button btnPlace, btnExport, btnShare;
    private TextView tvCurrentCategory, tvCategoryCount, tvObjectCount;

    private Bitmap backgroundBitmap;
    private List<Integer> objectList;
    private List<ObjectCategory> categoryList;
    private ObjectAdapter objectAdapter;
    private CategoryAdapter categoryAdapter;

    private ActivityResultLauncher<String> storagePermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Initialize views
        photoEditorView = findViewById(R.id.photoEditorView);
        rvObjects = findViewById(R.id.rvObjects);
        rvCategories = findViewById(R.id.rvCategories);
        btnPlace = findViewById(R.id.btnPlace);
        btnExport = findViewById(R.id.btnExport);
        btnShare = findViewById(R.id.btnShare);
        tvCurrentCategory = findViewById(R.id.tvCurrentCategory);
        tvCategoryCount = findViewById(R.id.tvCategoryCount);
        tvObjectCount = findViewById(R.id.tvObjectCount);

        // Load background image
        String imagePath = getIntent().getStringExtra("imagePath");
        if (imagePath != null) {
            File file = new File(imagePath);
            backgroundBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (photoEditorView != null && backgroundBitmap != null) {
                photoEditorView.setBackgroundBitmap(backgroundBitmap);
            }
        }

        // Initialize categories and objects
        initializeCategories();

        // Setup RecyclerViews
        setupRecyclerViews();

        // Initialize permission launcher
        initializePermissionLauncher();

        // Apply entrance animations
        applyEditorAnimations();

        // Update category count
        if (categoryList != null && tvObjectCount != null) {
            tvObjectCount.setText(String.valueOf(categoryList.size()));
        }

        // Button listeners with animations
        btnPlace.setOnClickListener(v -> {
            animateButtonPress(v);
            placeObject();
        });

        btnExport.setOnClickListener(v -> {
            animateButtonPress(v);
            checkPermissionAndExport();
        });

        btnShare.setOnClickListener(v -> {
            animateButtonPress(v);
            shareImage();
        });

        // Back button
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        // Hint card auto-hide
        View hintCard = findViewById(R.id.hintCard);
        if (hintCard != null) {
            hintCard.postDelayed(() -> {
                hintCard.animate()
                        .alpha(0f)
                        .alpha(0f)
                        .translationX(100f)
                        .setDuration(500)
                        .withEndAction(() -> hintCard.setVisibility(View.GONE))
                        .start();
            }, 4000);
        }
    }

    private void initializeCategories() {
        categoryList = new ArrayList<>();

        // 🏠 HALL Category - Living Room Furniture
        List<Integer> hallObjects = new ArrayList<>();
        hallObjects.add(R.drawable.sofa);           // ✅ Your sofa PNG
        hallObjects.add(R.drawable.tv);             // ✅ Your TV PNG
        hallObjects.add(R.drawable.table);          // ✅ Your table PNG
        hallObjects.add(R.drawable.lamp);           // ✅ Your lamp PNG
        hallObjects.add(R.drawable.chair);          // ✅ Your chair PNG
        categoryList.add(new ObjectCategory("Hall", android.R.drawable.ic_menu_view, hallObjects));

        List<Integer> kitchenObjects = new ArrayList<>();
        kitchenObjects.add(R.drawable.refrigerator); // ✅ Your fridge PNG
        kitchenObjects.add(R.drawable.oven);         // ✅ Your oven PNG
        kitchenObjects.add(R.drawable.microwave);    // ✅ Your microwave PNG
        kitchenObjects.add(R.drawable.toaster);      // ✅ Your toaster PNG
        kitchenObjects.add(R.drawable.kettle);       // ✅ Your kettle PNG
        categoryList.add(new ObjectCategory("Kitchen", android.R.drawable.ic_menu_compass, kitchenObjects));

        // 🚿 BATHROOM Category - Bathroom Fixtures
        List<Integer> bathroomObjects = new ArrayList<>();
        bathroomObjects.add(R.drawable.bathtub);     // ✅ Your bathtub PNG
        bathroomObjects.add(R.drawable.shower);      // ✅ Your shower PNG
        bathroomObjects.add(R.drawable.sink);        // ✅ Your sink PNG
        bathroomObjects.add(R.drawable.toilet);      // ✅ Your toilet PNG
        bathroomObjects.add(R.drawable.mirror);      // ✅ Your mirror PNG
        categoryList.add(new ObjectCategory("Bathroom", android.R.drawable.ic_menu_info_details, bathroomObjects));

        // 💼 OFFICE Category - Office Equipment
        List<Integer> officeObjects = new ArrayList<>();
        officeObjects.add(R.drawable.desk);          // ✅ Your desk PNG
        officeObjects.add(R.drawable.office_chair);  // ✅ Your chair PNG
        officeObjects.add(R.drawable.computer);      // ✅ Your computer PNG
        officeObjects.add(R.drawable.printer);       // ✅ Your printer PNG
        officeObjects.add(R.drawable.bookshelf);     // ✅ Your bookshelf PNG
        categoryList.add(new ObjectCategory("Office", android.R.drawable.ic_menu_edit, officeObjects));

        // 📚 CLASS Category - Classroom Items
        List<Integer> classObjects = new ArrayList<>();
        classObjects.add(R.drawable.blackboard);     // ✅ Your blackboard PNG
        classObjects.add(R.drawable.student_desk);   // ✅ Your desk PNG
        classObjects.add(R.drawable.globe);          // ✅ Your globe PNG
        classObjects.add(R.drawable.projector);      // ✅ Your projector PNG
        classObjects.add(R.drawable.books);          // ✅ Your books PNG
        categoryList.add(new ObjectCategory("Class", android.R.drawable.ic_menu_agenda, classObjects));

        // 🌳 GROUND Category - Outdoor Objects
        List<Integer> groundObjects = new ArrayList<>();
        groundObjects.add(R.drawable.bench);         // ✅ Your bench PNG
        groundObjects.add(R.drawable.tree);          // ✅ Your tree PNG
        groundObjects.add(R.drawable.fountain);      // ✅ Your fountain PNG
        groundObjects.add(R.drawable.trash_bin);     // ✅ Your trash bin PNG
        groundObjects.add(R.drawable.street_lamp);   // ✅ Your lamp PNG
        categoryList.add(new ObjectCategory("Ground", android.R.drawable.ic_menu_mapmode, groundObjects));

        // Set first category as default
        if (!categoryList.isEmpty()) {
            objectList = new ArrayList<>(categoryList.get(0).getObjects());
        } else {
            objectList = new ArrayList<>();
        }
    }

    private void setupRecyclerViews() {
        if (categoryList == null || categoryList.isEmpty()) {
            Toast.makeText(this, "Error loading categories", Toast.LENGTH_SHORT).show();
            return;
        }

        // Categories RecyclerView
        LinearLayoutManager categoryLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvCategories.setLayoutManager(categoryLayoutManager);
        categoryAdapter = new CategoryAdapter(this, categoryList, (category, position) -> {
            objectList = new ArrayList<>(category.getObjects());
            if (objectAdapter != null) {
                objectAdapter.updateObjects(objectList);
            }

            if (tvCurrentCategory != null) {
                tvCurrentCategory.setText(category.getName() + " Objects");
            }
            if (tvCategoryCount != null) {
                tvCategoryCount.setText(objectList.size() + " items");
            }

            rvObjects.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction(() -> rvObjects.animate().alpha(1f).setDuration(150).start())
                    .start();
        });
        rvCategories.setAdapter(categoryAdapter);

        // Objects RecyclerView
        LinearLayoutManager objectLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvObjects.setLayoutManager(objectLayoutManager);
        objectAdapter = new ObjectAdapter(this, objectList, this);
        rvObjects.setAdapter(objectAdapter);

        // Set initial UI
        if (tvCurrentCategory != null) {
            tvCurrentCategory.setText(categoryList.get(0).getName() + " Objects");
        }
        if (tvCategoryCount != null) {
            tvCategoryCount.setText(objectList.size() + " items");
        }
    }

    private void initializePermissionLauncher() {
        storagePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        exportImage();
                    } else {
                        Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyEditorAnimations() {
        View topBar = findViewById(R.id.topBar);
        View editorCard = findViewById(R.id.editorCard);
        View layoutBottom = findViewById(R.id.layoutBottom);
        View hintCard = findViewById(R.id.hintCard);

        if (topBar != null) {
            topBar.setTranslationY(-topBar.getHeight());
            topBar.animate().translationY(0f).setDuration(500).start();
        }

        if (editorCard != null) {
            editorCard.setScaleX(0.9f);
            editorCard.setScaleY(0.9f);
            editorCard.setAlpha(0f);
            editorCard.animate().scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(600).setStartDelay(200).start();
        }

        if (layoutBottom != null) {
            layoutBottom.setTranslationY(layoutBottom.getHeight());
            layoutBottom.animate().translationY(0f).setDuration(600).setStartDelay(400).start();
        }

        if (hintCard != null) {
            hintCard.setAlpha(0f);
            hintCard.setScaleX(0.8f);
            hintCard.setScaleY(0.8f);
            hintCard.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(500).setStartDelay(800).start();
        }
    }

    private void animateButtonPress(View view) {
        view.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                .start();
    }

    @Override
    public void onObjectClick(int objectResId) {
        photoEditorView.addObject(objectResId);
        showCustomToast("Object added! ✨");
    }

    private void showCustomToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void placeObject() {
        btnPlace.setEnabled(false);
        btnExport.setEnabled(false);
        btnShare.setEnabled(false);

        showCustomToast("Processing... ⚙️");

        new Thread(() -> {
            Bitmap processedBitmap = applyFiltersAndMerge();

            runOnUiThread(() -> {
                if (photoEditorView != null && processedBitmap != null) {
                    photoEditorView.setBackgroundBitmap(processedBitmap);
                    photoEditorView.clearObjects();
                }

                View editorCard = findViewById(R.id.editorCard);
                if (editorCard != null) {
                    editorCard.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150)
                            .withEndAction(() -> editorCard.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
                            .start();
                }

                btnPlace.setEnabled(true);
                btnExport.setEnabled(true);
                btnShare.setEnabled(true);

                showCustomToast("✨ Placed realistically! ✨");
            });
        }).start();
    }

    private Bitmap applyFiltersAndMerge() {
        Bitmap canvasBitmap = photoEditorView.getCurrentBitmap();
        if (canvasBitmap == null) return null;

        Bitmap resultBitmap = canvasBitmap.copy(Bitmap.Config.ARGB_8888, true);
        return resultBitmap;
    }

    private void checkPermissionAndExport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportImage();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                exportImage();
            } else {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private void exportImage() {
        Bitmap finalBitmap = photoEditorView.getCurrentBitmap();
        if (finalBitmap == null) {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "IMG_EDITED_" + timeStamp + ".jpg";

            Uri imageUri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

                imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (imageUri != null) {
                    OutputStream outputStream = getContentResolver().openOutputStream(imageUri);
                    if (outputStream != null) {
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                        outputStream.close();
                    }
                }
            } else {
                File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File imageFile = new File(picturesDir, fileName);
                FileOutputStream fos = new FileOutputStream(imageFile);
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                fos.close();

                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
                imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            }

            showSuccessDialog();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Success!")
                .setMessage("Image saved to gallery")
                .setPositiveButton("OK", null)
                .show();
    }

    private void shareImage() {
        Bitmap finalBitmap = photoEditorView.getCurrentBitmap();
        if (finalBitmap == null) return;

        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            FileOutputStream fos = new FileOutputStream(new File(cachePath, "shared_image.jpg"));
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
            fos.close();

            File imagePath = new File(getCacheDir(), "images");
            File newFile = new File(imagePath, "shared_image.jpg");
            Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    newFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/jpeg");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Image"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error sharing", Toast.LENGTH_SHORT).show();
        }
    }
}