package com.github.tartaricacid.touhoulittlemaid.compat.ponder;

import net.fabricmc.loader.api.FabricLoader;

/**
 * 机械动力（Create）的思索（Ponder）支持。
 * <p>
 * 26.3 这边由 Create Fly 提供这套 API，包名是 {@code com.zurrtum.create.client.ponder}，
 * 和上游 1.21.1 用的 {@code net.createmod.ponder}（独立的 Ponder 库）不是同一份，两边不能混用。
 * 所以这里只在运行期确认 Create 在场后才注册，编译期靠 libs 里的 Create Fly jar。
 */
public final class PonderCompat {
    public static final String MOD_ID = "create";

    private PonderCompat() {
    }

    public static void register() {
        if (FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            MaidPonderPlugin.register();
        }
    }
}
