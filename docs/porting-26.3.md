# 26.3 移植笔记

> 本文件记录 26.2 → 26.3 的 API 变更与移植决策，供后续提交（如 26.4）参考。

## 构建链

| 项 | 26.2 | 26.3 |
| --- | --- | --- |
| Gradle（wrapper） | 9.4.0 | **9.6.0** —— fabric-loom 1.17 要求 `org.gradle.plugin.api-version` ≥ 9.5.0，9.4.0 会在配置阶段直接失败 |
| fabric-loom | 1.16-SNAPSHOT | 1.17-SNAPSHOT（解析为 1.17.21） |
| Minecraft | 26.2 | 26.3 |
| Fabric Loader | 0.19.2 | 0.19.5 |
| fabric-api | 0.153.0+26.2 | 0.161.0+26.3 |

## 26.3 的两处大重构

### 1. 输入层：GLFW → SDL3（LWJGL 模块被换血）

26.3 的 Mojang manifest 里 LWJGL 只剩：
`lwjgl`、`freetype`、`jemalloc`、`openal`、`opengl`、**`sdl`**、`shaderc`、`spvc`、`stb`、`vma`、`vulkan`。

**`lwjgl-glfw` 与 `lwjgl-tinyfd` 双双被移除**，Mod 侧凡直接用
`org.lwjgl.glfw.GLFW`（原生键码）或 `org.lwjgl.util.tinyfd`（文件对话框）都会编译失败。

- 键码改用仍在 `com.mojang.blaze3d.platform` 下的 `InputConstants`。
- 文件/目录选择框需要改用 `lwjgl-sdl`（`SDL_ShowOpenFileDialog` 等），或暂时移除该交互。

### 2. 渲染层：提交节点（SubmitNode）+ 特性渲染器（FeatureRenderer）

- `MultiBufferSource` 在 26.1/26.2/26.3 三个版本中**均不存在**；26.x 的渲染走
  `SubmitNodeCollector`（`net.minecraft.client.renderer.SubmitNodeCollector`）与
  `net.minecraft.client.renderer.feature.*` 系列 `FeatureRenderer`。
- 第一人称手部渲染从 `ItemInHandRenderer` 改为
  **`FirstPersonHandsAndItemsRenderer`**（配套 `FirstPersonHandsAndItems`、
  `FirstPersonHandsAndItemsRenderState`）。
- `GpuBufferSlice` 迁至 `com.mojang.renderpearl.api.buffers.GpuBufferSlice`。

## 首次 `compileJava` 报错清单（15 文件 / 28 错）与修法

| 报错 | 文件 | 修法 |
| --- | --- | --- |
| `RedStoneWireBlock` 找不到 | `cn/sh1rocu/.../mixin/common/RedStoneWireBlockMixin` | 26.3 更名为 **`RedstoneWireBlock`**（仅大小写） |
| `org.lwjgl.glfw` 包不存在 | PressAIChatKeyEvent、DismountBroomKey、STTChatKey、AttackTaskConfigGui、AIChatScreen | 改用 `InputConstants` |
| `org.lwjgl.util.tinyfd` 包不存在 | SettingEditScreen | 改 `lwjgl-sdl` 的 SDL 对话框 |
| `ContextAwarePredicate` 找不到 | AltarCraftTrigger、MaidEventTrigger、GivePatchouliBookConfigTrigger、GiveSmartSlabConfigTrigger | 26.3 同包内该类消失且无新类补位，需按 `SimpleInstance` 新签名改 |
| `ItemInHandRenderer` 类消失 | `cn/sh1rocu/.../mixin/client/ItemInHandRendererMixin` | 重定向到 `FirstPersonHandsAndItemsRenderer` |
| `MultiBufferSource` | `mixin/client/DrawableGizmoPrimitives$GroupMixin`（**26.1 合并引入，26.2 分支没有，从未编译过**） | 按 26.3 的 `Font` + 提交节点 API 重写 |
| `FlyingAnimal` 找不到 | EntityFairy | `net.minecraft.world.entity.animal.FlyingAnimal` 在 26.1/26.2/26.3 均不存在 → 从 implements 摘除（飞行行为保留 `FlyingMoveControl`/导航） |
| `ShovelItem` 找不到 | TaskSnow | 26.3 删除该类（工具数据化）→ 改 tag/组件判定 |
| `ConstantValue`/`UniformGenerator` | LootTableGenerator | 26.3 拆为 `.number.floats.*` 与 `.number.ints.*` 两套 |
| `GpuBufferSlice` | LevelRendererMixin | 新包路径 `com.mojang.renderpearl.api.buffers` |

> 备注：`MultiBufferSource` 与 `FlyingAnimal` 两处源自 26.1 分支合并进来的代码，
> 即那批"26 个修复"本身并未编译通过，需要在本分支重写。

## 网络环境备注

中国大陆直连测试：`maven.fabricmc.net`、`libraries.minecraft.net`、`piston-meta.mojang.com`、
`api.modrinth.com`、`cdn.modrinth.com`、`maven.terraformersmc.com`、`maven.shedaniel.me`、
`repo1.maven.org`、`raw.githubusercontent.com`、`ghfast.top`、`api.github.com` 均可达；
仅 **`github.com`（git 协议）** 不通。因此构建依赖可直连拉取，仅推送需要走
GitHub Git Data API（`api.github.com`）或另行代理。
