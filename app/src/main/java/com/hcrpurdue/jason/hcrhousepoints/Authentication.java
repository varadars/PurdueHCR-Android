package com.hcrpurdue.jason.hcrhousepoints;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class Authentication extends AppCompatActivity {
    private FirebaseAuth auth;
    private Map<String, Pair<String, String>> floorCodes;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        getFloorCodes();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null)
            return; // TODO: Go to the logged in page
        // TODO:  Pull house info and floor codes
    }

    public void signIn(View view) {
        EditText email = findViewById(R.id.emailInput);
        EditText password = findViewById(R.id.passwordInput);
        String emailText = email.getText().toString();
        String passwordText = password.getText().toString();

        if (signInInvalid(email, password))
            return;

        auth.signInWithEmailAndPassword(emailText, passwordText)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            // TODO: Go to next page
                        } else {
                            Toast.makeText(getApplicationContext(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void signUp(View view) {
        EditText email = findViewById(R.id.emailInput);
        EditText password = findViewById(R.id.passwordInput);
        EditText passwordConfirm = findViewById(R.id.confirmPasswordInput);
        EditText name = findViewById(R.id.nameInput);
        EditText floorCode = findViewById(R.id.floorCodeInput);
        final String emailText = email.getText().toString();
        final String passwordText = password.getText().toString();
        final String nameText = name.getText().toString();
        final String floorCodeText = floorCode.getText().toString();


        if (signInInvalid(email, password) || signUpInvalid(password, passwordConfirm, name, floorCode))
            return;

        auth.createUserWithEmailAndPassword(emailText, passwordText)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();

                            // Generates all of the other data for the user in the DB
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("Name", nameText);
                            userData.put("FloorID", floorCodes.get(floorCodeText).first);
                            userData.put("House", floorCodes.get(floorCodeText).second);
                            userData.put("Permission Level", 0);
                            userData.put("TotalPoint", 0);
                            if(user != null)
                                db.collection("Users").document(user.getUid()).set(userData)
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(getApplicationContext(), "User DB binds failed, please tell your RHP to tell Jason", Toast.LENGTH_LONG).show();
                                            }
                                        });
                            else
                                Toast.makeText(getApplicationContext(), "User was created but not loaded, please tell your RHP to tell Jason", Toast.LENGTH_SHORT).show();
                            // TODO: Show rest of stuff after signed in
                        } else {
                            Toast.makeText(getApplicationContext(), "Authentication failed, try again later before contacting your RHP",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private boolean signInInvalid(EditText email, EditText password) {
        // Hide the virtual keyboard
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if(inputManager != null && getCurrentFocus() != null) // Avoids null pointer exceptions
            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);

        // Checks the email text, makes sure it's a valid email address, and makes sure its a Purdue address
        String emailText = email.getText().toString();
        if (TextUtils.isEmpty(emailText) || !Patterns.EMAIL_ADDRESS.matcher(emailText).matches() || !emailText.matches("[A-Z0-9a-z._%+-]+@purdue\\.edu")) {
            Toast.makeText(getApplicationContext(), "Email is not a valid Purdue email address", Toast.LENGTH_SHORT).show();
            return true;
        }

        // Checks the password text
        String passwordText = password.getText().toString();
        if (TextUtils.isEmpty(passwordText)) {
            Toast.makeText(getApplicationContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show();
            return true;
        }
        if(passwordText.length() < 6){
            Toast.makeText(getApplicationContext(), "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }

    private boolean signUpInvalid(EditText password, EditText passwordConfirm, EditText name, EditText floorCode) {
        String passwordText = password.getText().toString();
        String passwordConfirmText = passwordConfirm.getText().toString();
        String nameText = name.getText().toString();
        String floorCodeText = floorCode.getText().toString();

        // Checks that the passwords match
        if (!passwordText.equals(passwordConfirmText)) {
            Toast.makeText(getApplicationContext(), "Passwords must match", Toast.LENGTH_SHORT).show();
            return true;
        }

        // Checks to make sure that there is at least one space in the name for first and last name
        if(!nameText.contains(" ")){
            Toast.makeText(getApplicationContext(), "Name must include your first and last name", Toast.LENGTH_SHORT).show();
            return true;
        }

        // Checks that the floor code is valid
        if(floorCodes.get(floorCodeText)  == null){
            Toast.makeText(getApplicationContext(), "Floor code invalid", Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }

    // Fills floorCodes with key:value pairs in the form of {floorCode}:({floorName}:{houseName})
    private void getFloorCodes(){
        floorCodes = new HashMap<>();
        db.collection("House").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    for(QueryDocumentSnapshot document : task.getResult())
                    {
                        Map<String, Object> data = document.getData();
                        for(Map.Entry<String, Object> entry : data.entrySet())
                        {
                            if(entry.getKey().contains("Code") && entry.getValue().getClass() == entry.getKey().getClass())
                            {
                                floorCodes.put((String)entry.getValue(), new Pair<>(entry.getKey().replace("Code", ""), document.getId()));
                            }
                        }
                    }
                }
                else
                    Toast.makeText(getApplicationContext(), "Error retrieving Floor codes from database, please try again later before contacting your RHP", Toast.LENGTH_LONG).show();
            }
        });
    }
}
