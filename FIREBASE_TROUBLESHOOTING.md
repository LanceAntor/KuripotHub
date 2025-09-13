# Firebase Setup and Troubleshooting Guide

## Current Issues and Solutions

Your Firebase authentication errors are likely caused by one of these common issues:

### 1. **Firestore Security Rules** (Most Common Issue)
Your Firestore database might have restrictive security rules that prevent read/write operations.

**To Fix:**
1. Go to Firebase Console: https://console.firebase.google.com/
2. Select your project: `kuripothub-6172a`
3. Go to "Firestore Database" > "Rules"
4. Replace the current rules with these **temporary testing rules**:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow read/write access to all documents for authenticated and unauthenticated users
    // WARNING: These are permissive rules for testing only!
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

**For Production, use these more secure rules:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only access their own documents
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Allow reading users collection for username checks (signup/login)
    match /users/{userId} {
      allow read: if true;
    }
    
    // Users can only access their own expenses
    match /expenses/{expenseId} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
    }
  }
}
```

### 2. **Firestore Database Not Created**
If you haven't created a Firestore database yet:

1. Go to Firebase Console > Firestore Database
2. Click "Create database"
3. Choose "Start in test mode" (for now)
4. Select a location (preferably close to your users)

### 3. **Firebase Project Configuration**
Check that your `google-services.json` is properly configured:

- Project ID: `kuripothub-6172a`
- Package name: `com.example.kuripothub`
- API Key: Present and valid

### 4. **Network/Firewall Issues**
- Ensure your device/emulator has internet access
- Check if corporate firewalls are blocking Firebase URLs
- Try switching between WiFi and mobile data

## Testing Steps

1. **Use the Firebase Test Feature:**
   - Long press the "LOGIN" button on the login screen
   - This will open a Firebase connection test
   - Check the results to see what's failing

2. **Check Android Logs:**
   ```bash
   adb logcat | grep -E "(Firebase|Firestore|KuripotHub)"
   ```

3. **Common Error Messages and Solutions:**

   - **"PERMISSION_DENIED"**: Fix Firestore security rules (see #1 above)
   - **"UNAVAILABLE"**: Network connectivity issue or Firebase service down
   - **"UNAUTHENTICATED"**: User not signed in, but trying to access protected data
   - **"NOT_FOUND"**: Database doesn't exist or collection doesn't exist

## Immediate Action Plan

1. **First**: Check your Firestore security rules and set them to permissive for testing
2. **Second**: Run the Firebase test (long press login button)
3. **Third**: Try creating an account again
4. **Fourth**: Check the Android logs for specific error messages

## Files Modified for Better Debugging

1. `KuripotHubApplication.java` - Added Firebase initialization logging
2. `FirebaseManager.java` - Added detailed logging for all operations
3. `LoginActivity.java` - Added network checks and better error handling
4. `SignUpActivity.java` - Added network checks and better error handling
5. `FirebaseTestActivity.java` - Created test activity for connection verification

## Quick Test Command

To see real-time logs while testing:
```bash
adb logcat -c && adb logcat | grep -E "(Firebase|Firestore|KuripotHub|LoginActivity|SignUpActivity)"
```

This will clear logs and show only relevant Firebase/app logs.
