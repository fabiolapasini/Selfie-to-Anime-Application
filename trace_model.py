import numpy as np
import torch
import torchvision
from torch.utils.mobile_optimizer import optimize_for_mobile
from torchvision.io import image
from torchvision import transforms
import matplotlib.pyplot as plt
import models.networks
from options.train_options import TrainOptions
from models import create_model
from PIL import Image

# print infos
'''print(torch.cuda.is_available()) # true
print(torch.cuda.current_device()) # 0
print(torch.cuda.device(0)) # <torch.cuda.device object at 0x0000026CC26B9190>
print(torch.cuda.get_device_name(0)) #NVIDIA GeForce GTX 1650
'''

# get the model
opt = TrainOptions().parse()
model = create_model(opt)      # create a model given opt.model and other options
model.setup(opt)               # regular setup: load and print networks; create schedulers

# load the network
model.load_networks(200)

# per far funzionare il progetto sotto
model_G_A = model.netG_A.module

# save the model
MODEL_PATH = "phone_models/"
MODEL_NAME = "phone_net_G_A.pth"
MODEL_NAME2 = "phone_net_G_A.ptl"
#torch.save(model_G_A.state_dict(), MODEL_PATH+MODEL_NAME)  # non va

model_G_A.eval()
scripted_module = torch.jit.script(model_G_A)
# Export mobile interpreter version model (compatible with mobile interpreter)
optimized_scripted_module = optimize_for_mobile(scripted_module)
optimized_scripted_module._save_for_lite_interpreter(MODEL_PATH+MODEL_NAME2)

model_G_A = model.netG_A.cuda()

# func to denorm the tensor
'''def denorm(x):
    """Convert the range from [-1, 1] to [0, 1]."""
    out = (x + 1) / 2
    return out.clamp_(0, 1)

# func to load just one image
def image_loader(image_name, t):
    """load image, returns cuda tensor"""
    image = Image.open(image_name)
    image = t(image).float()
    image = image.unsqueeze(0)
    return image.cuda()  #assumes that you're using GPU
    #return image        # if not using: model.netG_A.cuda()


# define the transform to get the tensor with the right size and everything
transforms = transforms.Compose([
    transforms.Resize(256),
    transforms.ToTensor(),
    transforms.Normalize((0.5, 0.5, 0.5), (0.5, 0.5, 0.5))
])

#paths
PATH = "images/input/"
NAME = "marghe.jpg"
PATH_ANIME = "images/output/"
NAME_ANIME = "marghe_anime_200.png"


# load image
image = image_loader(PATH + NAME, transforms)

# inference
output = model_G_A(image)
output = denorm(output)

# save the output
torchvision.utils.save_image(output, PATH_ANIME + NAME_ANIME)'''