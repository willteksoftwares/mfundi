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

public class PlumberLoginRegisterActivity extends AppCompatActivity {

    private TextView plumberStatus, plumberLoginPageQuestion, plumberRegisterPageQuestion;
    private Button plumberLoginBtn, plumberRegistrationBtn;
    private EditText plumberEmail, plumberPassword;

    private FirebaseAuth mAuth;
    private ProgressDialog loader;
    private FirebaseAuth.AuthStateListener authStateListener;
    private DatabaseReference plumberDatabaseRef;
    private String currentPlumberOnlineID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plumber_login_register);

        plumberStatus = findViewById(R.id.plumberStatus);
        plumberLoginPageQuestion = findViewById(R.id.plumberLoginPageQuestion);
        plumberLoginBtn = findViewById(R.id.plumberLoginBtn);
        plumberRegistrationBtn = findViewById(R.id.plumberRegisterBtn);
        plumberEmail = findViewById(R.id.plumberLoginRegisterEmail);
        plumberPassword = findViewById(R.id.plumberLoginRegisterPassword);
        plumberRegisterPageQuestion = findViewById(R.id.plumberRegisterPageQuestion);

        loader = new ProgressDialog(this);
        mAuth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user!= null){
                    Intent intent = new Intent(PlumberLoginRegisterActivity.this, PlumbersMapsActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        plumberRegistrationBtn.setVisibility(View.INVISIBLE);
        plumberRegistrationBtn.setEnabled(false);

        plumberRegisterPageQuestion.setVisibility(View.INVISIBLE);
        plumberRegisterPageQuestion.setEnabled(false);

        plumberLoginPageQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                plumberRegistrationBtn.setEnabled(true);
                plumberRegistrationBtn.setVisibility(View.VISIBLE);
                plumberStatus.setText("Plumber Registration");
                plumberLoginPageQuestion.setVisibility(View.INVISIBLE);

                plumberLoginBtn.setVisibility(View.INVISIBLE);
                plumberLoginBtn.setEnabled(false);

                plumberRegisterPageQuestion.setEnabled(true);
                plumberRegisterPageQuestion.setVisibility(View.VISIBLE);
            }
        });

        plumberRegisterPageQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                plumberRegisterPageQuestion.setVisibility(View.INVISIBLE);
                plumberRegisterPageQuestion.setEnabled(false);
                plumberRegistrationBtn.setEnabled(false);
                plumberRegistrationBtn.setVisibility(View.INVISIBLE);
                plumberStatus.setText("Plumber Login");
                plumberLoginPageQuestion.setVisibility(View.VISIBLE);

                plumberLoginBtn.setVisibility(View.VISIBLE);
                plumberLoginBtn.setEnabled(true);
            }
        });

        plumberRegistrationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = plumberEmail.getText().toString();
                final  String password = plumberPassword.getText().toString();

                if (TextUtils.isEmpty(email)){
                    plumberEmail.setError("Email Required!");
                    return;
                }
                if (TextUtils.isEmpty(password)){
                    plumberPassword.setError("Password Required!");
                    return;
                } else {
                    loader.setMessage("Login in progress...");
                    loader.setCanceledOnTouchOutside(false);
                    loader.show();

                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(PlumberLoginRegisterActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()){
                                String error = task.getException().toString();
                                Toast.makeText(PlumberLoginRegisterActivity.this, "Registration Failed: \n" + error, Toast.LENGTH_SHORT).show();
                                loader.dismiss();
                            }else {
                                currentPlumberOnlineID = mAuth.getCurrentUser().getUid();
                                plumberDatabaseRef = FirebaseDatabase.getInstance().getReference().child("users").child("plumbers").child(currentPlumberOnlineID);
                                plumberDatabaseRef.setValue(true);

                                Intent intent = new Intent(PlumberLoginRegisterActivity.this, PlumbersMapsActivity.class);
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

        plumberLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = plumberEmail.getText().toString();
                final  String password = plumberPassword.getText().toString();

                if (TextUtils.isEmpty(email)){
                    plumberEmail.setError("Email Required!");
                    return;
                }
                if (TextUtils.isEmpty(password)){
                    plumberPassword.setError("Password Required!");
                    return;
                }else {
                    loader.setMessage("Login in progress...");
                    loader.setCanceledOnTouchOutside(false);
                    loader.show();

                    mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(PlumberLoginRegisterActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()){
                                String error = task.getException().toString();
                                Toast.makeText(PlumberLoginRegisterActivity.this, "Login failed: \n" + error, Toast.LENGTH_SHORT).show();
                                loader.dismiss();
                            }else {
                                Intent intent = new Intent(PlumberLoginRegisterActivity.this, PlumbersMapsActivity.class);
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
