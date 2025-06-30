package com.example.kuripothub.utils;

import android.util.Log;

import com.example.kuripothub.models.User;
import com.example.kuripothub.models.Expense;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class FirebaseManager {
    
    private static final String TAG = "FirebaseManager";
    private static final String USERS_COLLECTION = "users";
    private static final String EXPENSES_COLLECTION = "expenses";
    
    private static FirebaseManager instance;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    
    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }
    
    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }
    
    public FirebaseAuth getAuth() {
        return auth;
    }
    
    public FirebaseFirestore getFirestore() {
        return firestore;
    }
    
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }
    
    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }
    
    public void signOut() {
        auth.signOut();
    }
    
    // Authentication methods
    public Task<AuthResult> signUpWithEmailAndPassword(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password);
    }
    
    public Task<AuthResult> signInWithEmailAndPassword(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }
    
    public Task<Void> sendPasswordResetEmail(String email) {
        return auth.sendPasswordResetEmail(email);
    }
    
    // User data methods
    public Task<Void> createUserProfile(User user) {
        return firestore.collection(USERS_COLLECTION)
                .document(user.getUid())
                .set(user);
    }
    
    public Task<DocumentSnapshot> getUserProfile(String uid) {
        return firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get();
    }

    public Task<QuerySnapshot> checkUsernameAvailability(String username) {
        return firestore.collection(USERS_COLLECTION)
                .whereEqualTo("username", username)
                .get();
    }

    public Task<QuerySnapshot> getUserByUsername(String username) {
        return firestore.collection(USERS_COLLECTION)
                .whereEqualTo("username", username)
                .get();
    }
    
    public Task<Void> updateUserBudget(String uid, double budget) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("budget", budget);
        return firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update(updates);
    }
    
    // Expense methods
    public Task<DocumentReference> addExpense(Expense expense) {
        return firestore.collection(EXPENSES_COLLECTION)
                .add(expense);
    }
    
    public Task<QuerySnapshot> getUserExpenses(String userId) {
        return firestore.collection(EXPENSES_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
    }
    
    public Task<QuerySnapshot> getUserExpensesByDate(String userId, String date) {
        return firestore.collection(EXPENSES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
    }
    
    public Task<QuerySnapshot> getUserExpensesByDateSimple(String userId, String date) {
        // Simpler query without orderBy to avoid composite index requirement
        return firestore.collection(EXPENSES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date)
                .get();
    }
    
    public Task<QuerySnapshot> getUserExpensesByCategory(String userId, String category) {
        return firestore.collection(EXPENSES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("category", category)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
    }
    
    public Task<Void> deleteExpense(String expenseId) {
        return firestore.collection(EXPENSES_COLLECTION)
                .document(expenseId)
                .delete();
    }
    
    public Task<Void> updateExpense(String expenseId, Expense expense) {
        return firestore.collection(EXPENSES_COLLECTION)
                .document(expenseId)
                .set(expense);
    }
}
