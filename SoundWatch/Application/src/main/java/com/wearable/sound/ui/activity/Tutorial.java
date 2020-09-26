package com.wearable.sound.ui.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.hololo.tutorial.library.Step;
import com.hololo.tutorial.library.TutorialActivity;
import com.wearable.sound.R;

public class Tutorial extends TutorialActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TutorialActivity", "onCreated called");
        addFragment( new Step.Builder().setTitle("Step 1")
                .setContent("Select the sounds you want to recognize by simply unchecking and rechecking the boxes next to the sound title. ")
//                .setBackgroundColor(Color.parseColor("#00DC79"))
                .setBackgroundColor(Color.parseColor("#212121"))
                .setDrawable(R.drawable.tutorial_1)
                .build());

        addFragment( new Step.Builder().setTitle("Step 1")
                .setBackgroundColor(Color.parseColor("#212121"))
                .setDrawable(R.drawable.tutorial_2)
                .build());

        addFragment( new Step.Builder().setTitle("Step 3")
                .setContent("Check out the Help section for more instructions on " +
                "using the app and the About section to learn more about the background behind SmartWatch.  ")
                .setBackgroundColor(Color.parseColor("#212121"))
                .setDrawable(R.drawable.ic_baseline_help_48)
                .build());

        addFragment( new Step.Builder().setTitle("Step 1")
                .setContent("Press on the microphone icon to begin listening for sounds " +
                "(a Paired with \"Your Device\" notification should appear)")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_1)
                .build());

        addFragment( new Step.Builder().setTitle("Step 2")
                .setContent("The red icon with pulsing effect mean your watch is listening")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_2)
                .build());

        addFragment( new Step.Builder().setTitle("Step 3")
                .setContent("Go about your business and notifications should start " +
                "to appear identifying sounds")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_6)
                .build());

        addFragment( new Step.Builder().setTitle("Step 4")
                .setContent("To snooze a sound for a specified period of time, press the “snooze” button.\n")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_3)
                .build());

        addFragment( new Step.Builder().setTitle("Step 5").setContent("To pick the length of time of the snooze, press the “Open” " +
                "button and a list of options will be presented.  \n")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_4)
                .build());

        addFragment( new Step.Builder().setTitle("Step 6")
                .setContent("Click on the length of time you want to snooze.  \n")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_5)
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