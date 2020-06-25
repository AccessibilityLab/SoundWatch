
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

Folder Structure
-------------


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

