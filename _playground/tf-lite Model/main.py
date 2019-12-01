
import tensorflow as tf
import numpy as np
from vggish_input import waveform_to_examples
import homesounds
import pyaudio
from pathlib import Path
import time
import argparse
import wget
from helpers import dbFS

# contexts
context = homesounds.everything
active_context = homesounds.everything      # use this to change context -- see homesounds.py

# thresholds
PREDICTION_THRES = 0.5 # confidence
DBLEVEL_THRES = -40 # dB

# Variables
FORMAT = pyaudio.paInt16
CHANNELS = 1
RATE = 16000
CHUNK = RATE
MICROPHONES_DESCRIPTION = []
FPS = 60.0

MODEL_PATH = "models/example_model.hdf5"

##############################
# Load Deep Learning Model
##############################
print("Using deep learning model: %s" % (model_filename))
model = load_model(model_filename)
graph = tf.get_default_graph()

##############################
# Setup Audio Callback
##############################
def audio_samples(in_data, frame_count, time_info, status_flags):
    global graph
    np_wav = np.fromstring(in_data, dtype=np.int16) / 32768.0 # Convert to [-1.0, +1.0]
    print(np_wav)
    # Compute RMS and convert to dB
    rms = np.sqrt(np.mean(np_wav**2))
    db = dbFS(rms)

    # Make predictions
    x = waveform_to_examples(np_wav, RATE)
    predictions = []
    with graph.as_default():
        if x.shape[0] != 0:
            x = x.reshape(len(x), 96, 64, 1)
            pred = model.predict(x)
            predictions.append(pred)

        for prediction in predictions:
            context_prediction = np.take(prediction[0], [homesounds.labels[x] for x in active_context])
            m = np.argmax(context_prediction)
            if (context_prediction[m] > PREDICTION_THRES and db > DBLEVEL_THRES):
                print("Prediction: %s (%0.2f)" % (homesounds.to_human_labels[active_context[m]], context_prediction[m]))
            
    return (in_data, pyaudio.paContinue)

##############################
# Launch Application
##############################
while(1):
    ##############################
    # Setup Audio
    ##############################
    p = pyaudio.PyAudio()
    stream = p.open(format=FORMAT, channels=CHANNELS, rate=RATE, input=True, frames_per_buffer=CHUNK, stream_callback=audio_samples, input_device_index=MICROPHONE_INDEX)

    ##############################
    # Start Non-Blocking Stream
    ##############################
    print("# Live Prediction Using Microphone: %s" % (mic_desc))
    stream.start_stream()
    while stream.is_active():
        time.sleep(0.1)
