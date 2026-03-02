# 🔍 App Navigation Flow Analysis

## Current Flow Status

### ✅ **WORKING CORRECTLY:**

#### 1. **Login Activity** ✅
```
LoginActivity:
  ├─ Employee logs in
  │  └─ Role check: "employee" → Redirects to EmployeeMainActivity ✅
  └─ Manager logs in
     └─ Role check: "manager" → Redirects to ManagerMainActivity ✅
```

#### 2. **Employee Flow** ✅
```
Redirects to: EmployeeMainActivity
Features:
  ├─ Bottom Navigation Bar (2 tabs)
  │  ├─ 🏠 RaisingList (IngredientListFragment)
  │  │  ├─ Firestore Query: active ingredients
  │  │  ├─ Real-time listener
  │  │  ├─ Sorting: expiryDate ASC
  │  │  └─ RecyclerView with CardView
  │  └─ 📷 Scan (ScanFragment)
  │     ├─ CameraX real-time preview
  │     ├─ ML Kit text recognition
  │     ├─ DatePatternDetector
  │     └─ AddIngredientDialog
  ├─ WorkManager scheduled (daily expiry check)
  └─ Notification channel created
```

#### 3. **Manager Flow** ✅
```
Redirects to: ManagerMainActivity
Features:
  ├─ Display: "Manager: [Name]"
  ├─ Logout button
  └─ (Dashboard should be added in next phase)
```

---

## ⚠️ **ISSUES FOUND:**

### **Issue #1: SplashActivity - Incorrect Redirect Logic** 🔴
**File**: `SplashActivity.java` (lines 22-29)

**Current Code:**
```java
FirebaseUser currentUser = auth.getCurrentUser();
Intent intent;
if (currentUser != null) {
    intent = new Intent(SplashActivity.this, MainActivity.class);  // ❌ WRONG!
} else {
    intent = new Intent(SplashActivity.this, LoginActivity.class);
}
```

**Problem:**
- ✅ Correctly detects if user is logged in
- ❌ Always directs to `MainActivity` instead of checking role
- ❌ Ignores EmployeeMainActivity and ManagerMainActivity

**Expected Flow:**
```
User Logged In?
├─ NO → LoginActivity ✅
└─ YES → Check User Role
         ├─ Employee → EmployeeMainActivity ✅
         ├─ Manager → ManagerMainActivity ✅
         └─ Unknown → LoginActivity (for safety)
```

**Fix Needed:**
- Need to fetch user's role from Firestore
- Then redirect to appropriate activity

---

### **Issue #2: RegisterActivity - Manager Redirect Wrong** 🔴
**File**: `RegisterActivity.java` (line ~90)

**Current Code:**
```java
startActivity(new Intent(RegisterActivity.this, MainActivity.class));
```

**Problem:**
- Manager registration redirects to generic `MainActivity`
- Should redirect to `ManagerMainActivity` instead

**Expected:**
```java
startActivity(new Intent(RegisterActivity.this, ManagerMainActivity.class));
```

---

### **Issue #3: MainActivity Purpose Unclear** 🟡
**File**: `MainActivity.java`

**Current Code:**
```java
public class MainActivity extends AppCompatActivity {
    // Shows welcome + logout button
    // Not used in the flow anymore
}
```

**Problem:**
- This was the old "after-login" placeholder
- SplashActivity still directs here
- RegisterActivity (Manager) directs here
- **Should be removed or repurposed**

**Recommendation:**
- Remove MainActivity from the flow
- Use it only for developer/testing
- All users should go to Employee/Manager activities

---

## 📊 **Complete Fixed Flow**

### **App Startup:**
```
SplashActivity (2 sec splash screen)
    │
    ├─ Check Firebase Auth
    │
    ├─ NO USER → LoginActivity
    │              ├─ Email/Password → Login
    │              ├─ Success → Check Role → Redirect
    │              │ ├─ Employee → EmployeeMainActivity
    │              │ └─ Manager → ManagerMainActivity
    │              └─ "Don't have account?" → RegisterActivity
    │
    └─ USER EXISTS → Fetch User Role from Firestore
                     ├─ Role = "employee" → EmployeeMainActivity
                     ├─ Role = "manager" → ManagerMainActivity
                     └─ No role found → LoginActivity (error recovery)
```

### **Registration:**
```
RegisterActivity
    ├─ Role Selection
    │
    ├─ IF Manager:
    │  ├─ Input: Restaurant Name
    │  ├─ Action: Create Restaurant + Generate Invite Code
    │  └─ Redirect → ManagerMainActivity ⚠️ Currently MainActivity
    │
    └─ IF Employee:
       ├─ Input: Invite Code
       ├─ Action: Find Restaurant & Join
       └─ Redirect → EmployeeMainActivity ✅
```

