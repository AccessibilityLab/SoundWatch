
Introduction
------------
SoundWatch is an Android-based app that leverages commercially available smartwatches to provide
glanceable, always-available, and private sound feedback in multiple contexts. Building from previous work, SoundWatch informs users about three key sound properties: sound identity, loudness, and time of
occurrence through customizable sound alerts using visual and vibration feedback (Figure 1 and 3). We use
a deep learning-based sound classification engine (running on either the watch or on the paired phone or
cloud) to continually sense and process sound events in real-time. Below, we describe our sound classification
engine, our privacy-preserving sound sensing pipeline, system architectures, and implementation.

![SoundWatch system mockup](images/image.png?raw=true "Title")
![SoundWatch system mockup 1](images/image_1.png?raw=true "Title")


Pre-requisites
--------------
- Latest Android Studio 
- Android SDK 28
- Android Build Tools v28.0.3
- Android Support Repository
- Get the sound classification Tensorflow lite model that is open sourced [here]()


Screenshots
-------------

Folder Structure
-------------
- `server`: Python server to serve predictions from raw audio data sent from phone or watch. Refer to [server documentation](server/README.md)
- `SoundWatch/Application`: Android phone application to configure list of displayed sounds on watch and serve audio predictions from watch.Refer to [phone app documentation](SoundWatch/Application/README.md)
- `SoundWatch/Wearable`: Android watch application to listen for surrounding sounds and display the predictions sounds from ML model.Refer to [watch app documentation](SoundWatch/Wearable/README.md)

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

Configuration
-------------

- Let gradle configure and install dependencies for both `Application` and `Wearable` projects. 
- On top toolbar of Android Studio, make sure `Wearable` is chosen and click `Run` button. It is much preferred to use a physical Android Wear device since it is how we develop and tests the project. Otherwise, refer to the Android [documentation](https://developer.android.com/training/wearables/apps/creating) to set up the virtual Android Watch

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

