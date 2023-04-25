# Selfie-to-Anime Application
## _University Project - Advanced Programming of Mobile Systems - UniPr_

# Purpose

The purpose of this project was to develop an android application that was able to get selfie from the camera or from internal memory of the phone and then transform those seflies in anime using both an AI model optimized for android and OpenCV library. 

An AI model was first trained on a computer using the GPU and then the inference was performed on the mobile application.

# Development

## AI model

I trained a Cycle Gan PyTorch model on the Kaggle selfie2anime dataset 

- [CycleGan](https://github.com/junyanz/pytorch-CycleGAN-and-pix2pix) 
- [selfie2anime](https://www.kaggle.com/datasets/arnaud58/selfie2anime) 

to start the train of the model download the project and the dataset and then move inside the folder of the project and type:

_python train.py --dataroot PATH-TO-THE-DATASET --name selfie2anime --model cycle_gan_

Once the train is finished, get the last saved model from the checkpoints and oprtimize it to be used on android. Since Gan models are usually composed of both a generator part and a discriminator part but the purpose of this model was to create and generate a new image, I just needed the generator part. To run the optimization script type:

_python trace_model.py --name selfie2anime --model cycle_gan --dataroot PATH-TO-THE-DATASET_

# Android Application

## Dependencies

Since the application uses both PyTorch and OpenCV, those need to be added to dependences in the build gradle script

```
implementation project(':openCVLibrary343')
implementation 'org.pytorch:pytorch_android_lite:1.9.0'
implementation 'org.pytorch:pytorch_android_torchvision:1.9.0'
```

