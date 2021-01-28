package com.willteksolutions.mfundy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PlumberSettingsActivity extends AppCompatActivity {

    private EditText plumberName, plumberPhoneNumber,plumberCompany;
    private Button plumberConfirmSettingsBtn, plumberBackBtn;
    private ImageView plumberProfileImage;

    private FirebaseAuth mAuth;
    private DatabaseReference plumberDatabaseRef;
    private String userID;

    private ProgressDialog loader;

    private String mName = "";
    private String mPhone = "";
    private String mCompany = "";
    private String mProfilePicture = "";
    private Uri resultUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plumber_settings);

        plumberName = findViewById(R.id.plumberName);
        plumberPhoneNumber = findViewById(R.id.plumberPhoneNumber);
        plumberCompany = findViewById(R.id.plumberCompany);
        plumberConfirmSettingsBtn = findViewById(R.id.plumberConfirmSettingsBtn);
        plumberBackBtn = findViewById(R.id.plumberBackBtn);
        loader = new ProgressDialog(this);
        plumberProfileImage = findViewById(R.id.plumberProfileImage);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        plumberDatabaseRef = FirebaseDatabase.getInstance().getReference().child("users").child("plumbers").child(userID);
        getUserInfo();

        plumberProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });

        plumberConfirmSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                saveUserInformation();
            }
        });


        plumberBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;
            }
        });


    }

    private void getUserInfo(){
        plumberDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){

                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") !=null){
                        mName = map.get("name").toString();
                        plumberName.setText(mName);
                    }

                    if (map.get("phone") !=null){
                        mPhone = map.get("phone").toString();
                        plumberPhoneNumber.setText(mPhone);
                    }
                    if (map.get("location") !=null){
                        mCompany = map.get("location").toString();
                        plumberCompany.setText(mCompany);
                    }

                    if (map.get("profile picture url") !=null){
                        mProfilePicture = map.get("profile picture url").toString();
                        Glide.with(getApplication()).load(mProfilePicture).into(plumberProfileImage);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void saveUserInformation() {
        final String name = plumberName.getText().toString();
        final String phone = plumberPhoneNumber.getText().toString();
        final String company = plumberCompany.getText().toString();

        if (TextUtils.isEmpty(name)){
            plumberName.setError("Name is required!");
            return;
        }
        if (TextUtils.isEmpty(phone)){
            plumberPhoneNumber.setError("Phone number is required!");
            return;
        }
        if (TextUtils.isEmpty(company)){
            plumberCompany.setError("organization required!");
            return;
        }else {
            loader.setMessage("Uploading details");
            loader.setCanceledOnTouchOutside(false);
            loader.show();

            Map userInfo = new HashMap();
            userInfo.put("name",name);
            userInfo.put("phone", phone);
            userInfo.put("company", company);

            plumberDatabaseRef.updateChildren(userInfo).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()){
                        Toast.makeText(PlumberSettingsActivity.this, "Updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                        loader.dismiss();
                    }else {
                        String error = task.getException().toString();
                        Toast.makeText(PlumberSettingsActivity.this, "Update Failed: "+ error, Toast.LENGTH_SHORT).show();
                        finish();
                        loader.dismiss();
                    }
                }
            });
        }

        if (resultUri !=null){
            final StorageReference filepath = FirebaseStorage.getInstance().getReference().child("profile pictures").child(userID);
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ByteArrayOutputStream byteArrayOutputStStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20,byteArrayOutputStStream);
            byte[] data = byteArrayOutputStStream.toByteArray();
            UploadTask uploadTask = filepath.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                    return;
                }
            });

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    if (taskSnapshot.getMetadata() != null) {
                        if (taskSnapshot.getMetadata().getReference() != null) {
                            Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                            result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String imageUrl = uri.toString();
                                    Map newImageMap = new HashMap();
                                    newImageMap.put("profile picture url", imageUrl);
                                    plumberDatabaseRef.updateChildren(newImageMap).addOnCompleteListener(new OnCompleteListener() {
                                        @Override
                                        public void onComplete(@NonNull Task task) {
                                            if (task.isSuccessful()){
                                                Toast.makeText(PlumberSettingsActivity.this, "Image uploaded and link gotten successfully", Toast.LENGTH_SHORT).show();
                                            }else {
                                                String error = task.getException().toString();
                                                Toast.makeText(PlumberSettingsActivity.this, "Process failed "+ error, Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                    finish();
                                }
                            });
                        }
                    }
                }
            });
        }else {
            finish();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode ==1 && resultCode == Activity.RESULT_OK ){
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            plumberProfileImage.setImageURI(resultUri);
        }
    }
}

