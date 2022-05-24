package com.wearable.sound.ui.activity;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.wearable.sound.R;

public class HelpFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.help_fragment, container, false);
        final Button tutorialBtn = view.findViewById(R.id.tutorial_btn);
        tutorialBtn.setOnClickListener(v -> {
            Log.d("HelpFragment", "onClick called");
            Intent tutorial = new Intent(HelpFragment.this.getActivity(), Tutorial.class);
            startActivity(tutorial);
        });

        final Button watchTutorialBtn = view.findViewById(R.id.watch_tutorial_btn);
        watchTutorialBtn.setOnClickListener(v -> {
            Log.d("HelpFragment", "onClick called");
            Intent tutorial = new Intent(HelpFragment.this.getActivity(), WatchTutorial.class);
            startActivity(tutorial);
        });

        Display display = requireActivity().getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        ConstraintLayout constraintLayout = view.findViewById(R.id.help_layout);
        ViewGroup.LayoutParams layoutParams = constraintLayout.getLayoutParams();
        layoutParams.width = point.x;
        layoutParams.height = (int)(point.y * 0.95);
        constraintLayout.setLayoutParams(layoutParams);
        return view;
    }
}