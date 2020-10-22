package com.wearable.sound.ui.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.hololo.tutorial.library.Step;
import com.hololo.tutorial.library.TutorialActivity;
import com.wearable.sound.R;

public class WatchTutorial extends TutorialActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TutorialActivity", "onCreated called");

        addFragment( new Step.Builder().setTitle("WearOS Tutorial 1")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.watch_tutorial_1)
                .build());

        addFragment( new Step.Builder().setTitle("WearOS Tutorial 2")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.watch_tutorial_2)
                .build());

        addFragment( new Step.Builder().setTitle("WearOS Tutorial 3")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.watch_tutorial_3)
                .build());

        addFragment( new Step.Builder().setTitle("WearOS Tutorial 4")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.watch_tutorial_4)
                .build());

        addFragment( new Step.Builder().setTitle("WearOS Tutorial 5")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.watch_tutorial_5)
                .build());
    }

    @Override
    public void currentFragmentPosition(int position) {
//        Toast.makeText(this,"Position : " + position,Toast.LENGTH_SHORT).show();
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