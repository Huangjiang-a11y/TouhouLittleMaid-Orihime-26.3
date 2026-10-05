#!/usr/bin/env python3
"""
把书插图裁掉四周的透明边距。

18 张书插图都是 256x256，但实际内容只占左上约 200x200，剩下全是透明 ->
书壳按真实尺寸等比缩放时会被这些空白稀释（图显得又小又偏）。
上游 Patchouli 原样画 256x256，所以这不算 bug，但裁紧后清晰得多，jar 也小一圈。
"""
import glob, os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DIR = os.path.join(ROOT, "src/main/resources/assets/touhou_little_maid/textures/book")

total_before = total_after = 0
for path in sorted(glob.glob(os.path.join(DIR, "*.png"))):
    im = Image.open(path).convert("RGBA")
    box = im.getchannel("A").getbbox()
    if box is None or box == (0, 0) + im.size:
        print(f"  {os.path.basename(path):24s} {im.size} 已是紧的，跳过")
        continue
    before = os.path.getsize(path)
    cropped = im.crop(box)
    cropped.save(path, optimize=True, compress_level=9)
    after = os.path.getsize(path)
    total_before += before
    total_after += after
    print(f"  {os.path.basename(path):24s} {str(im.size):10s} -> {str(cropped.size):10s}")
print(f"合计 {total_before/1024:.0f}KB -> {total_after/1024:.0f}KB")
