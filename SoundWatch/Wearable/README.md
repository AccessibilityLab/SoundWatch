
Introduction
------------

Pre-requisites
--------------

- Android SDK 28
- Android Build Tools v28.0.3
- Android Support Repository
- Get the Tensorflow lite model that is open sourced [here]()


Folder Structure
-------------


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
- On top toolbar of Android Studio, make sure `Wearable` is chosen and click `Run` button. It is much preferred to use a physical Android Wear device since it is how we develop and tests the project. Otherwise, refer to the Android [documentation](https://developer.android.com/training/wearables/apps/creating) to set up the virtual Android Watch


Configurations
---------------
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
