
import tensorflow as tf
import numpy as np
from vggish_input import wavfile_to_examples
import homesounds

# contexts
context = homesounds.everything
active_context = homesounds.everything      # use this to change context -- see homesounds.py
selected_file = 'example.wav'

# thresholds
PREDICTION_THRES = 0.5 # confidence

model_filename="/storage/emulated/0/DJdev/example_model.hdf5"

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
            print("Prediction: %s (%0.2f)" % (
                homesounds.to_human_labels[active_context[m]], context_prediction[m]))