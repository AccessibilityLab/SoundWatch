import os
import numpy as np
import librosa
from java import jarray, jfloat
import wave
SAMPLING_RATE = 16000
FILE_SIZE = 1
STORAGE = os.environ["HOME"]

def get_melspectrogram_db(file_path, sr=SAMPLING_RATE, n_fft=512, hop_length=251, n_mels=32, fmin=20, fmax=8000, top_db=80):
    wav,sr = librosa.load(file_path,sr=sr)
    if wav.shape[0]<FILE_SIZE*sr:
        wav=np.pad(wav,int(np.ceil((FILE_SIZE*sr-wav.shape[0])/2)),mode='reflect')
    else:
        wav=wav[:FILE_SIZE*sr]
    spec=librosa.feature.melspectrogram(wav, sr=sr, n_fft=n_fft,
                                        hop_length=hop_length,n_mels=n_mels,fmin=fmin,fmax=fmax)
    spec_db=librosa.power_to_db(spec,top_db=top_db)
    return spec_db


def spec_to_image(spec, eps=1e-6):
    mean = spec.mean()
    std = spec.std()
    spec_norm = (spec - mean) / (std + eps)
    spec_min, spec_max = spec_norm.min(), spec_norm.max()
    spec_scaled = 255 * (spec_norm - spec_min) / (spec_max - spec_min)
    spec_scaled = spec_scaled.astype(np.uint8)
    return spec_scaled

def extract_features(x):
    x = x.replace("[", "").replace("]", "")
    x = x.split(",")
    x = np.array(x, dtype=np.short)
    print("x shape", x)
    with wave.open(STORAGE + "/querySound.wav", "wb") as out_f:
        out_f.setnchannels(1)
        out_f.setsampwidth(2) # number of bytes
        out_f.setframerate(SAMPLING_RATE)
        out_f.writeframesraw(x)
    features = spec_to_image(get_melspectrogram_db(STORAGE + "/querySound.wav"))
    os.remove(STORAGE + "/querySound.wav")
    features = np.reshape(features, (2048))
    return jarray(jfloat)(features)