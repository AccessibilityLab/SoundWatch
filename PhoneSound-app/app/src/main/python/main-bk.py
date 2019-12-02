
from com.phone.python.sound import R
import tensorflow as tf
from tensorflow.python.keras.models import load_model
import numpy as np
from vggish_input import waveform_to_examples
from helpers import dbFS
import homesounds

# Variables
RATE = 16000
PREDICTION_THRES = 0.5 # confidence
DBLEVEL_THRES = -35 # dB

# contexts
context = homesounds.everything
active_context = homesounds.everything      # use this to change context -- see homesounds.py
model_filename="/storage/emulated/0/DJdev/example_model.hdf5"

# Load Deep Learning Model
print("Using deep learning model: %s" % (model_filename))
model = load_model(model_filename)
graph = tf.get_default_graph()

def audio_samples(in_data):

    # get float data from string array
    in_data = in_data.replace("[", "").replace("]", "")
    in_data = in_data.split(",")
    in_data = np.array(in_data)
    in_data = in_data.astype(np.float)
    np_wav = in_data / 32768.0 #Convert to [-1.0, +1.0]
    #np_wav = np.fromstring(in_data, dtype=np.int16, sep=',') / 32768.0 # Convert to [-1.0, +1.0]

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
                #print ("Prediction: %s (%0.2f)" % (homesounds.to_human_labels[active_context[m]], context_prediction[m]))
                return("Prediction: %s (%0.2f)" % (homesounds.to_human_labels[active_context[m]], context_prediction[m]))