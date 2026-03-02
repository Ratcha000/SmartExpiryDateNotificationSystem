# Round 3 & 4 Implementation Summary

## 🎯 Overview
Successfully implemented OCR Camera Scanner (Round 3) and Push Notification system (Round 4).

---

## 🔴 ROUND 3 — OCR Camera Scanner ✅

### 1. **Dependencies & Permissions**
- ✅ Added `androidx.work:work-runtime:2.8.1` for WorkManager
- ✅ Added `android.permission.CAMERA` to AndroidManifest.xml
- ✅ CameraX, ML Kit dependencies already present from Round 1

### 2. **DatePatternDetector.java** — Utility Class
**File**: `util/DatePatternDetector.java`

**Features**:
- Detects multiple date formats:
  - `DD/MM/YYYY` or `DD/MM/YY`
  - `DD-MM-YYYY`
  - `MM/YYYY` (sets day to last day of month)
  - `YYYY-MM-DD` (ISO format)
- Detects date keywords: `EXP:`, `BBF:`, `หมดอายุ:`
- Returns `DateResult` with `timestamp`, `displayText`, and position in text
- Validates leap years and month lengths
- Handles 2-digit years (00-29 → 2000-2029, 30-99 → 1930-1999)

```java
DatePatternDetector.DateResult result = DatePatternDetector.detectDate(ocrText);
if (result.found) {
    long expiryTimestamp = result.timestamp;
    String formattedDate = result.displayText; // e.g., "15/03/2025"
}
```

### 3. **CameraOverlayView.java** — Custom View
**File**: `util/CameraOverlayView.java`

**Features**:
- Custom View that draws bounding boxes around detected text
- Receives `List<Text.TextBlock>` from ML Kit
- Draws green borders around recognized text
- Used in real-time preview to highlight detected dates

### 4. **ScanFragment.java** — Complete Rewrite
**File**: `fragment/ScanFragment.java`

**Features**:
- **CameraX Integration**:
  - Real-time preview using `PreviewView`
  - `ImageAnalysis` for continuous frame processing
  - Back camera (LENS_FACING_BACK)
  - `STRATEGY_KEEP_ONLY_LATEST` for performance

- **ML Kit Text Recognition**:
  - Uses on-device TextRecognizer (Latin languages)
  - Processes each frame for text detection
  - Returns bounding boxes and recognized text

- **Date Detection Flow**:
  1. Process image frame → ML Kit → Extract text
  2. Send text to `DatePatternDetector`
  3. If date found → Show green popup at bottom
  4. User can use detected date or scan again

- **UI Components**:
  - `PreviewView`: Camera preview
  - `CameraOverlayView`: Bounding boxes
  - Bottom panel with buttons: "🔄 สแกนใหม่", "✏️ กรอกเอง"
  - Green popup showing detected date

### 5. **AddIngredientDialog.java** — New Dialog
**File**: `fragment/AddIngredientDialog.java`

**Features**:
- Opens after successful OCR scan (or manual entry)
- Auto-fills `expiryDate` from OCR (can be edited)
- Form fields:
  - Name (EditText) - **Required**
  - Expiry Date Display + Edit Button (DatePicker)
  - Category (Spinner) - 7 options
  - Notify Days Before (NumberPicker) - 1-14 days, default 3
- Saves to Firestore with:
  - `scannedBy`: User ID
  - `scannedAt`: Current timestamp
  - `updatedBy`: User ID
- Handles Firestore save with error handling

### 6. **Layout Files**

#### fragment_scan.xml
- RelativeLayout with CameraX PreviewView
- CameraOverlayView for bounding boxes
- Bottom control panel (dark background)
- Top green popup for date detection
- Two buttons: "Scan Again" & "Manual Entry"

#### dialog_add_ingredient.xml
- TextInputLayout for ingredient name
- Date validation section (displays scanned date)
- Spinner for categories
- NumberPicker for notification days
- Save/Cancel buttons

---

## 🟠 ROUND 4 — Push Notifications ✅

