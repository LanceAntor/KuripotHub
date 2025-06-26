# KuripotHub Firebase Setup

## ⚠️ Important: Firebase Configuration Required

This project uses Firebase for authentication and data storage. You need to set up your own Firebase project and configuration.

### Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter project name: "KuripotHub" (or your preferred name)
4. Follow the setup wizard

### Step 2: Add Android App

1. In Firebase Console, click "Add app" → Android
2. Enter package name: `com.example.kuripothub`
3. Download the `google-services.json` file
4. Place it in the `app/` directory (replace the template file)

### Step 3: Enable Firebase Services

#### Authentication:
1. Go to Authentication → Sign-in method
2. Enable "Email/Password" provider

#### Firestore Database:
1. Go to Firestore Database
2. Click "Create database"
3. Choose "Start in test mode"
4. Select your preferred location

### Step 4: Replace Configuration File

1. Copy your downloaded `google-services.json` to `app/google-services.json`
2. The file should contain your actual Firebase project credentials
3. **Never commit this file to version control** (it's already in .gitignore)

### Step 5: Build and Run

```bash
./gradlew build
```

## Security Note

The `google-services.json` file contains sensitive API keys and should never be committed to version control. This repository includes a template file for reference only.

## Project Structure

```
KuripotHub/
├── app/
│   ├── google-services.json          ← Your actual Firebase config (not in repo)
│   ├── google-services.json.template ← Template for reference
│   └── src/
└── .gitignore                        ← Excludes sensitive files
```

## Troubleshooting

- If you get build errors, ensure `google-services.json` is in the correct location
- Make sure Firebase services are enabled in your Firebase project
- Check that your package name matches exactly: `com.example.kuripothub`
