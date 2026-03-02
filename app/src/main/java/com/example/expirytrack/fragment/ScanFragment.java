package com.example.expirytrack.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Fragment for scanning expiry dates using CameraX and ML Kit
 */
public class ScanFragment extends Fragment {
    private PreviewView previewView;
    private CameraOverlayView overlayView;
    private LinearLayout dateDetectionPopup;
    private TextView detectedDateText;
    private Button btnUseDetectedDate;
    private Button btnScanAgain;
    private Button btnManualEntry;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private TextRecognizer textRecognizer;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String restaurantId;
    private long lastDetectedDate = -1;

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
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        fetchRestaurantIdAndStartCamera();
        setupTextRecognizer();
        setupButtonListeners();
    }

    private void initializeViews(View view) {
        previewView = view.findViewById(R.id.previewView);
        overlayView = view.findViewById(R.id.overlayView);
        dateDetectionPopup = view.findViewById(R.id.dateDetectionPopup);
        detectedDateText = view.findViewById(R.id.detectedDateText);
        btnUseDetectedDate = view.findViewById(R.id.btnUseDetectedDate);
        btnScanAgain = view.findViewById(R.id.btnScanAgain);
        btnManualEntry = view.findViewById(R.id.btnManualEntry);
    }

    private void setupButtonListeners() {
        btnUseDetectedDate.setOnClickListener(v -> openAddIngredientDialog(lastDetectedDate));
        btnScanAgain.setOnClickListener(v -> hideDateDetectionPopup());
        btnManualEntry.setOnClickListener(v -> openAddIngredientDialog(System.currentTimeMillis()));
    }

    private void fetchRestaurantIdAndStartCamera() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (userId.isEmpty()) {
            return;
        }

        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                com.example.expirytrack.model.User user = documentSnapshot
                        .toObject(com.example.expirytrack.model.User.class);
                if (user != null && user.getRestaurantId() != null) {
                    restaurantId = user.getRestaurantId();
                    startCamera();
                }
            }
        });
    }

    private void setupTextRecognizer() {
        // Initialize ML Kit Text Recognizer with Latin script options
        TextRecognizerOptions options = new TextRecognizerOptions.Builder().build();
        textRecognizer = TextRecognition.getClient(options);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider
                .getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindImageAnalysis(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(requireContext()),
                image -> analyzeImage(image));

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void analyzeImage(androidx.camera.core.ImageProxy image) {
        android.media.Image mediaImage = image.getImage();
        if (mediaImage == null) {
            image.close();
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(
                mediaImage,
                image.getImageInfo().getRotationDegrees());

        textRecognizer.process(inputImage)
                .addOnSuccessListener(visionText -> {
                    processMlKitResults(visionText);
                    image.close();
                })
                .addOnFailureListener(e -> {
                    image.close();
                });
    }

    private void processMlKitResults(Text visionText) {
        StringBuilder fullText = new StringBuilder();
        List<Text.TextBlock> blocks = new ArrayList<>();

        for (Text.TextBlock block : visionText.getTextBlocks()) {
            fullText.append(block.getText()).append(" ");
            blocks.add(block);
        }

        overlayView.setTextBlocks(blocks);

        String detectedText = fullText.toString();
        if (!detectedText.isEmpty()) {
            DatePatternDetector.DateResult result = DatePatternDetector.detectDate(detectedText);
            if (result.found) {
                showDateDetectionPopup(result);
            }
        }
    }

    private void showDateDetectionPopup(DatePatternDetector.DateResult result) {
        lastDetectedDate = result.timestamp;
        detectedDateText.setText(result.displayText);
        dateDetectionPopup.setVisibility(View.VISIBLE);
    }

    private void hideDateDetectionPopup() {
        dateDetectionPopup.setVisibility(View.GONE);
    }

    private void openAddIngredientDialog(long expiryDate) {
        AddIngredientDialog dialog = AddIngredientDialog.newInstance(expiryDate, restaurantId);
        dialog.show(getChildFragmentManager(), "AddIngredient");
    }

    @Override
    public void onDestroyView() {
        if (textRecognizer != null) {
            try {
                textRecognizer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        super.onDestroyView();
    }
}
