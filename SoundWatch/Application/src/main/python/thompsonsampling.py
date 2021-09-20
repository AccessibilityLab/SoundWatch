import numpy as np 
import numpy.random as rand
import numpy.linalg as la
import pickle
import tflite_runtime.interpreter as tflite
from os.path import dirname, join
import os

STORAGE = os.environ["HOME"]
TS_INI_PATH = join(dirname(__file__), "hyperparameters_ini.pkl")
TS_UPDATE_PATH = STORAGE + "/hyperparameters_update.pkl"
NUM_CLASSES = 10

# Load TFLite model and allocate tensors.
interpreter = tflite.Interpreter(model_path=join(dirname(__file__), "sw_model_v2.tflite"))
interpreter.allocate_tensors()

dict = {}

def load_hyperparameters():
    if (os.path.exists(TS_UPDATE_PATH)):
        with open(TS_UPDATE_PATH,'rb') as f:
            hyperparameters = pickle.load(f)

    else:
        with open(TS_INI_PATH,'rb') as f:
            hyperparameters = pickle.load(f)
    V, S, Vhalf, Vinv= hyperparameters
    return V, S, Vhalf, Vinv


def get_prediction(x):
    V, S, Vhalf, Vinv = load_hyperparameters()
    x = np.array(x, dtype=np.float32)
    interpreter.set_tensor(2, x)
    interpreter.invoke()
    output_details = interpreter.get_tensor(34)[0][0]

    prediction_index = ts_predict(output_details, NUM_CLASSES, S, Vhalf, Vinv)

    return prediction_index


def ts_predict(features, num_classes, S, Vhalf, Vinv):
    '''
    Return a prediction without update the parameters
    Inputs: features - 1 x d matrix of feature vectors (d is dimension of
                        feature vector, 1 number of samples)
            num_classes - integer, number of possible class labels
            V, S, Vhalf, Vinv, U2, S2, V2 - variable initialized from training dataset

    Outputs: regret - n x 1 vector of regret incurred by algorithm
             accuracy - float value giving accuracy of algorithm

    '''
    global dict
    n,d = features.shape
    x = features[0]
    max_arm = -1
    reward = -100
    a = rand.randn(d)
    for i in range(num_classes):
        # compute mean of posterior for class i, this is the \phi_a vector
        post_mean = Vinv[i].dot(S[i])
        # draw sample from posterior for class i, this is the \tilde{\phi}_a vector
        theta_hat = post_mean + Vhalf[i].dot(a)
        # determine if class i the best observed so far
        if theta_hat.dot(x) > reward:
            reward = theta_hat.dot(x)
            # this is the prediction that we'll display to the user
            max_arm = i

    dict[max_arm] = features
    print("dict keys ", list(dict.keys()))
    return max_arm

def feedback_sound(prediction_index, feedback):
    global dict
    V, S, Vhalf, Vinv = load_hyperparameters()
    print("User gave feedback", feedback, "for sound" , prediction_index)
    if feedback != "none" and prediction_index in dict:
        with open(TS_UPDATE_PATH,'wb') as f:
            pickle.dump(ts_update(dict[prediction_index], feedback, V, S, Vhalf, Vinv, prediction_index), f)
        dict.pop(prediction_index)

def ts_update(features, feedback, V, S, Vhalf, Vinv, prediction_index):
    '''
    Update the RL hyperparameters without returning a prediction
    Inputs: features - n x d matrix of feature vectors (d is dimension of
                        feature vector, n number of samples)
            feedback - n x 1 vector of user feedback, 1 for correct, 0 for incorrect
            predicted_max_arm - integer, result from ts_predict
            num_classes - integer, number of possible class labels
            V, S, Vhalf, Vinv, U2, S2, V2 - variable initialized from training dataset

    Outputs: the hyperparameters

    '''
    if feedback == "true":
        reward = 10
    else:
        reward = -50
    print("Entered ts_update with feedback", reward)
    x = features[0,:]

    # update statistics
    V[prediction_index] += np.outer(x,x)
    Vinv[prediction_index] = la.inv(V[prediction_index])
    U2,S2,V2 = la.svd(Vinv[prediction_index])
    Vhalf[prediction_index] = U2.dot(np.diag(np.sqrt(S2))).dot(U2.T)
    S[prediction_index] += x*reward
    return V, S, Vhalf, Vinv

