# Touhou Little Maid（Orihime）· Minecraft 26.3 移植分支

> [!CAUTION]
> **非官方移植 · 实验性 · 上生产线前请先备份存档**
>
> 本仓库是 [Sh1roCu/TouhouLittleMaid-Orihime](https://github.com/Sh1roCu/TouhouLittleMaid-Orihime)（官方 [TartaricAcid/TouhouLittleMaid](https://github.com/TartaricAcid/TouhouLittleMaid) 的非官方 Fabric 移植）的 **26.3 社区移植分支**，与官方团队及原作者**没有任何关系**。
>
> - **请勿把本分支的问题反馈到上游。** 本分支不提供任何担保，可能出现崩溃、渲染异常或**存档损坏**。
> - 本分支的移植改动（版本与依赖升级、第三方兼容剥离、编译期修复）由 **AI 在人类指导下**完成，请视为实验性实现：依赖前请先审计代码，并在你自己的环境中实测。
> - **禁止商业使用**：素材采用 CC BY-NC-SA 4.0（见"许可"一节）。

> [!NOTE]
> 分支约定：分支名 = Minecraft 版本；当前工作分支 **`26.3`**。
> 基线为上游 `26.2`（`9464e48`），并已并入上游 `26.1` 分支的 26 个修复（#33 / #34 / #39 / #40 / #42 / #51 / #52 等）。

## 移植状态

| 项 | 值 |
| --- | --- |
| Minecraft | `26.3` |
| Fabric Loader | `0.19.5`+ |
| Fabric API | `0.161.0+26.3` |
| Gradle Loom | `1.17-SNAPSHOT` |
| 状态 | 🚧 移植中（编译修复阶段，尚未出可用构建） |

## 保留的依赖

| 模组 | 版本 |
| --- | --- |
| Sodium | `mc26.3-0.9.2-fabric` |
| Iris | `1.11.7+26.3-fabric` |
| Mod Menu | `21.0.0` |
| Cloth Config | `26.3.159` |
| Trinkets | `4.2.1+26.3` |
| Forge Config API Port | `26.3.1`（硬依赖） |
| Inventory Profiles Next（+ libipn） | `fabric-26.3-2.3.8` / `fabric-26.3-6.9.0` |
| Patchouli | `26.1-94-beta` ⚠️ 上游未发布 26.2/26.3 版本，本分支仅以 `compileOnly` 占位，运行时由 `isModLoaded` 自动降级（手册入口不显示，不会崩溃） |

## 已剥离的第三方兼容

本分支**不再提供**以下兼容（共 21 个包 / 47 个文件）：

`JEI`、`Jade`、`Aquaculture`、`Kaleidoscope`、`Farmer's Delight`、`Simple Hats`、`Oculus`、`Embeddium`、`Ponder`、`PatPat`、`JMC`、`TACZ / 卓越前线`（gun）、`Travelers' Backpack`、`Immersive Melodies`、`Sophisticated Backpacks`、`SlashBlade`、`Improved Mobs` mixin。

> 其中多数在 26.3 上并无可用版本（Aquaculture、Simple Hats、Improved Mobs 等），剥离后无实际功能损失；Oculus / Embeddium 是 Iris / Sodium 的 NeoForge 侧分支，Fabric 端本就冗余；其余为体积与维护成本考虑。

## 许可

上游为**双许可**，本分支**不改变任何许可条款**，并在仓库根目录保留两份许可原文：

| 部分 | 许可 | 约束 |
| --- | --- | --- |
| 代码 | **MIT**（`LICENSE-MIT`） | 可自由使用 / 修改 / 分发，须保留版权与许可声明。Copyright (c) 2019-2025 tartaric_acid |
| 素材（贴图 / 模型 / 音效 / 文本等） | **CC BY-NC-SA 4.0**（`LICENSE-CC`） | **署名** + **禁止商业性使用** + 修改后须以**相同许可**分发 |

- 本分支的移植改动同样以 **MIT（代码）/ CC BY-NC-SA 4.0（素材）** 条款提供。
- 再分发（打包进整合包、二次 fork 等）必须保留 `LICENSE-MIT` 与 `LICENSE-CC`，并注明原作者 `tartaric_acid`。
- 商业用途（付费整合包、赞助墙后分发、售卖等）**不被素材许可允许**。

## 致谢

- **tartaric_acid** —— 官方 [TouhouLittleMaid](https://github.com/TartaricAcid/TouhouLittleMaid) 作者（代码 MIT / 素材 CC BY-NC-SA 4.0）
- **Sh1roCu** —— [Fabric 移植](https://github.com/Sh1roCu/TouhouLittleMaid-Orihime) 作者，本分支的直接基线
- **26.3 移植所用 AI 编码助手**：DeepSeek、GLM（智谱）、StepFun（阶跃星辰）、Kimi（月之暗面）、GPT（OpenAI）
- 以及 Sodium、Iris、Trinkets、Cloth Config、Forge Config API Port、Mod Menu、Inventory Profiles Next 等依赖的作者

---

## 上游 README（原文，供参考）

## [TouhouLittleMaid](https://github.com/TartaricAcid/TouhouLittleMaid) unofficial Fabric port.
Available on [Modrinth](https://modrinth.com/mod/touhoulittlemaid-orihime) and [CurseForge](https://curseforge.com/minecraft/mc-mods/touhoulittlemaid-orihime).<br>
You can get the detail on TLM's [WIKI](http://page.cfpa.team/TouhouLittleMaid/).<br>

**Note:**
- **This mod requires [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port).**
- **If you want to change some settings in-game, you should install [Cloth Config API](https://modrinth.com/mod/cloth-config).**
- **This mod is still experimental, perhaps there exist some bugs.**
- **If you want to install TACZ-Fabric-1.20.1, you must use [this fork](https://github.com/Sh1roCu/TACZ-Fabric/releases/tag/v1.0.2-hotfix4), or else maids won't send sound.**
- **Compatible with [TACZ-Refabricated](https://github.com/Sh1roCu/TACZ-Refabricated) since version1.20.1-0.1.7.1-(neo)forge1.3.8, and don't support old version of TACZ-Fabric.**

**<br>
If you want to extend this mod, you can add an entry point of type "little_maid_extension" in your fabric.mod.json:**

```
  "entrypoints": {
    "little_maid_extension": [
      "com.example.yourmod.YourMaid"
    ]
  },
```

**and implement** ```ILittleMaid```
