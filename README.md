
# SoundWatch #

![Status](https://img.shields.io/badge/Version-Experimental-brightgreen.svg)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Introduction
------------
SoundWatch is an Android-based app designed for commercially available smartwatches to provide glanceable, always-available, and private sound feedback in multiple contexts. SoundWatch informs users about three key sound properties: sound identity, loudness, and time of occurrence through customizable sound alerts using visual and vibrational feedback. We use a deep learning-based sound classification engine (running on either the watch or on the paired phone or cloud) to continually sense and process sound events in real-time. SoundWatch supports four different architectural configurations: watch-only, watch+phone, watch+phone+cloud, and watch+cloud.

[[Website](https://makeabilitylab.cs.washington.edu/project/soundwatch/)]
[[Paper PDF](https://homes.cs.washington.edu/~djain/img/portfolio/Jain_SoundWatch_ASSETS2020.pdf)]


## Table Of Contents ##

0. [Prerequisite](#prerequisites)
1. [Screenshots](#Screenshots)
2. [Setup](#setup)
3. [Architecture performance test](#Architecture%20performance%20test)
4. [Scenarios](#Scenarios)
5. [Lists of recommended/compatible watches](#Lists%20of%20recommended/compatible%20watches)
6. [Frequently Asked Questions](#Frequently%20Asked%20Questions)
7. [Acknowledgement](#acknowledgement)
8. [Support](#support)

## Prerequisites ##
--------------
- Latest Android Studio (The project is developed with Android Studio 4.0)
- Android SDK 28
- Android Build Tools v28.0.3
- Android Support Repository
- Get the sound classification Tensorflow lite model and the label files that are open sourced [here](https://www.dropbox.com/sh/wngu1kuufwdk8nr/AAC1rm5QR-amL_HBzTOgsZnca?dl=0)

## Screenshots ##
-------------
![SoundWatch system mockup](images/mockup.png?raw=true "Title")
![SoundWatch system mockup 2](images/mockup_2.png?raw=true "Title")



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
            // buildPython "C:\\Users\\hungn\\AppData\\Local\\Programs\\Python\\Python36\\python.exe"
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
- On top toolbar of Android Studio, make sure `Wearable` is chosen and click `Run` button. It is much preferred to use a physical Android Wear device, which is how we developed and tested the project. Otherwise, refer to the Android [documentation](https://developer.android.com/training/wearables/apps/creating) to set up the virtual Android Watch
- Point `buildPython` in `build.gradle` to your local python installation (both of application and wearable modules)
- Change `ARCHITECTURE` in MainActivity for both Phone and Watch to switch between Architectures
- Copy the `tflite` model and `labels.txt` (downloaded above) into `src/assets` folder in Watch `Application` src folder, and specify the filename inside MODEL_FILENAME in MainActivity
- Also copy the same `tflite` model and `labels.txt` into `src/assets` folder in Watch `Wearable` src folder, and specify the filename inside MODEL_FILENAME in MainActivity
- WARNING: If using the `WATCH_ONLY_ARCHITECTURE`, please copy the tflite model into assets folder in Wearable src folder, and specify the filename inside `MODEL_FILENAME` in SoundRecorder
- Change `AUDIO_TRANMISSION_STYLE` in `MainActivity.java` for both Phone and Watch to change the Audio Transmission Style ("Raw Audio" which is faster to process and transmit vs. "Audio Features" which are more private)

## Architecture performance test ##
-------

- For Model Latency, enable "TEST_MODEL_LATENCY" in MainActivity for both Watch and Phone to test the model prediction time.
- For E2E Latency, enable "TEST_E2E_LATENCY" in MainActivity for both Watch and Phone to get the end to end latency from when the sound is first captured on the watch to the time when the notification is displayed on the watch.

After enabling the boolean flags, just run the watch and phone app like usual. The test results will be output as `*.txt` (i.e: `watch_model.txt`, `e2e_latency.txt`) to local device directory (phone or watch) inside the `com.wearable.sound` folder.

## Scenarios ##
-------

![SoundWatch system mockup 3](images/SoundWatch_mockups.png?raw=true "Title")

## Lists of recommended/compatible watches ##
-------

Under $200
- Fossil Sport
- Ticwatch E2
- Ticwatch S2
- Skagen Falster 3

Under $300
- Ticwatch Pro 2/3 (this is the watch we currently used for testing)
- Fossil Gen 5
- Moto 360
- Oppo Watch (this one is fairly new)

Above $300
- Suunto 7

## Frequently Asked Questions ##
-------
My phone and watch have become unpaired. How do I fix this?
- Frequently, phones and watches become unpaired due to a bug for Google WearOS. Simply reopen the WearOS app on your phone, wait until the app said "Connected via Bluetooth|Wifi" then return to your watch and re-click the listen microphone icon. A pop-up should come up that tells you that your device is reconnected.


How do I set SoundWatch to listen for sounds?
- Open the SoundWatch app in your watch and click on the microphone icon. That will begin listening capabilities.

My watch is not responding to touch or buttons.  How do I get it to respond?
- Your watch might be low in battery. Recharge and try again.

How do I select and deselect sounds for the watch to listen for?
- On the phone app, open SoundWatch.  You then will have a list of sounds. Simply uncheck the sounds you do not want the watch to listen for. Leave checked or recheck the sounds you want to listen for.

How do I snooze a sound on my watch?
- In the pop-up notification, there will be a button labeled “snooze” that you can press to snooze for a specified amount of time.  If you would like more options for the timing of the snooze, simply press “open” and it will display more options for you to press.

How do I see notifications if they aren’t popping up?
- If your watch is vibrating but no messages are popping up, you can swipe up on your watchface and the current and past notifications will display. 
## Support ##
-------
- Developed with [Dhruv Jain](https://homes.cs.washington.edu/~djain/) and collaborators at [MakeabilityLab](https://makeabilitylab.cs.washington.edu/)
- Contact [Khoa Nguyen](https://www.linkedin.com/in/akka/) @MakeabilityLab through email `akhoa99` at cs.washington.edu
- Contact [Hung V Ngo](www.hungvngo.com) @MakeabilityLab through email `hvn297` at cs.washington.edu



Drop us a note if you are using or plan to use SoundWatch for research purposes. We are also happy to help with any questions or issues.

## Acknowledgement ##
-------
- The Android watch was built based on the [Data layer sample](https://github.com/android/wear-os-samples/tree/master/DataLayer) from Google Android repo to establish connection between phone and watch.
- Python server is created with [Socket.io](https://socket.io/blog/native-socket-io-and-android/)
- The ML model depends on the Python module to convert raw audio sounds to MFCC features through a Python-Android bridge with [Chaquopy](https://chaquo.com/chaquopy/)

## Related work ##
--------
- [HomeSound](https://makeabilitylab.cs.washington.edu/project/smarthomedhh/): An Iterative Field Deployment of an In-Home Sound Awareness System for Deaf or Hard of Hearing Users
