package com.example.expirytrack.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.expirytrack.BuildConfig;
import com.example.expirytrack.model.MenuSuggestion;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calls the Gemini REST API directly via HttpURLConnection.
 * Avoids SDK version bugs (MissingFieldException, model-not-found quirks).
 */
public class GeminiService {

    private static final String TAG = "GeminiService";
    private static final String MODEL = "gemini-2.5-flash";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
            + MODEL + ":generateContent?key=";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface GeminiCallback {
        void onSuccess(List<MenuSuggestion> menus);

        void onError(String errorMessage);
    }

    /**
     * Requests 3 menu suggestions from Gemini for the given near-expiry ingredient.
     */
    public void getMenuSuggestions(String ingredientName, int daysLeft,
            List<String> allActiveIngredients,
            GeminiCallback callback) {

        String prompt = "คุณเป็นเชฟผู้เชี่ยวชาญอาหารไทยและอาหารทั่วไป\n" +
                "วัตถุดิบหลักที่ต้องใช้ก่อนหมดอายุใน " + daysLeft + " วัน: " + ingredientName + "\n" +
                "วัตถุดิบอื่นที่มีในร้านตอนนี้: " + String.join(", ", allActiveIngredients) + "\n\n" +
                "แนะนำเมนูอาหาร 3 อย่างที่:\n" +
                "1. ใช้ " + ingredientName + " เป็นส่วนประกอบหลัก\n" +
                "2. ใช้วัตถุดิบที่มีในร้านให้มากที่สุด เพื่อลดการสูญเสีย\n" +
                "3. ทำได้จริงในครัวร้านอาหาร\n\n" +
                "ตอบในรูปแบบ JSON เท่านั้น ห้ามมีข้อความอื่น ดังนี้:\n" +
                "{\n" +
                "  \"menus\": [\n" +
                "    {\n" +
                "      \"name\": \"ชื่อเมนู\",\n" +
                "      \"ingredients\": [\"วัตถุดิบ1\", \"วัตถุดิบ2\"],\n" +
                "      \"ingredientsInStock\": [\"วัตถุดิบที่มีในร้านแล้ว\"],\n" +
                "      \"steps\": [\"ขั้นตอน1\", \"ขั้นตอน2\", \"ขั้นตอน3\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        executor.execute(() -> {
            try {
                String responseText = callGeminiApi(prompt);
                Log.d(TAG, "Gemini response: " + responseText);
                List<MenuSuggestion> menus = parseMenuSuggestions(responseText);
                mainHandler.post(() -> callback.onSuccess(menus));
            } catch (Exception e) {
                Log.e(TAG, "Gemini error: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("เกิดข้อผิดพลาด: " + e.getMessage()));
            }
        });
    }

    // -------------------------------------------------------------------------
    // HTTP call
    // -------------------------------------------------------------------------

    private String callGeminiApi(String prompt) throws Exception {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("GEMINI_API_KEY ไม่พบใน BuildConfig");
        }

        URL url = new URL(BASE_URL + apiKey);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);

        // Build request body
        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);

        JSONArray partsArray = new JSONArray();
        partsArray.put(textPart);

        JSONObject content = new JSONObject();
        content.put("parts", partsArray);

        JSONArray contentsArray = new JSONArray();
        contentsArray.put(content);

        JSONObject requestBody = new JSONObject();
        requestBody.put("contents", contentsArray);

        byte[] bodyBytes = requestBody.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
        }

        int responseCode = conn.getResponseCode();
        BufferedReader reader;
        if (responseCode == 200) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
            sb.append(line);
        reader.close();
        conn.disconnect();

        String body = sb.toString();

        if (responseCode != 200) {
            // Parse error message from response if possible
            try {
                JSONObject errJson = new JSONObject(body);
                String msg = errJson.getJSONObject("error").optString("message", "HTTP " + responseCode);
                throw new Exception(msg);
            } catch (Exception e2) {
                throw new Exception("HTTP " + responseCode + ": " + body);
            }
        }

        // Extract text from response
        JSONObject resp = new JSONObject(body);
        return resp.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");
    }

    // -------------------------------------------------------------------------
    // JSON parsing
    // -------------------------------------------------------------------------

    private List<MenuSuggestion> parseMenuSuggestions(String responseText) throws Exception {
        String jsonStr = responseText.trim();
        if (!jsonStr.startsWith("{")) {
            Pattern pattern = Pattern.compile("\\{[\\s\\S]*\\}");
            Matcher matcher = pattern.matcher(jsonStr);
            if (matcher.find()) {
                jsonStr = matcher.group();
            } else {
                throw new Exception("ไม่พบ JSON ใน response");
            }
        }

        JSONObject root = new JSONObject(jsonStr);
        JSONArray menusArray = root.getJSONArray("menus");
        List<MenuSuggestion> result = new ArrayList<>();

        for (int i = 0; i < menusArray.length(); i++) {
            JSONObject menuObj = menusArray.getJSONObject(i);
            String name = menuObj.optString("name", "เมนูที่ " + (i + 1));
            List<String> ingredients = jsonArrayToList(menuObj.optJSONArray("ingredients"));
            List<String> ingredientsInStock = jsonArrayToList(menuObj.optJSONArray("ingredientsInStock"));
            List<String> steps = jsonArrayToList(menuObj.optJSONArray("steps"));
            result.add(new MenuSuggestion(name, ingredients, ingredientsInStock, steps));
        }
        return result;
    }

    private List<String> jsonArrayToList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array == null)
            return list;
        for (int i = 0; i < array.length(); i++) {
            list.add(array.optString(i, ""));
        }
        return list;
    }
}
