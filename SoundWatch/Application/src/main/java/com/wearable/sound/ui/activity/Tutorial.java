package com.wearable.sound.ui.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.hololo.tutorial.library.Step;
import com.hololo.tutorial.library.TutorialActivity;
import com.wearable.sound.R;
import com.wearable.sound.utils.Constants;

public class Tutorial extends TutorialActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Constants.DEBUG_LOG) Log.d("TutorialActivity", "onCreated called");

        addFragment(new Step.Builder().setTitle("INSTRUCTIONS")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.warning)
                .build());

        addFragment(new Step.Builder().setTitle("INSTRUCTIONS")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.data_privacy)
                .build());

        addFragment(new Step.Builder().setTitle("INSTRUCTIONS")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.important_notice)
                .build());

        addFragment(new Step.Builder().setTitle("Tutorial 1")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_1)
                .build());

        addFragment(new Step.Builder().setTitle("Tutorial 2")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_2)
                .build());

        addFragment(new Step.Builder().setTitle("Tutorial 3")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_3)
                .build());

        addFragment(new Step.Builder().setTitle("Tutorial 4")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_4)
                .build());

        addFragment(new Step.Builder().setTitle("Tutorial 5")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_5)
                .build());

        addFragment(new Step.Builder().setTitle("Tutorial 5")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_6)
                .build());

    }

    @Override
    public void currentFragmentPosition(int position) {
//        Toast.makeText(this,"Position : " + position,Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onClick(View v) {
        if (Constants.DEBUG_LOG) Log.d("TutorialActivity", "onClick called");
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