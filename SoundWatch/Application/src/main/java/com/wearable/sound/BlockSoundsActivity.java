package com.wearable.sound;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.WorkerThread;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class BlockSoundsActivity extends Activity {
    private static final String SPEECH = "Speech";
    private static final String KNOCKING = "Knocking";
    private static final String PHONE_RING = "Phone Ring";
    private static final String SOUND_ENABLE_FROM_PHONE_PATH = "/SOUND_ENABLE_FROM_PHONE_PATH";


    public static Map<String, SoundNotification> SOUNDS_MAP = new HashMap<String, SoundNotification>();
    private static final String TAG = "BlockSoundsActivity";

    //TODO HUNG 6: WHY ONLY 3 SOUNDS HERE?
    {
        SOUNDS_MAP.put(SPEECH, new SoundNotification(SPEECH, true, false));
        SOUNDS_MAP.put(KNOCKING, new SoundNotification(KNOCKING, true, false));
        SOUNDS_MAP.put(PHONE_RING, new SoundNotification(PHONE_RING, true, false));
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.block_sounds);
    }

    public void onCheckBoxClick(View view) {
        int id = view.getId();
        SoundNotification currentSound = null;
        switch (id) {
            case R.id.speech:
                currentSound = SOUNDS_MAP.get(SPEECH);
                break;
            case R.id.knocking:
                currentSound = SOUNDS_MAP.get(KNOCKING);
                break;
            case R.id.phone_ring:
                currentSound = SOUNDS_MAP.get(PHONE_RING);
                break;
            default:
                break;
        }

        if (currentSound != null) {
            boolean isEnabled = ((CheckBox) view).isChecked();
            currentSound.isEnabled = isEnabled;
            new sendSoundEnableMessageToWatchTask(currentSound).execute();
        }
    }

    public class sendSoundEnableMessageToWatchTask extends AsyncTask<Void, Void, Void> {
        private String data;

        public sendSoundEnableMessageToWatchTask(SoundNotification soundNotification) {
            data = soundNotification.label + "," + soundNotification.isEnabled + "," + soundNotification.isSnoozed;
        }
        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            Log.i(TAG, "Sending enabled data to watch " + nodes.size());
            for (String node : nodes) {
                Log.i(TAG, "Sending enabled data from phone: " + data);
                sendMessageWithData(node, SOUND_ENABLE_FROM_PHONE_PATH, data.getBytes());
            }
            return null;
        }
    }

    @WorkerThread
    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();

        Task<List<Node>> nodeListTask =
                Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();

        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            List<Node> nodes = Tasks.await(nodeListTask);

            for (Node node : nodes) {
                results.add(node.getId());
            }

        } catch (ExecutionException exception) {
            Log.e(TAG, "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }

        return results;
    }


    @WorkerThread
    private void sendMessageWithData(String node, String title, byte[] data) {
        Task<Integer> sendMessageTask =
                Wearable.getMessageClient(this)
                        .sendMessage(node, title, data);

        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            Integer result = Tasks.await(sendMessageTask);
            Log.e(TAG, "Message sent: " + result);

        } catch (ExecutionException exception) {
            Log.e(TAG, "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }
    }
}