---

## 🔧 **Fixes Required**

### **Fix #1: Update SplashActivity**
```java
private void checkUserAndRedirect() {
    FirebaseUser currentUser = auth.getCurrentUser();
    
    if (currentUser == null) {
        // No user logged in
        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
    } else {
        // User logged in - fetch role
        String userId = currentUser.getUid();
        db.collection("users").document(userId).get()
            .addOnSuccessListener(doc -> {
                String role = doc.getString("role");
                Intent intent;
                
                if ("employee".equalsIgnoreCase(role)) {
                    intent = new Intent(SplashActivity.this, EmployeeMainActivity.class);
                } else if ("manager".equalsIgnoreCase(role)) {
                    intent = new Intent(SplashActivity.this, ManagerMainActivity.class);
                } else {
                    // Unknown role - go to login
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }
                
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
    }
}
```

### **Fix #2: Update RegisterActivity (Manager Branch)**
Change line ~90 from:
```java
startActivity(new Intent(RegisterActivity.this, MainActivity.class));
```
To:
```java
startActivity(new Intent(RegisterActivity.this, ManagerMainActivity.class));
```

---

## 📋 **Testing Verification Checklist**

### **Test 1: New User - No Login**
- [ ] App starts → SplashActivity shows 2 sec
- [ ] Redirects to → LoginActivity ✅
- [ ] Shows email/password fields
- [ ] "Register" link visible

### **Test 2: Register as Employee**
- [ ] Click "Register" → RegisterActivity
- [ ] Select "Employee" role
- [ ] Fields shown: Email, Password, Name, InviteCode
- [ ] Fill and submit
- [ ] Redirects to → **EmployeeMainActivity** ✅
- [ ] Shows 2 tabs: Ingredients + Scan
- [ ] Bottom navigation works

### **Test 3: Register as Manager**
- [ ] Click "Register" → RegisterActivity
- [ ] Select "Manager" role
- [ ] Fields shown: Email, Password, Name, RestaurantName
- [ ] Fill and submit
- [ ] **Should redirect to ManagerMainActivity** (CURRENTLY WRONG)
- [ ] Shows: "Manager: [Name]" + Logout button

### **Test 4: Login as Employee**
- [ ] Go back to LoginActivity
- [ ] Enter employee credentials
- [ ] Redirects to → **EmployeeMainActivity** ✅
- [ ] Can see ingredients list

### **Test 5: Login as Manager**
- [ ] Enter manager credentials
- [ ] Redirects to → **ManagerMainActivity** ✅
- [ ] Can see "Manager: [Name]"

### **Test 6: Close & Reopen App (Employee)**
- [ ] Kill app
- [ ] Reopen
- [ ] SplashActivity → Should check role → **EmployeeMainActivity** (CURRENTLY GOES TO MAIN ACTIVITY)
- [ ] Should show ingredients list

### **Test 7: Close & Reopen App (Manager)**
- [ ] Kill app
- [ ] Reopen
- [ ] SplashActivity → Should check role → **ManagerMainActivity** (CURRENTLY GOES TO MAIN ACTIVITY)
- [ ] Should show "Manager: [Name]"

### **Test 8: Logout**
- [ ] Click logout
- [ ] Redirects to → LoginActivity ✅
- [ ] Credentials cleared

---

## 🎯 **Summary**

| Component | Status | Issue |
|-----------|--------|-------|
| LoginActivity | ✅ WORKING | Correctly routes to Employee/Manager |
| EmployeeMainActivity | ✅ WORKING | Shows ingredients + scan tabs |
| ManagerMainActivity | ✅ WORKING | Shows manager screen |
| RegisterActivity (Employee) | ✅ WORKING | Routes to EmployeeMainActivity |
| RegisterActivity (Manager) | 🔴 **BROKEN** | Routes to MainActivity instead of ManagerMainActivity |
| SplashActivity | 🔴 **BROKEN** | Routes to MainActivity instead of checking role |
| MainActivity | 🟡 UNUSED | No longer relevant in current flow |

---

## 📝 **Recommendation**

**Priority Fixes:**
1. 🔴 **HIGH**: Fix SplashActivity to check role
2. 🔴 **HIGH**: Fix RegisterActivity manager redirect
3. 🟡 **LOW**: Remove/repurpose MainActivity

These fixes will ensure the app correctly:
- Shows login on first launch ✅
- Routes manager to ManagerMainActivity ✅
- Routes employee to EmployeeMainActivity ✅
- Restores correct screen on app restart ✅

---

**Last Updated**: March 2, 2026
