package com.wearable.sound.service;
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Wearable;
import static com.wearable.sound.utils.Constants.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;



public class FeedbackSoundService extends IntentService {

    private static final String TAG = "FeedbackSoundService";

    private Set<String> connectedHostIds;

    public FeedbackSoundService() {
        super("FeedbackSoundService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "onHandleIntent(): " + intent);
        if (intent != null) {
            final int soundNotificationID = intent.getIntExtra(SOUND_ID, 0);
            final String soundLabel = intent.getStringExtra(SOUND_LABEL);
            final String feedback = intent.getStringExtra(FEEDBACK);
              String input = intent.getStringExtra(CONNECTED_HOST_IDS);
            if (input != null) {
                // There is a connected phone
                final Set<String> connectedHostIds = new HashSet<>(
                        Arrays.asList(
                                input.split(",")
                        )
                );
                this.connectedHostIds = connectedHostIds;
            }
            // Send a message to Phone to indicate this sound is given a feedback
            sendFeedbackSoundMessageToPhone(feedback+"_"+soundLabel);

        }
    }

    private void sendFeedbackSoundMessageToPhone(String feedback) {
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bas);
        if (connectedHostIds == null) {
            // No phone connected to send the message right now
            return;
        }
        for (String connectedHostId : connectedHostIds) {
            Log.d(TAG, "Sending feedback sound data to phone: " + feedback);
            Task<Integer> sendMessageTask =
                    Wearable.getMessageClient(this.getApplicationContext())
                            .sendMessage(connectedHostId, SOUND_FEEDBACK_FROM_WATCH_PATH, feedback.getBytes());
        }
    }
}
