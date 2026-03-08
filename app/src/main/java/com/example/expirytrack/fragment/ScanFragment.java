package com.example.expirytrack.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.expirytrack.R;
import com.example.expirytrack.util.CameraOverlayView;
import com.example.expirytrack.util.DatePatternDetector;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * ScanFragment — OCR camera scanner + gallery image upload.
 *
 * Two modes:
 *   1. LIVE MODE  — CameraX real-time preview analyses frames continuously.
 *   2. UPLOAD MODE — User picks an image from gallery; still image is run through
 *                    the same ML Kit text recogniser.
 */
public class ScanFragment extends Fragment {

    private static final int REQUEST_CAMERA_PERMISSION = 200;

    // ── Views ──
    private PreviewView previewView;
    private ImageView uploadedImagePreview;
    private CameraOverlayView overlayView;
    private View dateDetectionPopup;       // MaterialCardView in XML — use View to avoid cast
    private LinearLayout processingIndicator;
    private TextView detectedDateText;
    private Button btnUseDetectedDate;
    private Button btnScanAgain;
    private Button btnManualEntry;
    private Button btnUploadImage;

    // ── Camera / OCR ──
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private TextRecognizer textRecognizer;

    // ── Firebase ──
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String restaurantId;

    private long lastDetectedDate = -1;

