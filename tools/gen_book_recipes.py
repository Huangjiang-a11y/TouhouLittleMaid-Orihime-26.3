#!/usr/bin/env python3
"""
把手册 crafting 页引用的合成配方"烤"成一份静态资源，供客户端 CraftingPageRenderer 直接渲染。

为什么不走网络同步：26.3 客户端不再持有完整配方表，而 Fabric 的配方同步是按序列化器
逐项 opt-in 的——照搬会把**所有模组**的合成配方都推给客户端（登录时一次性传，modpack 下 MB 级）。
手册只是文档、配方又是本模组自己的，烤进资源既零开销又不受服务端影响。

为什么不烤成图片：TLM 不少物品（如 chair）的 GUI 图标是特殊 3D 模型，没有平面贴图，
烤图会画错；烤成数据让客户端照旧实时渲染就没这问题。

用法：python3 tools/gen_book_recipes.py
输出：src/main/resources/assets/touhou_little_maid/book_recipes.json
"""
import json, os, glob

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NS = "touhou_little_maid"
RECIPE_DIR = os.path.join(ROOT, "src/main/generated/data", NS, "recipe")
OUT = os.path.join(ROOT, "src/main/resources/assets", NS, "book_recipes.json")

# 标签 -> 代表物品（与客户端 resolveForStacks 取 tag 第一项的效果一致）
TAG_PICK = {
    "#minecraft:wool": "minecraft:white_wool",
    "#minecraft:planks": "minecraft:oak_planks",
    "#minecraft:planks": "minecraft:oak_planks",
    "#c:ingots/iron": "minecraft:iron_ingot",
    "#c:dusts/redstone": "minecraft:redstone",
    "#c:gems/diamond": "minecraft:diamond",
    "#c:rods/wooden": "minecraft:stick",
}


def main():
    out = {}
    for path in sorted(glob.glob(os.path.join(RECIPE_DIR, "*.json"))):
        rec = json.load(open(path, encoding="utf-8"))
        if rec.get("type") != "minecraft:crafting_shaped":
            continue
        key = rec["key"]
        pattern = rec["pattern"]
        h = len(pattern)
        w = max(len(r) for r in pattern)
        grid = []
        for r in range(h):
            row = pattern[r].ljust(w)
            for c in range(w):
                ch = row[c]
                grid.append("" if ch == " " else TAG_PICK.get(key[ch], key[ch]))
        out[f"{NS}:{os.path.splitext(os.path.basename(path))[0]}"] = {
            "width": w,
            "height": h,
            "grid": grid,
            "result": rec["result"]["id"],
        }

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, indent=2, sort_keys=True)
        f.write("\n")
    print(f"写出 {os.path.relpath(OUT, ROOT)}（{len(out)} 个配方）")
    for k, v in out.items():
        print(f"  {k}: {v['width']}x{v['height']} -> {v['result']}")


if __name__ == "__main__":
    main()
