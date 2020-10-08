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

        addFragment( new Step.Builder().setTitle("INSTRUCTIONS")
                .setContent("The SoundWatch app on the phone needs to be opened in order for the watch app to start listening.")
//                .setBackgroundColor(Color.parseColor("#00DC79"))
                .setBackgroundColor(Color.parseColor("#212121"))
                .setDrawable(R.drawable.warning)
                .build());

        addFragment( new Step.Builder().setTitle("How-to 1")
                .setContent("Select the sounds to recognize by checking the boxes next to the sound title. To stop recognizing a certain sound, simply uncheck that box.")
//                .setBackgroundColor(Color.parseColor("#00DC79"))
                .setBackgroundColor(Color.parseColor("#212121"))
                .setDrawable(R.drawable.tutorial_1)
                .build());

        addFragment( new Step.Builder().setTitle("How-to 2")
                .setBackgroundColor(Color.parseColor("#212121"))
                .setDrawable(R.drawable.tutorial_2)
                .build());

        addFragment( new Step.Builder().setTitle("How-to 3")
                .setContent("Check out the Help section for more instructions on " +
                "using the app, and the About section to learn more about the background behind SmartWatch.")
                .setBackgroundColor(Color.parseColor("#212121"))
                .setDrawable(R.drawable.tutorial_3)
                .build());

        addFragment( new Step.Builder().setTitle("WearOS How-to 1")
                .setContent("Press on the microphone icon to begin listening." +
                "A 'Connected:[Your Device]' notification will appear")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_1)
                .build());

        addFragment( new Step.Builder().setTitle("WearOS How-to 2")
                .setContent("The red icon with pulsing effect means \n your watch is listening.")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_2)
                .build());

        addFragment( new Step.Builder().setTitle("WearOS How-to 3")
                .setContent("Go about your business and notifications will start " +
                "to appear identifying sounds")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_6)
                .build());

        addFragment( new Step.Builder().setTitle("WearOS How-to 4")
                .setContent("To snooze a sound for a specified period of time, \n press the “snooze” button.\n")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_3)
                .build());

        addFragment( new Step.Builder().setTitle("WearOS How-to 5").setContent("To pick the length of time of the snooze, \n press the “Open” " +
                "button and a list of options will be presented.  \n")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_4)
                .build());

        addFragment( new Step.Builder().setTitle("WearOS How-to 6")
                .setContent("Click on the length of time you want to snooze.  \n")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_5)
                .build());

        addFragment( new Step.Builder().setTitle("WearOS How-to 7")
                .setContent("You can completely disable listening by turning ON \"Sleep Mode\" on the Settings page. \n This will not allow the watch to start listening again until Sleep Mode is turned OFF.\n")
                .setBackgroundColor(R.color.colorNavBar)
                .setDrawable(R.drawable.watch_7)
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