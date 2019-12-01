
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

# model 
model_filename = "models/example_model.tflite"


# Load TFLite model and allocate tensors.
interpreter = tf.lite.Interpreter(model_path=model_filename)
interpreter.allocate_tensors()

# Get input and output tensors.
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

# Test model on random input data.
input_shape = input_details[0]['shape']
print(input_shape)
input_data = wavfile_to_examples(selected_file)
input_data = input_data.reshape(len(input_data), 96, 64, 1)
print(np.array([input_data[0]], dtype=np.float32).shape)
#input_data = np.array(np.random.random_sample([ 19, 96, 64, 1]), dtype=np.float32)
interpreter.set_tensor(input_details[0]['index'], np.array([input_data[17]], dtype=np.float32))

interpreter.invoke()

# The function `get_tensor()` returns a copy of the tensor data.
# Use `tensor()` in order to get a pointer to the tensor.
output_data = interpreter.get_tensor(output_details[0]['index'])
print(homesounds.to_human_labels[active_context[np.argmax(output_data)]])


###########################
# Read Wavfile and Make Predictions
###########################

'''    
    predictions = model.predict(x)

    for k in range(len(predictions)):
        context_prediction = np.take(predictions[k], [homesounds.labels[x] for x in active_context])
        m = np.argmax(context_prediction)
        if (context_prediction[m] > PREDICTION_THRES):
            print("Prediction: %s (%0.2f)" % (homesounds.to_human_labels[active_context[m]], context_prediction[m]))
'''