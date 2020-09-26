package com.wearable.sound.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wearable.sound.R;

import java.util.Objects;

public class HelpFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
//        if (container != null) {
//            container.setVisibility(View.GONE);
//        }
        View view = inflater.inflate(R.layout.help_fragment, container, false);
        final Button tutorialBtn = view.findViewById(R.id.tutorial_btn);
        tutorialBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ScrollingFragment2", "onClick called");
                Intent tutorial = new Intent(HelpFragment.this.getActivity(), Tutorial.class);
                startActivity(tutorial);
            }
        });

        Display display = requireActivity().getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        ConstraintLayout constraintLayout = view.findViewById(R.id.help_layout);
        ViewGroup.LayoutParams layoutParams = constraintLayout.getLayoutParams();
        layoutParams.width = point.x;
        layoutParams.height = (int)(point.y * 0.86);
        constraintLayout.setLayoutParams(layoutParams);
        return view;
    }
}