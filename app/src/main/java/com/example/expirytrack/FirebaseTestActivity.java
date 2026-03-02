package com.example.expirytrack;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class FirebaseTestActivity extends AppCompatActivity {

    private static final String TAG = "FirebaseTest";
    private TextView resultText;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_test);

        resultText = findViewById(R.id.result_text);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loginAndTest();
    }

    private void loginAndTest() {

        updateResult("Logging in with Email/Password...");

        auth.signInWithEmailAndPassword("test@gmail.com", "123456")
                .addOnSuccessListener(authResult -> {
                    updateResult("✅ Login Success");
                    writeTestData();
                })
                .addOnFailureListener(e -> {
                    updateResult("❌ Login Failed: " + e.getMessage());
                });
    }

    private void writeTestData() {

        updateResult("Testing Firestore Write...");

        HashMap<String, Object> data = new HashMap<>();
        data.put("message", "Firebase Connected!");
        data.put("timestamp", System.currentTimeMillis());

        db.collection("test").document("connection_test")
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Firestore Write Success");
                    updateResult("✅ Write Success");
                    testRead();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Write Failed: " + e.getMessage());
                    updateResult("❌ Write Failed: " + e.getMessage());
                });
    }

    private void testRead() {

        db.collection("test").document("connection_test")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "✅ Firestore Read Success");
                        updateResult("✅ Read Success");
                        updateResult("🎉 Firebase Setup Complete!");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Read Failed: " + e.getMessage());
                    updateResult("❌ Read Failed: " + e.getMessage());
                });
    }

    private void updateResult(String text) {
        runOnUiThread(() -> resultText.append(text + "\n"));
    }
}