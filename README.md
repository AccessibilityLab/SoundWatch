
Introduction
------------
SoundWatch is an Android-based app that leverages commercially available smartwatches to provide
glanceable, always-available, and private sound feedback in multiple contexts. Building from previous work, SoundWatch informs users about three key sound properties: sound identity, loudness, and time of
occurrence through customizable sound alerts using visual and vibration feedback (Figure 1 and 3). We use
a deep learning-based sound classification engine (running on either the watch or on the paired phone or
cloud) to continually sense and process sound events in real-time. Below, we describe our sound classification
engine, our privacy-preserving sound sensing pipeline, system architectures, and implementation.

Pre-requisites
--------------

- Android SDK 28
- Android Build Tools v28.0.3
- Android Support Repository
- Get the Tensorflow lite model that is open sourced [here]()


Screenshots
-------------

Folder Structure
-------------
- `server`: Python server to serve predictions from raw audio data sent from phone or watch. Refer to [server documentation](server/README.md)
- `SoundWatch/Application`: Android phone application to configure list of displayed sounds on watch and serve audio predictions from watch.Refer to [phone app documentation](SoundWatch/Application/README.md)
- `SoundWatch/Wearable`: Android watch application to listen for surrounding sounds and display the predictions sounds from ML model.Refer to [watch app documentation](SoundWatch/Wearable/README.md)

Getting Started
---------------

- Point buildPython in build.grade to your local python installation (both of application and wearable modules)
- Change ARCHITECTURE in MainActivity for both Phone and Watch to switch between Architectures
- please copy the tflite model into assets folder in Phone Application assets folder, and specify the filename inside MODEL_FILENAME in DataLayerListenerService
- please copy the tflite model into assets folder in Watch Wearable assets folder, and specify the filename inside MODEL_FILENAME in MainActivity
- WARNING: If use WATCH_ONLY_ARCHITECTURE, please copy the tflite model into assets folder in Wearable src folder, and specify the filename inside MODEL_FILENAME in SoundRecorder
- Change AUDIO_TRANMISSION_STYLE in MainActivity for both Phone and Watch to change the Audio Transmission Style (Raw Audio vs. Audio Features)


Tests
-------

- For Model Latency, enable "TEST_MODEL_LATENCY" in MainActivity for both Watch and Phone 
- For E2E Latency, enable "TEST_E2E_LATENCY" in MainActivity for both Watch and Phone

Support
-------

