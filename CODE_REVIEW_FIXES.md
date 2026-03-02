# Code Review & Fixes - Expirytrack App (Round 2)

## Summary
ตรวจสอบโค้ดแล้ว พบปัญหา 5 ข้อที่สำคัญ แล้วแก้ไขทั้งหมด

---

## 🔴 Issues Found & Fixed

### 1. **CRITICAL: IngredientListFragment - restaurantId ไม่ถูกต้อง**
**ไฟล์**: `IngredientListFragment.java` (บรรทัด 51)

**ปัญหา**:
```java
restaurantId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
```
- ใช้UID ของผู้ใช้เป็น restaurantId 
- สาเหตุ: restaurantId ควรมาจากเอกสาร User ใน Firestore ไม่ใช่จาก UID

**ผลกระทบ**: 
- Query ไม่จะหาข้อมูลเลย
- Ingredients ไม่แสดงบนหน้าแม่น้อย

**✅ แก้ไขแล้ว**:
- สร้าง `fetchUserAndLoadIngredients()` method ใหม่
- ดึง User document จาก Firestore เพื่อได้ restaurantId ที่ถูกต้อง
- เพิ่ม error handling

---

### 2. **Missing Dependency: CardView**
**ไฟล์**: `build.gradle`

**ปัญหา**:
- Layout ใช้ `CardView` แต่ไม่มีในdependency
- อาจเกิดปัญหารันไทม์

**✅ แก้ไขแล้ว**:
```gradle
implementation 'androidx.cardview:cardview:1.0.0'
```

---

### 3. **Incomplete Ingredient Model**
**ไฟล์**: `Ingredient.java`

**ปัญหา**:
- ขาด fields: `scannedBy`, `scannedAt`, `updatedBy`
- ตามข้อกำหนด (spec) ต้องมีฟิลด์เหล่านี้

**✅ แก้ไขแล้ว**:
- เพิ่ม `scannedBy` field
- เพิ่ม `scannedAt` field  
- เพิ่ม `updatedBy` field
- เพิ่ม getter/setter สำหรับฟิลด์ใหม่

---

### 4. **Missing updatedBy field in Operations**
**ไฟล์**: `IngredientListFragment.java`

**ปัญหา**:
- `markAsUsed()` และ `confirmDelete()` ไม่ได้บันทึก `updatedBy`
- ไม่สามารถรู้ว่าใครแก้ไขข้อมูล

**✅ แก้ไขแล้ว**:
- เพิ่ม `updatedBy` ใน `markAsUsed()`
- เพิ่ม `updatedBy` ใน `confirmDelete()`

---

### 5. **Missing editIngredientDialog updatedBy**
**ไฟล์**: `EditIngredientDialog.java`

**ปัญหา**:
- `saveIngredient()` ไม่ได้บันทึก userId เป็น `updatedBy`

**✅ แก้ไขแล้ว**:
- เพิ่มการบันทึก `updatedBy` เมื่ออัปเดต ingredient

---

## 🟡 UX/UI Improvements Made

### 6. **Item Layout Enhancement**
**ไฟล์**: `item_ingredient.xml`

**การปรับปรุง**:
- ✅ ปรับสีของ Chip ให้ดูสวยงาม (green background with border)
- ✅ เปลี่ยน Button เป็น MaterialButton มีสี
  - ✓ (Green)
  - ✏️ (Blue)
  - 🗑️ (Red)
- ✅ ปรับ CardView corner radius เป็น 12dp
- ✅ เพิ่ม text color ให้ชัดเจน

---

### 7. **Empty State Layout**
**ไฟล์**: `fragment_ingredient_list.xml`

**การปรับปรุง**:
- ✅ เปลี่ยน LinearLayout เป็น RelativeLayout
- ✅ เพิ่ม emptyState view สำหรับขณะไม่มีข้อมูล
- ✅ แสดงข้อความให้ผู้ใช้รู้ว่าต้องสแกนเพื่อเพิ่มข้อมูล

---

### 8. **Adapter Improvement**
**ไฟล์**: `IngredientAdapter.java`

**การปรับปรุง**:
- ✅ เพิ่ม `setIngredients()` method เพื่อง่ายต่อการอัปเดต list

---

## 📋 Error Handling Improvements

### IngredientListFragment
```java
private void loadIngredients() {
    if (restaurantId == null || restaurantId.isEmpty()) {
        return;  // ✅ เพิ่มการเช็ค restaurantId
    }
    
    db.collection("ingredients")
        ...
        .addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e("IngredientListFragment", "Error loading ingredients", error);  // ✅ เพิ่ม error logging
                return;
            }
            ...
        });
}
```

---

## ✅ Testing Checklist

เพื่อยืนยันว่าแก้ไขถูกต้อง:

- [ ] Build project ให้ผ่าน ไม่มี compile errors
- [ ] Run app แล้วเข้าขาด Employee Main Activity
- [ ] ตรวจสอบว่า Ingredients แสดงขึ้นมาถูกต้อง
- [ ] ทดสอบปุ่ม ✓ (Mark as Used) 
- [ ] ทดสอบปุ่ม ✏️ (Edit)
- [ ] ทดสอบปุ่ม 🗑️ (Delete)
- [ ] ตรวจสอบ Firestore จำหน่ายบันทึก UsageHistory
- [ ] ตรวจสอบว่า updatedBy บันทึกถูกต้อง
- [ ] ทดสอบตรวจสอบสีของ Card (Red/Orange/Green)

---

## 📝 Files Modified

1. ✅ `app/build.gradle` - เพิ่ม CardView dependency
2. ✅ `model/Ingredient.java` - เพิ่ม fields
3. ✅ `fragment/IngredientListFragment.java` - แก้ restaurantId + error handling
4. ✅ `fragment/EditIngredientDialog.java` - เพิ่ม updatedBy
5. ✅ `adapter/IngredientAdapter.java` - เพิ่ม setIngredients method
6. ✅ `res/layout/item_ingredient.xml` - UI improvements
7. ✅ `res/layout/fragment_ingredient_list.xml` - เพิ่ม empty state

---

## Next Steps (ROUND 3)

อื่นต่อไปควรทำ:
1. สร้าง Add New Ingredient Activity/Dialog
2. สร้าง ManagerMainActivity + Dashboard
3. สร้าง ScanFragment (Camera + ML Kit)
4. เพิ่ม Notification functionality

---

**Last Updated**: March 2, 2026  
**Status**: ✅ All critical issues fixed
