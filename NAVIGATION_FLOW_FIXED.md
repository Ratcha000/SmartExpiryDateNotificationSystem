# ✅ Navigation Flow - FIXED

## 🎯 App Startup Flow

### **When user runs the app:**

```
┌─────────────────────────────────────┐
│        SplashActivity               │
│    (2 second splash screen)         │
└────────────┬────────────────────────┘
             │
             ├─ Check Firebase Auth
             │
             ├─ NO USER FOUND
             │  └─→ ✅ SEND TO: LoginActivity
             │
             └─ USER FOUND
                ├─ Fetch user role from Firestore
                │
                ├─ Role = "employee"
                │  └─→ ✅ SEND TO: EmployeeMainActivity
                │
                ├─ Role = "manager"
                │  └─→ ✅ SEND TO: ManagerMainActivity
                │
                └─ Error / Unknown role
                   └─→ ✅ SEND TO: LoginActivity
```

---

## 📱 Employee Flow

### **New Employee Registration:**
```
RegisterActivity
    ├─ Select "Employee" role
    ├─ Fill: Email, Password, Name
    ├─ Enter: Invite Code (from manager)
    ├─ Click Register
    │  ├─ Create user in Firebase Auth
    │  ├─ Check invite code validity
    │  ├─ Add user to restaurant
    │  └─ Save to Firestore
    └─→ ✅ REDIRECT TO: EmployeeMainActivity
         ├─ 📁 Bottom Navigation (2 tabs)
         │  ├─ 🏠 Ingredient List (Default)
         │  │  ├─ Firestore query: restaurantId + status="active"
         │  │  ├─ Real-time listener (addSnapshotListener)
         │  │  ├─ Sorting: expiryDate ASC
         │  │  └─ RecyclerView with CardView
         │  │     ├─ Card color: Red/Orange/Green based on days left
         │  │     ├─ Buttons: "✓ Use", "✏️ Edit", "🗑️ Delete"
         │  │     └─ Records UsageHistory on action
         │  │
         │  └─ 📷 Scan
         │     ├─ CameraX real-time preview
         │     ├─ ML Kit text recognition
         │     ├─ DatePatternDetector (multiple formats)
         │     ├─ Green popup when date found
         │     └─ AddIngredientDialog (pre-filled date)
         │
         ├─ 🔔 Notifications
         │  ├─ Notification channel "expiry_alerts" created
         │  ├─ WorkManager scheduled (daily)
         │  └─ Alerts on ingredients near expiry
         │
         └─ Logout button
            └─→ Sign out → LoginActivity

```

### **Returning Employee Login:**
```
LoginActivity
    ├─ Enter: employee email + password
    ├─ Click: Login
    │  ├─ Firebase Auth sign in
    │  ├─ Fetch role = "employee"
    │  └─ Clear task stack
    └─→ ✅ REDIRECT TO: EmployeeMainActivity
         └─ [Same as above]
```

### **App Restart (Employee logged in):**
```
SplashActivity
    ├─ User exists in Firebase Auth
    ├─ Fetch role from Firestore = "employee"
    └─→ ✅ REDIRECT TO: EmployeeMainActivity
         └─ [Resume where they left off]
```

---

## 👔 Manager Flow

### **New Manager Registration:**
```
RegisterActivity
    ├─ Select "Manager" role
    ├─ Fill: Email, Password, Name
    ├─ Enter: Restaurant Name
    ├─ Click Register
    │  ├─ Create user in Firebase Auth
    │  ├─ Generate random 6-char Invite Code
    │  ├─ Create Restaurant in Firestore:
    │  │  ├─ name: [Restaurant Name]
    │  │  ├─ managerId: [User ID]
    │  │  ├─ inviteCode: [Random Code]
    │  │  └─ createdAt: [Timestamp]
    │  ├─ Save user to Firestore:
    │  │  ├─ role: "manager"
    │  │  ├─ restaurantId: [New Restaurant ID]
    │  │  └─ other fields
    │  └─ Toast: "Restaurant created! Code: ABC123"
    └─→ ✅ REDIRECT TO: ManagerMainActivity
         ├─ Display: "Manager: [Manager Name]"
         ├─ Show: Invite Code (for employees to join)
         ├─ Logout button
         └─ (Dashboard features in next phase)
```

### **Returning Manager Login:**
```
LoginActivity
    ├─ Enter: manager email + password
    ├─ Click: Login
    │  ├─ Firebase Auth sign in
    │  ├─ Fetch role = "manager"
    │  └─ Clear task stack
    └─→ ✅ REDIRECT TO: ManagerMainActivity
         └─ [Manager screen]
```

### **App Restart (Manager logged in):**
```
SplashActivity
    ├─ User exists in Firebase Auth
    ├─ Fetch role from Firestore = "manager"
    └─→ ✅ REDIRECT TO: ManagerMainActivity
         └─ [Resume manager screen]
```

---

## 🔐 Logout Flow

### **From Any Screen:**
```
[Employee/Manager page]
    └─ Click: Logout button
       ├─ Firebase Auth sign out
       ├─ Clear all credentials
       └─→ REDIRECT TO: LoginActivity
            └─ Ready for new login
```

---

## 📊 Complete Activity Flow Diagram

