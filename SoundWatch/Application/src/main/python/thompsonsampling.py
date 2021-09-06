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





def ts_predict(features, num_classes, V, S, Vhalf, Vinv):
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
        theta_hat = post_mean + Vhalf[i].dot(rand.randn(d))
        print("reward at i:",i,":", theta_hat.dot(x))
        # determine if class i the best observed so far
        if theta_hat.dot(x) > reward:
            reward = theta_hat.dot(x)
            # this is the prediction that we'll display to the user
            max_arm = i

    dict[max_arm] = features
    print("dict keys ", list(dict.keys()))
    return max_arm

def feedback_sound(predicted_max_arm, feedback):
    # global V
    # global S
    # global Vhalf
    # global Vinv
    global dict
    V, S, Vhalf, Vinv = load_hyperparameters()
    print("User gave feedback", feedback, "for sound" , predicted_max_arm)
    if feedback != "none" and predicted_max_arm in dict:
        with open(TS_UPDATE_PATH,'wb') as f:
            pickle.dump(ts_update(dict[predicted_max_arm], feedback, NUM_CLASSES, V, S, Vhalf, Vinv, predicted_max_arm), f)
        dict.pop(predicted_max_arm)

def ts_update(features, feedback, V, S, Vhalf, Vinv, predicted_max_arm):
    '''
    Update the RL hyperparameters without returning a prediction 
    Inputs: features - n x d matrix of feature vectors (d is dimension of 
                        feature vector, n number of samples)
            feedback - n x 1 vector of user feedback, 1 for correct, 0 for incorrect
            predicted_max_arm - integer, result from ts_predict
            num_classes - integer, number of possible class labels
            V, S, Vhalf, Vinv, U2, S2, V2 - variable initialized from training dataset
    
    Outputs: regret - n x 1 vector of regret incurred by algorithm
             accuracy - float value giving accuracy of algorithm

    '''
    if feedback == "true":
        reward = 5
    else:
        reward = -10
    print("Entered ts_update with feedback", reward)
    x = features[0,:]

    # update statistics
    V[predicted_max_arm] += np.outer(x,x)
    Vinv[predicted_max_arm] = la.inv(V[predicted_max_arm])
    U2,S2,V2 = la.svd(Vinv[predicted_max_arm])
    Vhalf[predicted_max_arm] = U2.dot(np.diag(np.sqrt(S2))).dot(U2.T)
    S[predicted_max_arm] += x*reward
    return V, S, Vhalf, Vinv


def load_input(x):
    V, S, Vhalf, Vinv = load_hyperparameters()
    x = np.array(x, dtype=np.float32)
    print("x", x)
    interpreter.set_tensor(2, x)

    interpreter.invoke()
    output_details = interpreter.get_tensor(34)[0][0]
    print("output_detailS", output_details)
    predicted_max_arm = ts_predict(output_details, NUM_CLASSES, V, S, Vhalf, Vinv)

    return predicted_max_arm


def load_hyperparameters():
    if (os.path.exists(TS_UPDATE_PATH)):
        print("Loaded TS_UPDATE_PATH")
        with open(TS_UPDATE_PATH,'rb') as f:
            hyperparameters = pickle.load(f)

    else:
        print("Loaded TS_INI_PATH")
        with open(TS_INI_PATH,'rb') as f:
            hyperparameters = pickle.load(f)
    V, S, Vhalf, Vinv= hyperparameters
    return V, S, Vhalf, Vinv





