package com.wearable.sound.ui.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.hololo.tutorial.library.PermissionStep;
import com.hololo.tutorial.library.Step;
import com.hololo.tutorial.library.TutorialActivity;
import com.wearable.sound.R;

public class Tutorial extends TutorialActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TutorialActivity", "onCreated called");
        addFragment( new Step.Builder().setTitle("Step 1")
                .setContent("Content 1")
                .setBackgroundColor(Color.parseColor("#00DC79"))
                .setDrawable(R.drawable.ss_1)
                .setSummary("Summary 1")
                .build());

        addFragment( new Step.Builder().setTitle("Step 2")
                .setContent("Content 2")
                .setBackgroundColor(Color.parseColor("#00DC79"))
                .setDrawable(R.drawable.ic_outline_about_48)
                .setSummary("Summary 2")
                .build());

        addFragment( new Step.Builder().setTitle("Step 3")
                .setContent("Content 3")
                .setBackgroundColor(Color.parseColor("#00DC79"))
                .setDrawable(R.drawable.ss_1)
                .setSummary("Summary 3")
                .build());


    }

    @Override
    public void currentFragmentPosition(int position) {
        Toast.makeText(this,"Position : " + position,Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onClick(View v) {
        Log.d("TutorialActivity", "onClick called");
        super.onClick(v);
        switch (v.getId()) {
            case R.id.prev:
//                Toast.makeText(this, "Back button clicked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.next:
//                Toast.makeText(this, "Next button clicked", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}