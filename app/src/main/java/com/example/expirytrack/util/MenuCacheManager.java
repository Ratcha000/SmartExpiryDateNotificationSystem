package com.example.expirytrack.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.expirytrack.model.MenuSuggestion;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages caching of menu suggestions with timestamp validation.
 * Caches are TTL-based (Time-To-Live).
 */
public class MenuCacheManager {
    private static final String TAG = "MenuCacheManager";
    private static final String PREF_NAME = "menu_cache";
    private static final long CACHE_TTL_MS = 1000 * 60 * 60 * 24; // 24 hours

    private final SharedPreferences prefs;
    private final Gson gson;

    public MenuCacheManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * Generate cache key for an ingredient
     */
    private String getCacheKey(String ingredientName) {
        return "menu_" + ingredientName.hashCode();
    }

    private String getTimestampKey(String ingredientName) {
        return "ts_" + ingredientName.hashCode();
    }

    /**
     * Get cached menus, return null if expired or not found
     */
    public List<MenuSuggestion> getMenusFromCache(String ingredientName) {
        String key = getCacheKey(ingredientName);
        String timestampKey = getTimestampKey(ingredientName);

        if (!prefs.contains(key) || !prefs.contains(timestampKey)) {
            return null;
        }

        long cachedTime = prefs.getLong(timestampKey, 0);
        long now = System.currentTimeMillis();

        if (now - cachedTime > CACHE_TTL_MS) {
            // Cache expired
            clearMenuCache(ingredientName);
            return null;
        }

        try {
            String json = prefs.getString(key, "");
            Type listType = new TypeToken<ArrayList<MenuSuggestion>>() {
            }.getType();
            List<MenuSuggestion> menus = gson.fromJson(json, listType);
            Log.d(TAG, "Loaded " + (menus != null ? menus.size() : 0) +
                    " menus from cache for " + ingredientName);
            return menus;
        } catch (Exception e) {
            Log.e(TAG, "Error deserializing cached menus", e);
            clearMenuCache(ingredientName);
            return null;
        }
    }

    /**
     * Save menus to cache
     */
    public void saveMenusToCache(String ingredientName, List<MenuSuggestion> menus) {
        try {
            String key = getCacheKey(ingredientName);
            String timestampKey = getTimestampKey(ingredientName);

            String json = gson.toJson(menus);
            prefs.edit()
                    .putString(key, json)
                    .putLong(timestampKey, System.currentTimeMillis())
                    .apply();

            Log.d(TAG, "Saved " + menus.size() + " menus to cache for " + ingredientName);
        } catch (Exception e) {
            Log.e(TAG, "Error saving menus to cache", e);
        }
    }

    /**
     * Clear cache for a specific ingredient
     */
    public void clearMenuCache(String ingredientName) {
        String key = getCacheKey(ingredientName);
        String timestampKey = getTimestampKey(ingredientName);
        prefs.edit()
                .remove(key)
                .remove(timestampKey)
                .apply();
        Log.d(TAG, "Cleared cache for " + ingredientName);
    }

    /**
     * Clear all menu caches
     */
    public void clearAllMenuCache() {
        prefs.edit().clear().apply();
        Log.d(TAG, "Cleared all menu cache");
    }
}
