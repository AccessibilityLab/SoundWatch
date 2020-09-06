package com.wearable.sound.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wearable.sound.R;

public class HelpFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
//        if (container != null) {
//            container.setVisibility(View.GONE);
//        }
        View view = inflater.inflate(R.layout.fragment_scrolling2, container, false);
        final Button tutorialBtn = view.findViewById(R.id.tutorial_btn);
        tutorialBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ScrollingFragment2", "onClick called");
                Intent tutorial = new Intent(HelpFragment.this.getActivity(), Tutorial.class);
                startActivity(tutorial);
            }
        });
        return view;
    }
}