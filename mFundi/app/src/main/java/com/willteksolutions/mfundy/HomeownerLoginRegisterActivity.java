package com.willteksolutions.mfundy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class HomeownerLoginRegisterActivity extends AppCompatActivity {

    private TextView homeOwnerStatus, homeOwnerLoginPageQuestion,homeOwnerRegisterPageQuestion;
    private Button homeOwnerLoginBtn, homeOwnerRegisterBtn;
    private EditText homeOwnerEmail, homeOwnerPassword;

    private FirebaseAuth mAuth;
    private ProgressDialog loader;
    private DatabaseReference homeOwnerDatabaseRef;
    private FirebaseAuth.AuthStateListener authStateListener;
    private  String currentHomeownerOnlineID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homeowner_login_register);

        homeOwnerStatus = findViewById(R.id.homeOwnerStatus);
        homeOwnerLoginPageQuestion = findViewById(R.id.homeOwnerLoginPageQuestion);
        homeOwnerLoginBtn = findViewById(R.id.homeOwnerLoginBtn);
        homeOwnerRegisterBtn = findViewById(R.id.homeOwnerRegisterBtn);
        homeOwnerEmail = findViewById(R.id.homeownerLoginRegisterEmail);
        homeOwnerPassword = findViewById(R.id.homeownerLoginRegisterPassword);
        homeOwnerRegisterPageQuestion = findViewById(R.id.homeOwnerRegisterPageQuestion);
        loader = new ProgressDialog(this);
        mAuth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user!= null){
                    Intent intent = new Intent(HomeownerLoginRegisterActivity.this, HomeownersMapsActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };


        homeOwnerRegisterBtn.setEnabled(false);
        homeOwnerRegisterBtn.setVisibility(View.INVISIBLE);

        homeOwnerRegisterPageQuestion.setVisibility(View.INVISIBLE);
        homeOwnerRegisterPageQuestion.setEnabled(false);

        homeOwnerLoginPageQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeOwnerStatus.setText("Homeowner Registration");
                homeOwnerLoginBtn.setVisibility(View.INVISIBLE);
                homeOwnerLoginPageQuestion.setVisibility(View.INVISIBLE);
                homeOwnerRegisterBtn.setVisibility(View.VISIBLE);
                homeOwnerRegisterBtn.setEnabled(true);

                homeOwnerRegisterPageQuestion.setEnabled(true);
                homeOwnerRegisterPageQuestion.setVisibility(View.VISIBLE);
            }
        });

        homeOwnerRegisterPageQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                homeOwnerRegisterPageQuestion.setVisibility(View.INVISIBLE);
                homeOwnerRegisterPageQuestion.setEnabled(false);
                homeOwnerRegisterBtn.setEnabled(false);
                homeOwnerRegisterBtn.setVisibility(View.INVISIBLE);
                homeOwnerStatus.setText("Homeowner Login");
                homeOwnerLoginPageQuestion.setVisibility(View.VISIBLE);

                homeOwnerLoginBtn.setVisibility(View.VISIBLE);
                homeOwnerLoginBtn.setEnabled(true);

            }
        });

        homeOwnerRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = homeOwnerEmail.getText().toString();
                final  String password = homeOwnerPassword.getText().toString();

                if (TextUtils.isEmpty(email)){
                    homeOwnerEmail.setError("Email Required!");
                    return;
                }
                if (TextUtils.isEmpty(password)){
                    homeOwnerPassword.setError("Password Required!");
                    return;
                }else {
                    loader.setMessage("Login in progress...");
                    loader.setCanceledOnTouchOutside(false);
                    loader.show();

                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(HomeownerLoginRegisterActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()){
                                String error = task.getException().toString();
                                Toast.makeText(HomeownerLoginRegisterActivity.this, "Registration Failed: \n" + error, Toast.LENGTH_SHORT).show();
                                loader.dismiss();
                            }else {
                                currentHomeownerOnlineID = mAuth.getCurrentUser().getUid();
                                homeOwnerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("users").child("homeowners").child(currentHomeownerOnlineID);
                                homeOwnerDatabaseRef.setValue(true);

                                Intent intent = new Intent(HomeownerLoginRegisterActivity.this, HomeownersMapsActivity.class);
                                startActivity(intent);
                                finish();
                                loader.dismiss();
                                return;
                            }
                        }
                    });
                }
            }
        });

        homeOwnerLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = homeOwnerEmail.getText().toString();
                final  String password = homeOwnerPassword.getText().toString();

                if (TextUtils.isEmpty(email)){
                    homeOwnerEmail.setError("Email Required!");
                    return;
                }
                if (TextUtils.isEmpty(password)){
                    homeOwnerPassword.setError("Password Required!");
                    return;
                }else {
                    loader.setMessage("Login in progress...");
                    loader.setCanceledOnTouchOutside(false);
                    loader.show();

                    mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(HomeownerLoginRegisterActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()){
                                String error = task.getException().toString();
                                Toast.makeText(HomeownerLoginRegisterActivity.this, "Login failed: \n" + error, Toast.LENGTH_SHORT).show();
                                loader.dismiss();
                            }else {
                                Intent intent = new Intent(HomeownerLoginRegisterActivity.this, HomeownersMapsActivity.class);
                                startActivity(intent);
                                finish();
                                loader.dismiss();
                                return;

                            }
                        }
                    });
                }
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(authStateListener);
    }
}
