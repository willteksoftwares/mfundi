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
import com.google.android.libraries.places.api.Places;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeownerSettingsActivity extends AppCompatActivity {

    private EditText homeownerName, homeownerPhoneNumber,homeownerLocation;
    private Button homeownerConfirmSettingsBtn, homeownerBackBtn;
    private ImageView homeownerProfileImage;

    private FirebaseAuth mAuth;
    private DatabaseReference homeownerDatabaseRef;
    private String userID;

    private ProgressDialog loader;

    private String mName = "";
    private String mPhone = "";
    private String mLocation = "";
    private String mProfilePicture = "";
    private Uri resultUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homeowner_settings);

        homeownerName = findViewById(R.id.homeownerName);
        homeownerPhoneNumber = findViewById(R.id.homeownerPhoneNumber);
        homeownerLocation = findViewById(R.id.homeownerLocation);
        homeownerConfirmSettingsBtn = findViewById(R.id.homeownerConfirmSettingsBtn);
        homeownerBackBtn = findViewById(R.id.homeownerBackBtn);
        loader = new ProgressDialog(this);
        homeownerProfileImage = findViewById(R.id.homeownerProfileImage);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        homeownerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("users").child("homeowners").child(userID);
        getUserInfo();

        homeownerProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Intent intent = new Intent(Intent.ACTION_PICK);
               intent.setType("image/*");
               startActivityForResult(intent, 1);
            }
        });

        homeownerConfirmSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                saveUserInformation();
            }
        });


        homeownerBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;
            }
        });


    }

    private void getUserInfo(){
        homeownerDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
              if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){

                  Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                  if (map.get("name") !=null){
                      mName = map.get("name").toString();
                      homeownerName.setText(mName);
                  }

                  if (map.get("phone") !=null){
                      mPhone = map.get("phone").toString();
                      homeownerPhoneNumber.setText(mPhone);
                  }
                  if (map.get("location") !=null){
                      mLocation = map.get("location").toString();
                      homeownerLocation.setText(mLocation);
                  }

                  if (map.get("profile picture url") !=null){
                      mProfilePicture = map.get("profile picture url").toString();
                      Glide.with(getApplication()).load(mProfilePicture).into(homeownerProfileImage);
                  }

              }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void saveUserInformation() {
        final String name = homeownerName.getText().toString();
        final String phone = homeownerPhoneNumber.getText().toString();
        final String location = homeownerLocation.getText().toString();

        if (TextUtils.isEmpty(name)){
            homeownerName.setError("Name is required!");
            return;
        }
        if (TextUtils.isEmpty(phone)){
            homeownerPhoneNumber.setError("Phone number is required!");
            return;
        }
        if (TextUtils.isEmpty(location)){
            homeownerLocation.setError("Location required!");
            return;
        }else {
            loader.setMessage("Uploading details");
            loader.setCanceledOnTouchOutside(false);
            loader.show();

            Map userInfo = new HashMap();
            userInfo.put("name",name);
            userInfo.put("phone", phone);
            userInfo.put("location", location);

            homeownerDatabaseRef.updateChildren(userInfo).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()){
                        Toast.makeText(HomeownerSettingsActivity.this, "Updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                        loader.dismiss();
                    }else {
                        String error = task.getException().toString();
                        Toast.makeText(HomeownerSettingsActivity.this, "Update Failed: "+ error, Toast.LENGTH_SHORT).show();
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
                                    homeownerDatabaseRef.updateChildren(newImageMap).addOnCompleteListener(new OnCompleteListener() {
                                        @Override
                                        public void onComplete(@NonNull Task task) {
                                           if (task.isSuccessful()){
                                               Toast.makeText(HomeownerSettingsActivity.this, "Image uploaded and link gotten successfully", Toast.LENGTH_SHORT).show();
                                           }else {
                                               String error = task.getException().toString();
                                               Toast.makeText(HomeownerSettingsActivity.this, "Process failed "+ error, Toast.LENGTH_SHORT).show();
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
            homeownerProfileImage.setImageURI(resultUri);
        }
    }
}