### 1. **NotificationHelper.java** — Notification Management
**File**: `util/NotificationHelper.java`

**Features**:
- **Channel Management**:
  - Creates notification channel "expiry_alerts"
  - Importance: HIGH (urgent alerts)
  - Enables lights and vibration
  - Description: "แจ้งเตือนเมื่อวัตถุดิบใกล้หมดอายุ"

- **Notification Types**:
  - `sendExpiryNotification()`: Individual item alerts
  - `sendGroupNotification()`: Summary for multiple items
  - All use `IMPORTANCE_HIGH` priority

- **Notification Format**:
  - Individual: "ชื่อวัตถุดิบ — เหลืออีก X วัน"
  - Expired: "❌ [ชื่อ] หมดอายุแล้ว!"
  - Multiple: "⚠️ มี 3 รายการใกล้หมดอายุ"

### 2. **ExpiryCheckWorker.java** — Scheduled Background Task
**File**: `worker/ExpiryCheckWorker.java`

**Features**:
- Extends `androidx.work.Worker`
- **Execution**:
  - Runs daily (PeriodicWorkRequest)
  - Repeats: Every 24 hours
  - Can be initiated from MainActivity or EmployeeMainActivity

- **Logic**:
  1. Fetch all restaurants from Firestore
  2. For each restaurant:
     - Get all active ingredients
     - Calculate days until expiry
     - Filter into 2 groups:
       - **Expiring**: daysLeft ≤ notifyDaysBefore
       - **Expired**: daysLeft < 0
  3. Send notifications accordingly

- **Notification Rules**:
  - If 1 expiring item: Single notification with name and days left
  - If multiple expiring: Summary notification + individual notifications
  - All expired items: Individual "❌ expired" notifications
  - Uses notification IDs based on `restaurantId.hashCode()`

- **Day Calculation**:
  ```java
  (expiryDate - today) / (1000 * 60 * 60 * 24)
  ```
  Both dates normalized to 00:00:00 for accurate counting

### 3. **MainActivity.java** — Setup & Scheduling
**File**: `MainActivity.java`

**Updates**:
- `scheduleExpiryCheck()`: Enqueues PeriodicWorkRequest
  - 1-day interval
  - `ExistingPeriodicWorkPolicy.KEEP`: Keep existing if already scheduled
  - Work name: "expiry_check"
- `NotificationHelper.createNotificationChannels(this)`: Creates channel on app startup

### 4. **EmployeeMainActivity.java** — Setup & Scheduling
**File**: `activity/EmployeeMainActivity.java`

**Updates**:
- Same as MainActivity
- Ensures notifications work for Employee flow

---

## 📊 Architecture Diagram

```
ScanFragment
    ├─ CameraX (PreviewView)
    ├─ ML Kit Text Recognition
    ├─ DatePatternDetector → DateResult
    ├─ CameraOverlayView (draws bounding boxes)
    └─ User clicks "Use Date" → AddIngredientDialog
            ├─ Firestore: Save Ingredient
            │   ├─ name, category
            │   ├─ expiryDate, notifyDaysBefore
            │   ├─ scannedBy, scannedAt
            │   ├─ updatedBy, status="active"
            │   └─ restaurantId
            └─ Return to ScanFragment

Daily @ WorkManager (8:00 AM)
    ├─ ExpiryCheckWorker
    │   ├─ Fetch all restaurants
    │   ├─ For each restaurant:
    │   │   ├─ Get active ingredients
    │   │   ├─ Calculate daysLeft
    │   │   ├─ Filter expiring/expired
    │   │   └─ Send notifications
    │   └─ NotificationHelper.sendExpiryNotification()
    │       └─ NotificationManager (CHANNEL_ID: expiry_alerts)
    └─ User sees notification in status bar
```

---

## 🔄 Data Flow

