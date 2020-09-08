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
        addFragment( new Step.Builder().setTitle("Select the sounds you want to recognize by simply unchecking and rechecking the boxes next to the sound title. ")
//                .setBackgroundColor(Color.parseColor("#00DC79"))
                .setBackgroundColor(Color.parseColor("#A9A9A9"))
                .setDrawable(R.drawable.tutorial_1)
                .setSummary("Phone Tutorial")
                .build());

        addFragment( new Step.Builder().setTitle("Step 2")
                .setBackgroundColor(Color.parseColor("#A9A9A9"))
                .setDrawable(R.drawable.tutorial_2)
                .build());

        addFragment( new Step.Builder().setTitle("Check out the Help section for more instructions on " +
                "using the app and the About section to learn more about the background behind SmartWatch.  ")
                .setBackgroundColor(Color.parseColor("#A9A9A9"))
                .setDrawable(R.drawable.ic_baseline_help_48)
                .setSummary("Up next: Watch Tutorial")
                .build());

        addFragment( new Step.Builder().setTitle("Press on the microphone icon to begin listening for sounds " +
                "(a Paired with \"Your Device\" notification should appear)")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_1)
                .setSummary("Watch Tutorial - Step 1")
                .build());

        addFragment( new Step.Builder().setTitle("The red icon with pulsing effect mean your watch is listening")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_2)
                .setSummary("Watch Tutorial - Step 2")
                .build());

        addFragment( new Step.Builder().setTitle("Go about your business and notifications should start " +
                "to appear identifying sounds")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_6)
                .setSummary("Watch Tutorial - Step 3")
                .build());

        addFragment( new Step.Builder().setTitle("To snooze a sound for a specified period of time, press the “snooze” button.\n")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_3)
                .setSummary("Watch Tutorial - Step 4")
                .build());

        addFragment( new Step.Builder().setTitle("To pick the length of time of the snooze, press the “Open” " +
                "button and a list of options will be presented.  \n")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_4)
                .setSummary("Watch Tutorial - Step 4")
                .build());

        addFragment( new Step.Builder().setTitle("Click on the length of time you want to snooze.  \n")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_5)
                .setSummary("Watch Tutorial - Step 5")
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