
# SoundWatch #

![Status](https://img.shields.io/badge/Version-Experimental-brightgreen.svg)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Introduction
------------
SoundWatch is an Android-based app that leverages commercially available smartwatches to provide
glanceable, always-available, and private sound feedback in multiple contexts. Building from previous work, SoundWatch informs users about three key sound properties: sound identity, loudness, and time of
occurrence through customizable sound alerts using visual and vibration feedback. We use
a deep learning-based sound classification engine (running on either the watch or on the paired phone or the
cloud) to continually sense and process sound events in real-time. Below, we describe our sound classification
engine, our privacy-preserving sound sensing pipeline, system architectures, and implementation.

[[Website](https://makeabilitylab.cs.washington.edu/project/soundwatch/)]
[[Paper PDF](https://makeabilitylab.cs.washington.edu/media/publications/Jain_ExploringSmartwatchBasedDeepLearningApproachesToSupportSoundAwarenessForDeafAndHardOfHearingUsers_ASSETS2020.pdf)]

![SoundWatch system mockup](images/image.png?raw=true "Title")
![SoundWatch system mockup 1](images/image_1.png?raw=true "Title")

## Table Of Contents ##

0. [Prerequesites](#prerequesites)
1. [Setup](#setup)
2. [Architecture performance test](#test)
3. [Acknowledgement](#acknowledgement)
4. [Support](#support)

## Pre-requisites ##
--------------
- Latest Android Studio (The project is developed with Android Studio 4.0)
- Android SDK 28
- Android Build Tools v28.0.3
- Android Support Repository
- Get the sound classification Tensorflow lite model that is open sourced [here](https://www.dropbox.com/s/suwdf3bub5nd933/example_model.tflite?dl=0&fbclid=IwAR10xvhog9POOdamV7oUmOjkFQhiMNQp61ZZjaQwzVjD2RyY8fsZNznnMps)

Screenshots
-------------
![SoundWatch system mockup](images/demo.png?raw=true "Title")


Folder Structure
-------------
- `server`: Python server to serve predictions from raw audio data sent from phone or watch. Refer to [server documentation](server/README.md)
- `SoundWatch/Application`: Android phone application to configure list of displayed sounds on watch and serve audio predictions from watch. Refer to [phone app documentation](SoundWatch/Application/README.md)
- `SoundWatch/Wearable`: Android watch application to listen for ambient sounds and display the predicted sound list from ML model.Refer to [watch app documentation](SoundWatch/Wearable/README.md)

## Setup ##
---------------
- Point buildPython in `build.gradle` to your local python installation (both of application and wearable modules), i.e:

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

Configuration
-------------

- Let gradle configure and install dependencies for both `Application` and `Wearable` projects. 
- On top toolbar of Android Studio, make sure `Wearable` is chosen and click `Run` button. It is much preferred to use a physical Android Wear device since it is how we develop and tests the project. Otherwise, refer to the Android [documentation](https://developer.android.com/training/wearables/apps/creating) to set up the virtual Android Watch
- Point `buildPython` in `build.gradle` to your local python installation (both of application and wearable modules)
- Change `ARCHITECTURE` in MainActivity for both Phone and Watch to switch between Architectures
- Copy the `tflite` model and `labels.txt` into `src/assets` folder in Watch `Application` src folder, and specify the filename inside MODEL_FILENAME in MainActivity
- Copy the `tflite` model and `labels.txt` into `src/assets` folder in Watch `Wearable` src folder, and specify the filename inside MODEL_FILENAME in MainActivity
- WARNING: If use `WATCH_ONLY_ARCHITECTURE`, please copy the tflite model into assets folder in Wearable src folder, and specify the filename inside `MODEL_FILENAME` in SoundRecorder
- Change `AUDIO_TRANMISSION_STYLE` in `MainActivity.java` for both Phone and Watch to change the Audio Transmission Style (Raw Audio vs. Audio Features)


## Test ##
-------

- For Model Latency, enable "TEST_MODEL_LATENCY" in MainActivity for both Watch and Phone to get the time used to run the ML model for audio recognition
- For E2E Latency, enable "TEST_E2E_LATENCY" in MainActivity for both Watch and Phone to get the end to end latency from when the sound is first captured on the watch to the time when the notification is displayed on the watch.

After enabling the boolean flags, just run the watch and phone app like usual. The test results will be output as `*.txt` (i.e: `watch_model.txt`, `e2e_latency.txt`) to local device directory (Phone or watch) inside the `com.wearable.sound` folder.

## Support ##
-------
Contact [Hung V Ngo](www.hungvngo.com) @MakeabilityLab through email `hvn297` at cs.washington.edu
Developed with [Dhruv Jain](https://homes.cs.washington.edu/~djain/) and collaborators at [MakeabilityLab](https://makeabilitylab.cs.washington.edu/)

Drop us a note if you are using or plan to use SoundWatch for research purposes.

## Acknowledgement ##
-------
- The Android watch was built based on the [Data layer sample](https://github.com/android/wear-os-samples/tree/master/DataLayer) from Google Android repo to establish connection between phone and watch.
- Python server is created with [Socket.io](https://socket.io/blog/native-socket-io-and-android/)
- The ML model depends on the Python module to convert raw audio sounds to MFCC features through the use of Python-Android bridge with [Chaquopy](https://chaquo.com/chaquopy/)

## Related work ##
--------
- [HomeSound](https://makeabilitylab.cs.washington.edu/project/smarthomedhh/): An Iterative Field Deployment of an In-Home Sound Awareness System for Deaf or Hard of Hearing Users

