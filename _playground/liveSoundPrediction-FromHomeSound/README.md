The deep learning system is written in `python3`, specifically `tensorflow` and `keras`.

To begin, first install python 3.6.4 (Important to install this version only, don't use 3.7). Once python is installed do 
```bash
$ pip install numpy==1.14.1 tensorflow==1.5.0 keras==2.1.6 wget
```

Finally, install `pyaudio` for microphone access:
```bash
$ pip install --global-option='build_ext' --global-option='-I/opt/local/include' --global-option='-L/opt/local/lib' pyaudio
```
Keep in mind that `pyaudio` will require `portaudio` and `libasound` as non-python dependencies. You'll have to install those separately for your OS.

`IMPORTANT:` When you install `pyaudio` via pip, you need to manually specify the `lib` and `include` directories via the `--global-option` flag. The example above assumes `portaudio` is installed under `/opt/local/include` and `/opt/local/lib`.

To run the demo, use: 

```shell
$ python main.py
```
The script will automatically download a model file called into the `/models` directory (if it doesn't exist). It's a ~1GB file, so the download might take a while depending on your Internet connection. The script might show some warnings and ask you to select the microphone for input. Also, look at  `homesounds.py`on what are the different classes and prediction contexts available.
