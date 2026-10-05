#!/usr/bin/env python3
"""
把物品栏里的**帧动画贴图**转成静图：只保留一帧，并删除 .png.mcmeta。

为什么：这些动图在背包/GUI 里会被逐帧 tick + 上传，白耗性能；手册/日常使用并不需要它们动。
19 张里最重的 spawn_box 有 41 帧、wireless_io 29 帧。

挑帧策略：取「不透明像素最多」的一帧（而不是无脑第 0 帧）——
nimble_fabric / wireless_io 的动画是「暗→亮→暗」，第 0 帧几乎是全黑的，直接取会变成隐形图标。

用法：python3 tools/make_item_textures_static.py [--dry-run]
"""
import os, sys, glob, struct
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEM_TEX = os.path.join(ROOT, "src/main/resources/assets/touhou_little_maid/textures/item")

# 用户人工核定的帧号（全部 19 张都显式指定，保证可复现）
OVERRIDE = {
    'camera.png': 0,
    'drown_protect_bauble.png': 3,
    'explosion_protect_bauble.png': 0,
    'fall_protect_bauble.png': 0,
    'favorability_tool_add.png': 3,
    'favorability_tool_full.png': 0,
    'favorability_tool_reduce.png': 4,
    'fire_protect_bauble.png': 0,
    'item_magnet_bauble.png': 0,
    'kappa_compass.png': 0,
    'keyboard.png': 0,
    'magic_protect_bauble.png': 0,
    'nimble_fabric.png': 0,
    'projectile_protect_bauble.png': 0,
    'reborn_maid.png': 8,
    'scarecrow.png': 0,
    'smart_slab_has_maid.png': 0,
    'spawn_box.png': 7,
    'wireless_io.png': 0,
}

ALPHA_MIN = 128


def frames_of(img, size):
    w, _ = size
    return [img.crop((0, i * w, w, (i + 1) * w)) for i in range(img.height // w)]


def ink(frame):
    """不透明像素数 + 平均亮度权重，挑出最"实"的一帧。"""
    px = frame.load()
    n = 0
    for y in range(frame.height):
        for x in range(frame.width):
            if px[x, y][3] >= ALPHA_MIN:
                n += 1
    return n


def main():
    dry = "--dry-run" in sys.argv
    changed = []
    for meta in sorted(glob.glob(os.path.join(ITEM_TEX, "*.png.mcmeta"))):
        with open(meta, encoding="utf-8") as f:
            if '"animation"' not in f.read():
                continue
        png = meta[:-len(".mcmeta")]
        if not os.path.isfile(png):
            print(f"  ⚠ 缺 PNG: {png}")
            continue
        img = Image.open(png).convert("RGBA")
        w, h = img.size
        if h % w != 0:
            print(f"  ⚠ {os.path.basename(png)} 高度不是宽度整数倍（{w}x{h}），跳过")
            continue
        n = h // w
        strip = frames_of(img, (w, h))
        idx = OVERRIDE.get(os.path.basename(png), max(range(n), key=lambda i: ink(strip[i])))
        if dry:
            print(f"  {os.path.basename(png):42s} {n:3d} 帧 -> 取第 {idx} 帧")
            continue
        strip[idx].save(png)
        os.remove(meta)
        changed.append((os.path.basename(png), n, idx))
    if dry:
        return
    print(f"已转静图 {len(changed)} 张：")
    for name, n, idx in changed:
        print(f"  {name:42s} {n:3d} 帧 -> 第 {idx} 帧")


if __name__ == "__main__":
    main()
