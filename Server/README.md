
Introduction
------------

Pre-requisites
--------------
- Python 3.6
- Get the Tensorflow lite model that is open sourced [here]()

Screenshots
-------------

Folder Structure
-------------

Getting Started
---------------
`main.py`: CLI to run and test the model with computer microphone (for testing purposes)
*Usage*:
``` python main.py ```
`server.py`: Socket server to serve audio predictions from clients devices such as phone and watch
*Usage*:
``` python server.py ```

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


Tests
-------

- For Model Latency, enable "TEST_MODEL_LATENCY" in MainActivity for both Watch and Phone 
- For E2E Latency, enable "TEST_E2E_LATENCY" in MainActivity for both Watch and Phone

Support
-------
Contact [Hung V Ngo](www.hungvngo.com) @MakeabilityLab through email `hvn297` at cs.washington.edu
Developed with [Dhruv Jain](https://homes.cs.washington.edu/~djain/) and collaborators at [MakeabilityLab](https://makeabilitylab.cs.washington.edu/)

