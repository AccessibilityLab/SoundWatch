
from keras.models import load_model
import tensorflow as tf
import numpy as np
from vggish_input import wavfile_to_examples
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
selected_file = 'example.wav'

# thresholds
PREDICTION_THRES = 0.5 # confidence

###########################
# Download model, if it doesn't exist
###########################
MODEL_URL = "https://www.dropbox.com/s/cq1d7uqg0l28211/example_model.hdf5?dl=1"
MODEL_PATH = "models/example_model.hdf5"
print("=====")
print("2 / 2: Checking model... ")
print("=====")
model_filename = "models/example_model.hdf5"
homesounds_model = Path(model_filename)
if (not homesounds_model.is_file()):
    print("Downloading example_model.hdf5 [867MB]: ")
    wget.download(MODEL_URL,MODEL_PATH)

##############################
# Load Deep Learning Model
##############################
print("Using deep learning model: %s" % (model_filename))
model = load_model(model_filename)
graph = tf.get_default_graph()


###########################
# Read Wavfile and Make Predictions
###########################
x = wavfile_to_examples(selected_file)
with graph.as_default():
    
    x = x.reshape(len(x), 96, 64, 1)
    predictions = model.predict(x)

    for k in range(len(predictions)):
        context_prediction = np.take(predictions[k], [homesounds.labels[x] for x in active_context])
        m = np.argmax(context_prediction)
        if (context_prediction[m] > PREDICTION_THRES):
            print("Prediction: %s (%0.2f)" % (homesounds.to_human_labels[active_context[m]], context_prediction[m]))
