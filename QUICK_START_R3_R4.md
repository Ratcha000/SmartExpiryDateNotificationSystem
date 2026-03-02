# 🚀 Quick Start Guide — Round 3 & 4

## 📋 What's New

### Round 3: OCR Camera Scanner
1. **ScanFragment** — Real-time camera scanning with ML Kit
2. **DatePatternDetector** — Detects multiple date formats
3. **AddIngredientDialog** — Add ingredients from scan results
4. **CameraOverlayView** — Shows bounding boxes for detected text

### Round 4: Push Notifications
1. **ExpiryCheckWorker** — Daily background task
2. **NotificationHelper** — Creates notification channel
3. **WorkManager** — Schedules daily checks at 8:00 AM

---

## 🔧 Installation & Setup

### Step 1: Update Android Manifest
✅ Already added:
- `<uses-permission android:name="android.permission.CAMERA" />`

### Step 2: Build gradle
✅ Already added:
- `androidx.work:work-runtime:2.8.1`
- All CameraX and ML Kit dependencies

### Step 3: Runtime Permissions
The app will request CAMERA permission on first ScanFragment open (Android 6+).

---

## 🎬 How to Use

### Scanning Expiry Dates
1. **Open App** → Login → EmployeeMainActivity
2. **Click "📷 สแกน"** tab
3. **Point camera** at expiry date label
4. **Wait** for date detection (green popup appears)
5. **Click "✓ ใช้"** to use detected date
   - OR "🔄 สแกนใหม่" to scan again
   - OR "✏️ กรอกเอง" for manual entry

### Add Ingredient Dialog
- **Name**: Required field
- **Expiry Date**: Pre-filled from scan (can edit)
- **Category**: Choose from 7 categories
- **Notify Days**: 1-14 days (default 3)
- **Save**: Adds to Firestore

### Notifications
- **Automatically runs daily** at any time (WorkManager handles)
- **Check in Settings → Notifications → Expiry**
- See "⚠️ วัตถุดิบใกล้หมดอายุ" alerts

---

## 📱 Testing

### Test OCR Detection
```bash
# Create test ingredients with near-future expiry dates
adb shell setprop debug.firebase.analytics.app_startup_timeout 30000

# Or manually add in Firestore:
- expiryDate: March 5, 2026 (today + 2 days)
- notifyDaysBefore: 3
- status: "active"
```

### Test Notifications
```bash
# Force WorkManager execution
adb shell cmd jobscheduler run --force com.example.expirytrack 1

# Or wait 24 hours for automatic execution
```

### Check Logs
```bash
# View WorkManager logs
adb logcat | grep WorkManager

# View ML Kit logs
adb logcat | grep "mlkit"

# View notification logs
adb logcat | grep "notification"
```

---

## 🐛 Common Issues

### 1. Camera Preview Not Showing
- **Issue**: Camera permission not granted
- **Fix**: 
  ```
  Settings → Apps → [App] → Permissions → Camera → Allow
  ```

### 2. Text Not Detected
- **Issues**: 
  - Image too blurry (ML Kit needs clear images)
  - Date label too small
  - Wrong angle
- **Fix**: 
  - Hold camera steady
  - Keep label in frame center
  - Ensure good lighting

### 3. Notifications Not Showing
- **Issue**: Notification channel not created
- **Fix**: 
  - Open MainActivity or EmployeeMainActivity first
  - Wait 24 hours for WorkManager, or force run:
  ```bash
  adb shell cmd jobscheduler run --force com.example.expirytrack 1
  ```

### 4. App Crashes on Camera
- **Issue**: Permission not properly handled
- **Fix**: Implement permission request:
  ```java
  // Already handled in ScanFragment
  // But ensure API level >= 21
  ```

---

## 🔍 Code Structure

```
📁 com/example/expirytrack/
├── 📁 fragment/
│   ├── ScanFragment.java ✨ (CameraX + ML Kit)
│   ├── AddIngredientDialog.java ✨ (New dialog)
│   ├── IngredientListFragment.java (existing)
│   └── EditIngredientDialog.java (existing)
├── 📁 util/
│   ├── DatePatternDetector.java ✨ (Date regex)
│   ├── CameraOverlayView.java ✨ (Bounding boxes)
│   └── NotificationHelper.java ✨ (Notification mgmt)
├── 📁 worker/
│   └── ExpiryCheckWorker.java ✨ (Daily task)
├── 📁 activity/
│   └── EmployeeMainActivity.java 🔄 (Added WorkManager)
└── MainActivity.java 🔄 (Added WorkManager)

📁 res/layout/
├── fragment_scan.xml 🔄 (CameraX + overlays)
└── dialog_add_ingredient.xml ✨ (New)
```

---

## 🎯 Key Methods

### DatePatternDetector
```java
DatePatternDetector.DateResult result = 
    DatePatternDetector.detectDate("Exp: 15/03/2025");

if (result.found) {
    long timestamp = result.timestamp;  // ms since epoch
    String display = result.displayText; // "15/03/2025"
    int start = result.startIndex;       // position in text
    int end = result.endIndex;
}
```

### ScanFragment
```java
// User scans → DateDetected → AddIngredientDialog opens
AddIngredientDialog.newInstance(expiryDate, restaurantId);

// Dialog saves → Ingredient added to Firestore
ingredient.setScannedBy(userId);
ingredient.setScannedAt(System.currentTimeMillis());
db.collection("ingredients").add(ingredient);
```

### ExpiryCheckWorker
```java
// Runs daily via WorkManager
// Calculates daysLeft for each active ingredient
long daysLeft = calculateDaysUntilExpiry(ingredientExpiryDate);

if (daysLeft < 0) {
    // Send expired notification
} else if (daysLeft <= ingredient.getNotifyDaysBefore()) {
    // Send expiring notification
}
```

---

## 📚 Dependencies Used

| Dependency | Version | Purpose |
|-----------|---------|---------|
| CameraX | 1.3.0 | Real-time camera |
| ML Kit Text Recognition | 16.0.0 | OCR |
| WorkManager | 2.8.1 | Scheduled tasks |
| Material Design 3 | 1.11.0 | UI components |

---

## 🔐 Permissions

| Permission | Purpose | Level |
|-----------|---------|-------|
| CAMERA | Access camera for scanning | Runtime (API 23+) |

no Internet permission needed (uses local ML Kit)

---

## ✅ Verification Checklist

Before considering Round 3 & 4 complete:

- [ ] App builds without errors
- [ ] ScanFragment opens camera without crashing
- [ ] OCR detects dates from camera feed
- [ ] AddIngredientDialog saves to Firestore
- [ ] Notification channel created (check in Settings)
- [ ] WorkManager scheduled (check with adb)
- [ ] No null pointer exceptions in logs
- [ ] All dependencies resolved

---

## Next Phase: Round 5

Ready to implement:
1. ManagerMainActivity with Dashboard
2. Advanced ingredient filtering
3. Usage history viewer
4. Notifications for Managers

---

**Last Updated**: March 2, 2026  
**Status**: ✅ Ready for Testing
