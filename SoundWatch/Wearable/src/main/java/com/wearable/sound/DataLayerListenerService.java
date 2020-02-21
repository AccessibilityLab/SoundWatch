/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wearable.sound;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import java.util.List;

/** Listens to DataItems and Messages from the local node. */
public class DataLayerListenerService extends WearableListenerService {

    private static final String TAG = "DataLayerService";

    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";
    private static final String AUDIO_PREDICTION_PATH = "/audio-prediction";
    private static final String SOUND_ENABLE_FROM_PHONE_PATH = "/SOUND_ENABLE_FROM_PHONE_PATH";
    private static final String SEND_CURRENT_BLOCKED_SOUND_PATH = "/SEND_CURRENT_BLOCKED_SOUND_PATH";
    public static final String COUNT_PATH = "/count";
    public static final String AUDIO_LABEL_FROM_PHONE = "AUDIO_LABEL_FROM_PHONE";

    @Override
    public void onPeerConnected(Node node) {
        super.onPeerConnected(node);
        Log.i(TAG, "onPeerConnected()");
        List blockedSounds = ((MyApplication) this.getApplication()).getBlockedSounds();
        Task<Integer> sendMessageTask =
                Wearable.getMessageClient(this)
                        .sendMessage(node.getId(), SEND_CURRENT_BLOCKED_SOUND_PATH,
                                String.join(",", blockedSounds).getBytes());
    }

    @Override
    public void onPeerDisconnected(Node node) {
        super.onPeerDisconnected(node);
        Log.i(TAG, "onPeerDisconnected()");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged: " + dataEvents);

        // Loop through the events and send a message back to the node that created the data item.
        for (DataEvent event : dataEvents) {
            Uri uri = event.getDataItem().getUri();
            String path = uri.getPath();

            if (COUNT_PATH.equals(path)) {
                // Get the node id of the node that created the data item from the host portion of
                // the uri.
                String nodeId = uri.getHost();
                // Set the data of the message to be the bytes of the Uri.
                byte[] payload = uri.toString().getBytes();

                // Send the rpc
                // Instantiates clients without member variables, as clients are inexpensive to
                // create. (They are cached and shared between GoogleApi instances.)
                Task<Integer> sendMessageTask =
                        Wearable.getMessageClient(this)
                                .sendMessage(nodeId, DATA_ITEM_RECEIVED_PATH, payload);

                sendMessageTask.addOnCompleteListener(
                        new OnCompleteListener<Integer>() {
                            @Override
                            public void onComplete(Task<Integer> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "Message sent successfully");
                                } else {
                                    Log.d(TAG, "Message failed.");
                                }
                            }
                        });
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived: " + messageEvent);
        Log.d(TAG, "messagePath: " + messageEvent.getPath());
        // Check to see if the message is to start an activity
        if (messageEvent.getPath().equals(START_ACTIVITY_PATH)) {
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        } else if (messageEvent.getPath().equals(AUDIO_PREDICTION_PATH)) {
            Log.d(TAG, "Audio received from phone: " + new String(messageEvent.getData()));
            AudioLabel audioLabel = new AudioLabel(messageEvent.getData());
            /** Display Snooze on Phone**/
            Log.i(TAG, "Sending label broadcast to MainActivity");
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(MainActivity.mBroadcastSoundPrediction);
            broadcastIntent.putExtra(AUDIO_LABEL_FROM_PHONE, new String(messageEvent.getData()));
            sendBroadcast(broadcastIntent);
//            createAudioLabelNotification(audioLabel);
        } else if (messageEvent.getPath().equals(SOUND_ENABLE_FROM_PHONE_PATH)) {
            Log.d(TAG, "Received sound enabled from phone: " + new String(messageEvent.getData()));
            handleEnableSoundNotification(new String(messageEvent.getData()));
        }
    }

    public void handleEnableSoundNotification(String message) {
        String[] parts = message.split(",");
        if (parts.length != 3) {
            Log.i(TAG, "Malformed Sound Enabled notification " + message);
        }
        String soundLabel = parts[0];
        boolean isEnabled = Boolean.parseBoolean(parts[1]);
        boolean isSnoozed = Boolean.parseBoolean(parts[2]);
        List<String> enabledSounds = ((MyApplication) this.getApplication()).enabledSounds;

        Log.i(TAG, "handleEnableSoundNotificatioN()");

        if (isEnabled) {
            Log.i(TAG, "Enabling sound ");
            if (!enabledSounds.contains(soundLabel)) {
                Log.i(TAG, "Remove from list of blocked sounds " + soundLabel);
                ((MyApplication) this.getApplication()).addEnabledSound(soundLabel);
            }
        } else {
            // adding the sound from current sound list
            Log.i(TAG, "Disabling sound ");
            if (enabledSounds.contains(soundLabel)) {
                Log.i(TAG, "Add to list of blocked sounds " + soundLabel);
                ((MyApplication) this.getApplication()).removeEnabledSound(soundLabel);

            }
        }
    }

    public class AudioLabel {
        String label;
        double confidence;
        String time;
        public AudioLabel(byte[] data) {
            String message = new String(data);
            String[] parts = message.split(",");
            if (parts.length == 3) {
                label = parts[0];
                confidence = Double.parseDouble(parts[1]);
                time = parts[2];
            }
        }
    }
}
