package com.example.kuripothub;

import android.os.Bundle;
import android.widget.Button;
import android.content.Intent;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class PrefaceActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preface_activity); // Make sure this XML exists in res/layout


        Button finishButton = findViewById(R.id.finishButton);
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PrefaceActivity.this, ExpenseTrackingActivity.class);
                startActivity(intent);
            }
        });
    }
}