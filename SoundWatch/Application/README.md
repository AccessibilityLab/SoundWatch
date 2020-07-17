
Introduction
------------

Pre-requisites
--------------
- Android SDK 28
- Android Build Tools v28.0.3
- Android Support Repository
- Get the Tensorflow lite model that is open sourced [here]()

Screenshots
-------------
Refer to main documentation

Core functions
-------------
[onMessageReceived](src/main/java/com/wearable/sound/ui/activity/datalayer/DataLayerListenerService.java)
-----
Description:
Main gateway to receive message from watch including various messages
- `SOUND_SNOOZE_FROM_WATCH_PATH`
- `SOUND_UNSNOOZE_FROM_WATCH_PATH`
- `SEND_CURRENT_BLOCKED_SOUND_PATH`
- `AUDIO_PREDICTIOn`


[processAudioRecognition](src/main/java/com/wearable/sound/ui/activity/datalayer/DataLayerListenerService.java)
------
Description:
Run different audio prediction algorithms depends on different conditions:
- Process by raw audio or MFCC features
- Process directly on phone (running ML model on phone)
- Send data to server through socket and wait for response from server


[sendSoundFeaturesToServer](src/main/java/com/wearable/sound/ui/activity/datalayer/DataLayerListenerService.java)
-----
Description: Send socket events to server to process audio recognition



[predictSoundsFromRawAudio](src/main/java/com/wearable/sound/ui/activity/datalayer/DataLayerListenerService.java)
-------
Description: Convert raw audio sound of 16000 float elements (in 1 second) to corresponding label by using `tflite` model


Parameters:
- `soundBuffer`: `List<Short>`
- `recordTime`: `long`

Pseudocode:
1. Check if decibel threshold is large enough (to ensure the sound is sufficiently large)
2. Compute [MFCC features](https://en.wikipedia.org/wiki/Mel-frequency_cepstrum) from raw audio by utilizing [Chaquopy](https://chaquo.com/chaquopy/) to run `python` function.
3. If the accuracy is larger than the minimum threshold, the sound label is sent to `MainActivity` to display watch notification by sending a `BroadCastIntent`

[SendAudioLabelToWearTask](src/main/java/com/wearable/sound/ui/activity/datalayer/DataLayerListenerService.java)
-----
Description: Send serialized `AudioLabel` as a concatenated string object back to watch for displaying notification





Folder Structure
-------------

Getting Started
---------------

- Point buildPython in build.grade to your local python installation (both of application and wearable modules), i.e:

```gradle
        python {
            // If Chaquopy fails to find Python on your build machine, enable the following
            // line and edit it to point to Python 3.4 or later.
              buildPython "C:/Python36/python3.exe"
//            buildPython "C:\\Users\\hungn\\AppData\\Local\\Programs\\Python\\Python36\\python.exe"
            pip {
                install "numpy==1.14.2"
            }
        }
        ndk {
            abiFilters "armeabi-v7a", "x86"
        }
```

- Let gradle configure and install dependencies for both `Application` and `Wearable` projects.
- On top toolbar of Android Studio, make sure `Application` is chosen and click `Run` button. It is much preferred to use a physical Android Wear device since it is how we develop and tests the project. Otherwise, refer to the Android [documentation](https://developer.android.com/training/wearables/apps/creating) to set up the virtual Android Watch

- Point buildPython in build.grade to your local python installation (both of application and wearable modules)
- Change ARCHITECTURE in MainActivity for both Phone and Watch to switch between Architectures
- please copy the `tflite` model and `labels.txt` into `src/assets` folder in Watch `Application` src folder, and specify the filename inside MODEL_FILENAME in MainActivity
- please copy the `tflite` model and `labels.txt` into `src/assets` folder in Watch `Wearable` src folder, and specify the filename inside MODEL_FILENAME in MainActivity
- WARNING: If use WATCH_ONLY_ARCHITECTURE, please copy the tflite model into assets folder in Wearable src folder, and specify the filename inside MODEL_FILENAME in SoundRecorder
- Change AUDIO_TRANMISSION_STYLE in MainActivity for both Phone and Watch to change the Audio Transmission Style (Raw Audio vs. Audio Features)


Tests
-------

- For Model Latency, enable "TEST_MODEL_LATENCY" in MainActivity for both Watch and Phone
- For E2E Latency, enable "TEST_E2E_LATENCY" in MainActivity for both Watch and Phone

Support
-------
Contact [Hung V Ngo](www.hungvngo.com) @MakeabilityLab through email `hvn297` at cs.washington.edu
Developed with [Dhruv Jain](https://homes.cs.washington.edu/~djain/) and collaborators at [MakeabilityLab](https://makeabilitylab.cs.washington.edu/)