    // ── Gallery picker launcher ──
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            processGalleryImage(uri);
                        }
                    });

    // ────────────────────────────── LIFECYCLE ──────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupTextRecognizer();
        setupButtonListeners();
        checkCameraPermission();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        closeTextRecognizer();
        if (cameraProvider != null) cameraProvider.unbindAll();
    }

    // ────────────────────────────── INIT ──────────────────────────────────────

    private void initializeViews(View view) {
        previewView          = view.findViewById(R.id.previewView);
        uploadedImagePreview = view.findViewById(R.id.uploadedImagePreview);
        overlayView          = view.findViewById(R.id.overlayView);
        dateDetectionPopup   = view.findViewById(R.id.dateDetectionPopup);
        processingIndicator  = view.findViewById(R.id.processingIndicator);
        detectedDateText     = view.findViewById(R.id.detectedDateText);
        btnUseDetectedDate   = view.findViewById(R.id.btnUseDetectedDate);
        btnScanAgain         = view.findViewById(R.id.btnScanAgain);
        btnManualEntry       = view.findViewById(R.id.btnManualEntry);
        btnUploadImage       = view.findViewById(R.id.btnUploadImage);
    }

    private void setupTextRecognizer() {
        textRecognizer = TextRecognition.getClient(new TextRecognizerOptions.Builder().build());
    }

    // ────────────────────────────── BUTTON LISTENERS ──────────────────────────

    private void setupButtonListeners() {
        if (btnUseDetectedDate != null) {
            btnUseDetectedDate.setOnClickListener(v -> {
                if (lastDetectedDate != -1) {
                    hideDateDetectionPopup();
                    openAddIngredientDialog(lastDetectedDate);
                }
            });
        }

        if (btnScanAgain != null) {
            btnScanAgain.setOnClickListener(v -> {
                hideDateDetectionPopup();
                switchToLiveMode();
            });
        }

        if (btnManualEntry != null) {
            btnManualEntry.setOnClickListener(v -> {
                hideDateDetectionPopup();
                long defaultDate = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);
                openAddIngredientDialog(defaultDate);
            });
        }

        if (btnUploadImage != null) {
            btnUploadImage.setOnClickListener(v -> openGallery());
        }
    }

    // ────────────────────────────── GALLERY / IMAGE UPLOAD ────────────────────

    /** Open the system gallery picker */
    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    /**
     * Called when a URI is returned from the gallery.
     * Pauses the live camera, shows the selected image, runs OCR.
     */
    private void processGalleryImage(Uri uri) {
        // Switch to image-preview mode: hide live preview, show still image
        if (previewView != null)          previewView.setVisibility(View.GONE);
        if (overlayView  != null)         overlayView.setVisibility(View.GONE);
        if (uploadedImagePreview != null) {
            uploadedImagePreview.setImageURI(uri);
            uploadedImagePreview.setVisibility(View.VISIBLE);
        }

        // Stop live camera to save resources
        if (cameraProvider != null) cameraProvider.unbindAll();

        // Show processing spinner
        showProcessing(true);

        // Build InputImage from URI and run ML Kit
        InputImage image;
        try {
            image = InputImage.fromFilePath(requireContext(), uri);
        } catch (IOException e) {
            showProcessing(false);
            Toast.makeText(getContext(), "ไม่สามารถโหลดรูปภาพได้: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            return;
        }

        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    showProcessing(false);
                    processOcrText(visionText);
                })
                .addOnFailureListener(e -> {
                    showProcessing(false);
                    Toast.makeText(getContext(), "OCR ล้มเหลว: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /** Return to live-camera mode from image-preview mode */
    private void switchToLiveMode() {
        if (uploadedImagePreview != null) {
            uploadedImagePreview.setVisibility(View.GONE);
            uploadedImagePreview.setImageURI(null);
        }
        if (previewView != null) previewView.setVisibility(View.VISIBLE);
        if (overlayView  != null) overlayView.setVisibility(View.VISIBLE);

        // Restart camera
        if (restaurantId != null) startCamera();
    }

    // ────────────────────────────── CAMERA PERMISSION ─────────────────────────

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            fetchRestaurantIdAndStartCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchRestaurantIdAndStartCamera();
            } else {
                Toast.makeText(getContext(),
                        "กรุณาอนุญาตให้เข้าถึงกล้องเพื่อใช้งานสแกน", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ────────────────────────────── CAMERA SETUP ──────────────────────────────

    private void fetchRestaurantIdAndStartCamera() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (userId.isEmpty()) {
            Toast.makeText(getContext(), "กรุณาเข้าสู่ระบบก่อนใช้งานสแกน", Toast.LENGTH_LONG).show();
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        com.example.expirytrack.model.User user =
                                doc.toObject(com.example.expirytrack.model.User.class);
                        if (user != null && user.getRestaurantId() != null) {
                            restaurantId = user.getRestaurantId();
                            startCamera();
                        } else {
                            Toast.makeText(getContext(), "ไม่พบข้อมูลร้านอาหาร", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "ไม่สามารถเชื่อมต่อฐานข้อมูลได้", Toast.LENGTH_LONG).show());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());

        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindImageAnalysis(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider provider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()),
                this::analyzeImage);

        try {
            provider.unbindAll();
            camera = provider.bindToLifecycle(this, selector, preview, analysis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ────────────────────────────── IMAGE ANALYSIS (LIVE) ─────────────────────

    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void analyzeImage(androidx.camera.core.ImageProxy image) {
        android.media.Image mediaImage = image.getImage();
        if (mediaImage == null) { image.close(); return; }

        InputImage inputImage = InputImage.fromMediaImage(
                mediaImage, image.getImageInfo().getRotationDegrees());

        textRecognizer.process(inputImage)
                .addOnSuccessListener(visionText -> {
                    processOcrText(visionText);
                    image.close();
                })
                .addOnFailureListener(e -> image.close());
    }

    // ────────────────────────────── OCR PROCESSING ────────────────────────────

    /** Shared logic for both live and upload modes */
    private void processOcrText(Text visionText) {
        StringBuilder fullText = new StringBuilder();
        List<Text.TextBlock> blocks = new ArrayList<>();

        for (Text.TextBlock block : visionText.getTextBlocks()) {
            fullText.append(block.getText()).append(" ");
            blocks.add(block);
        }

        // Update overlay (only meaningful in live mode)
        if (overlayView != null && overlayView.getVisibility() == View.VISIBLE) {
            overlayView.setTextBlocks(blocks);
        }

        String text = fullText.toString().trim();
        if (!text.isEmpty()) {
            DatePatternDetector.DateResult result = DatePatternDetector.detectDate(text);
            if (result.found) {
                showDateDetectionPopup(result);
            } else {
                // Only show "not found" toast in upload mode (not every live frame)
                if (uploadedImagePreview != null &&
                        uploadedImagePreview.getVisibility() == View.VISIBLE) {
                    Toast.makeText(getContext(),
                            "ไม่พบวันที่ในรูปภาพ กรุณาลองรูปอื่นหรือกรอกเอง",
                            Toast.LENGTH_LONG).show();
                }
            }
        } else {
            if (uploadedImagePreview != null &&
                    uploadedImagePreview.getVisibility() == View.VISIBLE) {
                Toast.makeText(getContext(),
                        "ไม่พบข้อความในรูปภาพ", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ────────────────────────────── POPUP / DIALOG ────────────────────────────

    private void showDateDetectionPopup(DatePatternDetector.DateResult result) {
        lastDetectedDate = result.timestamp;
        if (detectedDateText != null)
            detectedDateText.setText("พบวันหมดอายุ: " + result.displayText);
        if (dateDetectionPopup != null)
            dateDetectionPopup.setVisibility(View.VISIBLE);
    }

    private void hideDateDetectionPopup() {
        if (dateDetectionPopup != null)
            dateDetectionPopup.setVisibility(View.GONE);
        lastDetectedDate = -1;
    }

    private void openAddIngredientDialog(long expiryDate) {
        AddIngredientDialog dialog = AddIngredientDialog.newInstance(expiryDate, restaurantId);
        dialog.show(getChildFragmentManager(), "AddIngredient");
    }

    // ────────────────────────────── HELPERS ───────────────────────────────────

    private void showProcessing(boolean visible) {
        if (processingIndicator != null)
            processingIndicator.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void closeTextRecognizer() {
        if (textRecognizer != null) {
            try { textRecognizer.close(); } catch (Exception ignored) {}
        }
    }
}
