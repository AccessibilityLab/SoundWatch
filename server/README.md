
Introduction
------------
This repo contains the Python socket server to process the audio recognition process. Clients (phones and watches) sends raw audio or audio features to servers in an event-based architecture and server will send the audio label back to clients.

This server can serve requests with either raw audio or audio features:
- Using raw audio requests can boost the performance since the server is much more performant in preprocessing the raw audio compared to performing it in edge devices. 
- Using audio features can protect user's privacy by not exposing raw audio to middle men attackers.

Pre-requisites
--------------
- Python 3.6
- Get the sound classification Tensorflow lite model and the label files that are open sourced [here](https://www.dropbox.com/sh/wngu1kuufwdk8nr/AAC1rm5QR-amL_HBzTOgsZnca?dl=0)

Getting Started
---------------
- `main.py`: CLI to run and test the model with computer microphone (for testing purposes)

*Usage*:
``` python main.py ```

- `server.py`: Socket server to serve audio predictions from clients devices such as phone and watch

- `e2eServer.py`: server application that measures the end to end runtime of the sound prediction 

*Warning*: This requires the watch and the phone to set `TEST_E2E_LATENCY` to be `true` in the corresponding files.

*Usage*:
``` python server.py ```

- `melFeatures.py`, `easing.py`, `helpers.py`: preprocessing files that convert the raw audio to MFCC features to feed to the ML model.

Socket Events
---------------
Example Usage of SocketIO Client:
```python
    socket.emit('audio_data',
                {
                    "data": [1.0,2.0, 3.0]
                })
```
- `audio_feature_data`: Serve predictions from audio MFCC features
*Request*:
```json
{
    "data": [1.0,2.0,3.0],
    "db": "1.0"
}
```
*Response*:
```json
{
    "label": "Knocking",
    "accuracy": "0.96"
}
```
- `audio_data`: Serve predictions from raw audio
```json
{
    "data": [1.0,2.0,3.0]
}
```
*Response*:
```json
{
    "label": "Knocking",
    "accuracy": "0.96"
}
```