### 1. **Scanning Flow**
```
Camera Feed
    → ML Kit → Text Recognition
        → DatePatternDetector.detectDate()
            → DateResult found?
                YES → Show green popup + "Use Date" button
                NO → Continue scanning
                    → User clicks "Manual Entry"
                        → Open AddIngredientDialog (empty date)

User confirms → AddIngredientDialog.saveIngredient()
    → Firestore: /ingredients collection
```

### 2. **Notification Flow**
```
WorkManager Schedule (Daily)
    → ExpiryCheckWorker.doWork()
        → Query: ingredients where status="active"
            → Process each:
                daysLeft < 0? → EXPIRED notification
                daysLeft ≤ notifyDays? → EXPIRING notification
            → NotificationHelper.sendExpiryNotification()
                → NotificationManager.notify()
                    → System shows in status bar
```

---

## 🚀 Testing Checklist

- [ ] **Camera Permission**
  - [ ] App requests permission on first ScanFragment open
  - [ ] Permission granted → Camera preview shows
  - [ ] Permission denied → Show error message

- [ ] **OCR Detection**
  - [ ] Point camera at expiry date labels
  - [ ] Wait for text recognition
  - [ ] Detected date appears in green popup
  - [ ] Date formats work: DD/MM/YYYY, DD-MM-YYYY, YYYY-MM-DD, MM/YYYY
  - [ ] Keywords work: "EXP:", "BBF:", "หมดอายุ:"

- [ ] **AddIngredientDialog**
  - [ ] Dialog opens with scanned date pre-filled
  - [ ] Can edit date with DatePicker
  - [ ] Category dropdown has all 7 categories
  - [ ] NumberPicker ranges 1-14 days
  - [ ] Name validation (required field)
  - [ ] Saves to Firestore correctly

- [ ] **Notifications**
  - [ ] Notification channel created (check Settings → Notifications → Expiry)
  - [ ] Set ingredient with expiryDate = today + 2 days, notifyDays = 3
  - [ ] Wait 24 hours or manually trigger WorkManager
  - [ ] Notification appears: "⚠️ [Name] — เหลืออีก 2 วัน"
  - [ ] Set expiryDate = today - 1 day
  - [ ] Notification: "❌ [Name] หมดอายุแล้ว!"
  - [ ] Multiple items → Shows summary + individual notifications

- [ ] **WorkManager**
  - [ ] `adb shell dumpsys package w com.example.expirytrack` (check WorkManager logs)
  - [ ] Work scheduled with name "expiry_check"
  - [ ] Periodic interval: 1 day

---

## 📝 Files Created/Modified

### **Created Files** ✨
1. ✅ `util/DatePatternDetector.java` — Date pattern detection
2. ✅ `util/CameraOverlayView.java` — Bounding box drawing
3. ✅ `util/NotificationHelper.java` — Notification management
4. ✅ `fragment/AddIngredientDialog.java` — Add ingredient dialog
5. ✅ `worker/ExpiryCheckWorker.java` — Background notification task
6. ✅ `res/layout/dialog_add_ingredient.xml` — Dialog layout

### **Modified Files** 🔄
1. ✅ `app/build.gradle` — Added WorkManager dependency
2. ✅ `AndroidManifest.xml` — Added CAMERA permission
3. ✅ `fragment/ScanFragment.java` — Complete rewrite with CameraX + ML Kit
4. ✅ `res/layout/fragment_scan.xml` — Updated layout
5. ✅ `MainActivity.java` — Added notification setup + WorkManager scheduling
6. ✅ `activity/EmployeeMainActivity.java` — Added notification setup + WorkManager scheduling

---

## Next Steps (ROUND 5+)

1. **Manager Dashboard**
   - Inventory statistics
   - Low stock alerts
   - Usage history viewer
   - Employee management

2. **Advanced Filtering**
   - Search by name/category
   - Filter by expiry range
   - Sort options

3. **Analytics**
   - Waste statistics
   - Usage patterns
   - Most/least used items

4. **Settings**
   - Notification preferences
   - Default notification days
   - App preferences

---

**Last Updated**: March 2, 2026  
**Status**: ✅ Round 3 & 4 Complete
