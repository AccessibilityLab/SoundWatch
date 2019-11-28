from android.os import Bundle
from android.support.v7.app import AppCompatActivity
from com.chaquo.python.hello import R
from java import jvoid, Override, static_proxy
#from os.path import dirname, join
from pkgutil import get_data
import tensorflow as tf
from tensorflow.python.keras.models import load_model

#model_path = get_data(__name__, "liveSoundPrediction/main.py")
#print("Here: " + model_path)
#model = load_model("/data/user/0/com.chaquo.python.hello/files/chaquopy/AssetFinder/app/modelsDir/example_model.hdf5")


model = load_model("/storage/emulated/0/DJdev/example_model.hdf5")
#from com.chaquo.python import Python
#context = Python.getPlatform().getApplication().getExternalFilesDir()
#print(context)
#model = load_model("/data/user/0/com.chaquo.python.hello/files/chaquopy/AssetFinder/app/modelsDir/example_model.hdf5")
#model = load_model(model_path)
#readme = pkgutil.get_data(__name__, "liveSoundPrediction/models/example_model.hdf5")
#model = load_model(io.StringIO(readme.decode()))
#model_path = open((join(dirname(modelsDir.__file__), "example_model.hdf5")
#model = load_model(model_path)
# /data/user/0/com.chaquo.python.hello/modelsDir/
#print("Here", modelsDir.__file__)

class MainActivity(static_proxy(AppCompatActivity)):

    @Override(jvoid, [Bundle])
    def onCreate(self, state):
        AppCompatActivity.onCreate(self, state)
        self.setContentView(R.layout.activity_main)
