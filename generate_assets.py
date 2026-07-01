import numpy as np
from PIL import Image, ImageDraw, ImageFilter
import os

drawable_path = "app/src/main/res/drawable"

# 1. Realistic Cloud Texture (Perlin noise-based radial mask)
def make_cloud():
    width, height = 512, 256
    # Create white image
    cloud = Image.new('RGBA', (width, height), (255, 255, 255, 0))
    draw = ImageDraw.Draw(cloud)
    
    # Draw several overlapping ellipses with blur to create a cloud shape
    for _ in range(10):
        w = np.random.randint(100, 250)
        h = np.random.randint(100, 200)
        x = np.random.randint(50, width - w - 50)
        y = np.random.randint(30, height - h - 30)
        alpha = np.random.randint(100, 200)
        draw.ellipse([x, y, x+w, y+h], fill=(255, 255, 255, alpha))
        
    cloud = cloud.filter(ImageFilter.GaussianBlur(15))
    cloud.save(os.path.join(drawable_path, "realistic_cloud.png"))

# 2. Realistic Rain Streak
def make_rain():
    width, height = 20, 200
    rain = Image.new('RGBA', (width, height), (255, 255, 255, 0))
    draw = ImageDraw.Draw(rain)
    
    # Draw a gradient streak
    for y in range(height):
        # alpha falls off at the top and bottom
        if y < height / 2:
            alpha = int((y / (height / 2)) * 100)
        else:
            alpha = int(((height - y) / (height / 2)) * 100)
            
        # Draw a line with a tiny bit of horizontal fade
        for x in range(width):
            dist_from_center = abs(x - width/2)
            if dist_from_center < 2:
                final_alpha = int(alpha * (1 - (dist_from_center / 2)))
                draw.point((x, y), fill=(255, 255, 255, final_alpha))
                
    rain = rain.filter(ImageFilter.GaussianBlur(1))
    rain.save(os.path.join(drawable_path, "realistic_rain.png"))

# 3. Realistic Snow Flake / Bokeh
def make_snow():
    width = 64
    snow = Image.new('RGBA', (width, width), (255, 255, 255, 0))
    draw = ImageDraw.Draw(snow)
    
    center = width // 2
    for r in range(center):
        # Soft glowing falloff
        alpha = int(255 * (1 - (r / center)**2))
        draw.ellipse([center - r, center - r, center + r, center + r], outline=(255, 255, 255, alpha))
        
    snow = snow.filter(ImageFilter.GaussianBlur(2))
    snow.save(os.path.join(drawable_path, "realistic_snow.png"))

make_cloud()
make_rain()
make_snow()
