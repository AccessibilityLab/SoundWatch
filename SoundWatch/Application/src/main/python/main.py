import numpy as np
from vggish_input import waveform_to_examples
from java import jarray, jfloat
np.set_printoptions(threshold=np.inf)

# Variables
RATE = 16000

def audio_samples(x):
    # get float data from string array
    x = x.replace("[", "").replace("]", "")
    x = x.split(",")
    x = np.array(x)
    x = x.astype(np.float)
    x = x / 32768.0 # Convert to [-1.0, +1.0]

    # Calculate MFCC features
    x = waveform_to_examples(x, RATE)

    return jarray(jfloat) (x.reshape(-1))