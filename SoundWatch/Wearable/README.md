
Introduction
------------
This folder contains the SoundWatch application on the Android phone. 

## Features
- Record audio in the background and continuously recognizing sounds based on different architectures
- Display sound notification with vibration


Folder Structure
-------------
- `service` folder contains `ForegroundService` to continuously record audio data and stream data to the ML model or to phone/server and `SnoozeSoundService` to listen for snooze events from the Android phone app to stop listen to particular sounds.
- `DataLayerListenerService` is the main service to receive messages from watch, including the predicted sound label from the phone as well as snooze events.
- `python` folder contains necessary Python utility functions to convert raw audio to MFCC features from `vggish_input.py` through function `audio_samples` in `main.py`

Core functions
-------------
[predictSoundsFromRawAudio](src/main/java/com/wearable/sound/utils/SoundRecorder.java)
-------
Description: Convert raw audio sound of 16000 float elements (in 1 second) to corresponding label by using `tflite` model


Parameters:
- `soundBuffer`: `List<Short>`
- `recordTime`: `long`

Pseudocode: 
1. Check if decibel threshold is large enough (to ensure the sound is sufficiently large)
2. Compute [MFCC features](https://en.wikipedia.org/wiki/Mel-frequency_cepstrum) from raw audio by utilizing [Chaquopy](https://chaquo.com/chaquopy/) to run `python` function.  
3. If the accuracy is larger than the minimum threshold, the sound label is sent to `MainActivity` to display watch notification by sending a `BroadCastIntent`

[createAudioLabelNotification](src/main/java/com/wearable/sound/ui/activity/MainActivity.java)
----------

Description: Display audio notification to watch with vibration which shows sound label and accuracy percentage

Parameters:
- `audioLabel`: `AudioLabel`

Pseudocode:
- Take in an `AudioLabel` object that has all information about the sound (label, accuracy) to display. Use [Android Notification](https://developer.android.com/training/notify-user/build-notification) feature of Android to display the notification

## Warning ##
If use WATCH_ONLY_ARCHITECTURE, please copy the tflite model into assets folder in Wearable src folder, and specify the filename inside MODEL_FILENAME in SoundRecorder but the watch can be heated up fast so be careful!
