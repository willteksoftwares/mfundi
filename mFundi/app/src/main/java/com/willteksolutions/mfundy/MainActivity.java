package com.willteksolutions.mfundy;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button homeownerBtn, plumberBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        homeownerBtn  = findViewById(R.id.homeowner);
        plumberBtn = findViewById(R.id.plumber);

        homeownerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HomeownerLoginRegisterActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

       plumberBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               Intent intent = new Intent(MainActivity.this, PlumberLoginRegisterActivity.class);
               startActivity(intent);
               finish();
               return;
           }
       });

    }
}