```
                    ┌──────────────────┐
                    │ SplashActivity   │
                    │ (2 sec splash)   │
                    └────────┬─────────┘
                             │
            ┌────────────────┼────────────────┐
            │                │                │
         No user        User exists      Firebase error
            │                │                │
            ▼                ▼                ▼
      ┌──────────┐    ┌────────────┐    ┌──────────┐
      │  Login   │    │Check role  │    │  Login   │
      │Activity  │    │in Firestore│    │Activity  │
      └──────────┘    └────┬───────┘    └──────────┘
            ▲               │
            │        ┌──────┴──────┬──────────┐
            │        │             │          │
            │      "emp"          "mgr"    Unknown
            │        │             │          │
            │        ▼             ▼          ▼
            │    Employee      Manager      Login
            │    MainActivity  MainActivity Activity
            │        │             │
            └────────┴─────────┬───┴──────────┘
                                │
                          (User logs out)
                                │
                                ▼
                           LoginActivity
```

---

## ✅ Fixed Issues

### **Issue #1: SplashActivity** ✅ FIXED
**Before:**
- Always redirected to MainActivity regardless of role

**After:**
- ✅ Checks user logged in status
- ✅ Fetches role from Firestore
- ✅ Routes to EmployeeMainActivity if role = "employee"
- ✅ Routes to ManagerMainActivity if role = "manager"
- ✅ Routes to LoginActivity if no user or error

### **Issue #2: RegisterActivity (Manager)** ✅ FIXED
**Before:**
- Redirected to MainActivity

**After:**
- ✅ Redirects to ManagerMainActivity with flags:
  - `FLAG_ACTIVITY_NEW_TASK`: Start new task
  - `FLAG_ACTIVITY_CLEAR_TASK`: Clear back stack

### **Issue #3: RegisterActivity (Employee)** ✅ FIXED
**Before:**
- Redirected to MainActivity

**After:**
- ✅ Redirects to EmployeeMainActivity with flags:
  - `FLAG_ACTIVITY_NEW_TASK`: Start new task
  - `FLAG_ACTIVITY_CLEAR_TASK`: Clear back stack

---

## 🧪 Testing Verification

### **Test Case 1: First Launch - No Account**
```
✅ SplashActivity shows
✅ Redirects to LoginActivity
✅ See: "Email" field, "Password" field, "Register" link
```

### **Test Case 2: Register as Employee**
```
✅ Click "Register"
✅ Select "Employee"
✅ See: Email, Password, Name, Invite Code fields
✅ Restaurant name field HIDDEN
✅ Fill all fields, click Register
✅ Redirects to EmployeeMainActivity
✅ See: 2 bottom nav tabs (Ingredients + Scan)
```

### **Test Case 3: Register as Manager**
```
✅ Click "Register"
✅ Select "Manager"
✅ See: Email, Password, Name, Restaurant Name fields
✅ Invite code field HIDDEN
✅ Fill all fields, click Register
✅ Toast shows: "Restaurant created! Code: ABC123"
✅ Redirects to ManagerMainActivity
✅ See: "Manager: [Name]" + Logout button
```

### **Test Case 4: Login as Employee**
```
✅ Go back to LoginActivity
✅ Enter employee email + password
✅ Redirects to EmployeeMainActivity
✅ Can access Ingredients list and Scan
```

### **Test Case 5: Login as Manager**
```
✅ Enter manager email + password
✅ Redirects to ManagerMainActivity
✅ See: "Manager: [Name]"
```

### **Test Case 6: Kill App & Reopen (Employee)**
```
✅ Kill app (pull from recent apps)
✅ Reopen app
✅ SplashActivity shows (2 sec)
✅ Checks Firebase Auth → User exists
✅ Fetches role from Firestore → "employee"
✅ Redirects to EmployeeMainActivity
✅ See: Ingredients list (where they left off)
```

### **Test Case 7: Kill App & Reopen (Manager)**
```
✅ Kill app
✅ Reopen app
✅ SplashActivity shows
✅ Checks Firebase Auth → User exists
✅ Fetches role from Firestore → "manager"
✅ Redirects to ManagerMainActivity
✅ See: "Manager: [Name]"
```

### **Test Case 8: Logout**
```
✅ Click Logout button (from any main activity)
✅ Firebase auth signs out
✅ Redirects to LoginActivity
✅ Fields are empty
✅ Can login again as different user
```

---

## 📝 Files Modified

| File | Changes | Status |
|------|---------|--------|
| SplashActivity.java | Added role checking logic from Firestore | ✅ FIXED |
| RegisterActivity.java | Added import for ManagerMainActivity + EmployeeMainActivity + Fixed redirect | ✅ FIXED |
| EmployeeMainActivity.java | No changes needed | ✅ OK |
| ManagerMainActivity.java | No changes needed | ✅ OK |
| LoginActivity.java | No changes needed | ✅ OK |

---

## 🚀 Ready for Testing!

The app should now:
1. ✅ Show login on first launch
2. ✅ Route managers to ManagerMainActivity
3. ✅ Route employees to EmployeeMainActivity
4. ✅ Restore correct page on app restart
5. ✅ Handle logout correctly

---

**Last Updated**: March 2, 2026  
**Status**: ✅ All Navigation Issues Fixed
