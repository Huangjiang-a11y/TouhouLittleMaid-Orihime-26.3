# Touhou Little Maid（Orihime）· Minecraft 26.3 移植分支

> [!CAUTION]
> **非官方移植 · 实验性 · 上生产线前请先备份存档**
>
> 本仓库是 [Sh1roCu/TouhouLittleMaid-Orihime](https://github.com/Sh1roCu/TouhouLittleMaid-Orihime)（官方 [TartaricAcid/TouhouLittleMaid](https://github.com/TartaricAcid/TouhouLittleMaid) 的非官方 Fabric 移植）的 **26.3 社区移植分支**，与官方团队及原作者**没有任何关系**。
>
> - **请勿把本分支的问题反馈到上游。** 本分支不提供任何担保，可能出现崩溃、渲染异常或**存档损坏**。
> - 本分支的移植改动（版本与依赖升级、第三方兼容先剥离、后续再重新加回、编译期修复）由 **AI 在人类指导下**完成，请视为实验性实现：依赖前请先审计代码，并在你自己的环境中实测。
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
| 状态 | ✅ 实验性可用：已在 Android（FCL + MobileGlues）真机跑通，可正常进存档游玩 |

构建产物由 GitHub Actions 的 `26.3-Snapshot` 工作流在每次推送后自动发布（Release 标签形如 `26.3-snapshot-<日期-时间>`）。文件名与 jar 内 `fabric.mod.json` 的版本号都带 git 短哈希：

```
touhoulittlemaid-fabric-1.0.0-26.3-<短哈希>.jar
```

## 本分支做了什么（相对上游 26.x）

### 修复上游尚未处理的 issue

截至 2026-10-07，上游 open 的 7 条里修了 6 条（第 7 条 `#49` 是 Kaleidoscope 兼容请求，按下面"兼容先剥离"的方针不做）：

| issue | 现象 | 修法 |
| --- | --- | --- |
| #41 | 相机收纳女仆 / 照片放出后女仆"消失"（有声音、重进存档又回来） | 客户端也执行了 `discard`（移植回归）→ 状态变更改为仅服务端；并且"放出失败"不再消耗物品、给出提示 |
| #37 | 御币不能在附魔台附魔 | 26.3 能否附魔由 `Enchantable` 数据组件决定（`Item#getEnchantmentValue` 已不可覆写）→ 补 `.enchantable(22)` |
| #47 | 手办超过 60 格只剩阴影 | `BlockEntityRenderer#getViewDistance()` 默认只有 64 → 手办提高到 256 |
| #35 | Gecko 模型女仆被抱起时不在肩上 | Gecko 渲染分支补上与基岩分支相同的肩部变换 |
| #48 | 坐垫 / 椅子上入睡时视角偏下 | 乘客坐标每 tick 被载具覆盖 → `startSleeping` 前先离开座椅 |
| #5 | 抱起失败后残留坐姿、看着像悬空 | 失败分支清坐姿、停寻路、清动量 |

### 有意保留的"看似死代码"

- **9 个方块实体渲染器的离屏渲染**：中国象棋盘 / 国际象棋盘 / 围棋盘 / 祭坛 / 野餐垫 = `shouldRenderOffScreen() = true`；女仆床 / 零食柜 / 雕像 / 坐垫 = `false`。
  模型明显大于方块本身时必须为 `true`，否则 ① 站在区块边界往另一侧看会"半截消失"；② 装了 EntityCulling / MoreCulling 时，锚点方块被挡住会让**整个模型不渲染**。
- **`InitDataComponent` 里的 `tanks` 数据组件**：代码里零引用，但老存档的物品数据会引用它 —— 删掉注册后载入存档会弹 `Missing content detected`。**注册保留、功能不搬。**
- **储物背包家族**（Tank / CraftingTable / EnderChest / Furnace）不搬回：上游 26.x 已主动砍掉，只剩 4 种纯存储背包。

### 原则

非官方移植，不做复杂定制：凡上游靠其它模组或非官方扩展才有的能力、且本移植没有入口/消费者的，直接删掉。

## 构建

```bash
./gradlew build     # 产物在 build/libs/，取带 git 短哈希的那个（不要取 -sources）
```

- 文件名与 jar 内 `fabric.mod.json` 版本都是 `1.0.0-26.3-<短哈希>`；带 `-dirty` 说明工作区有未提交改动（名字与内部版本不一致就是包不对）。
- 依赖与版本集中在 `gradle.properties`。
- 移植过程中的踩坑记录（26.3 API 变更、报错清单、渲染/注册机制）见 [`docs/porting-26.3.md`](docs/porting-26.3.md)。

## 保留的依赖

| 模组 | 版本 | 说明 |
| --- | --- | --- |
| Forge Config API Port | `26.3.1` | **硬依赖**（配置系统） |
| Cloth Config | `26.3.159` | 想在游戏内改配置就需要 |
| Mod Menu | `21.0.0` | 模组列表入口 |
| Sodium | `mc26.3-0.9.2-fabric` | 开发/测试环境保留 |
| Iris | `1.11.7+26.3-fabric` | 开发/测试环境保留 |
| Trinkets | `4.2.1+26.3` | 饰品（其 GUI 由上游禁用，详见移植笔记） |
| Inventory Profiles Next（+ libipn） | `fabric-26.3-2.3.8` / `fabric-26.3-6.9.0` | 可选 |
| JEI | `31.9.0.57` | 可选兼容，未安装自动降级 |
| Jade | `26.3.5+fabric` | 可选兼容，未安装自动降级 |
| Patchouli | `26.1-94-beta` ⚠️ | 上游未发布 26.2/26.3 版本，本分支仅以 `compileOnly` 占位，运行时由 `isModLoaded` 自动降级（手册入口不显示，不会崩溃） |

## 第三方兼容：先剥离，后续重新加回

**当前状态：JEI 与 Jade 已回搬**（entrypoint 已注册）。其余仍剥离，相对上游 `26.2` 共 **14 个包 / 31 个文件**：

| 兼容 | 文件数 |
| --- | --- |
| Aquaculture | 6 |
| Sophisticated Backpacks | 4 |
| TACZ / 卓越前线（gun） | 3 |
| Kaleidoscope | 3 |
| SlashBlade | 3 |
| Farmer's Delight | 2 |
| Immersive Melodies | 2 |
| Travelers' Backpack | 2 |
| Embeddium / Oculus / JMC / PatPat / Ponder / Simple Hats | 各 1 |

另移除 Improved Mobs 的 mixin、TACZ 工具类，以及随上游一起清掉的 YSM 动画链。

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
- **26.3 移植所用 AI 编码助手**：DeepSeek、GLM（智谱）、Kimi（月之暗面）、GPT（OpenAI）、Meta（Llama）
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
