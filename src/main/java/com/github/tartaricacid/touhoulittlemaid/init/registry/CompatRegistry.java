package com.github.tartaricacid.touhoulittlemaid.init.registry;

import com.github.tartaricacid.touhoulittlemaid.compat.patchouli.PatchouliCompat;
import net.fabricmc.loader.api.FabricLoader;

/**
 * 26.3 移植：只保留白名单内的模组兼容 —— forgeconfigapiport / trinkets /
 * IPN / sodium / modmenu / cloth-config / patchouli / iris。
 * 其余第三方兼容（jei、jade、curios 本体、背包类、农夫乐事、枪械类等）已剥离。
 */
public final class CompatRegistry {
    public static final String PATCHOULI = "patchouli";
    // 为什么 Fabric 端的 id 要改（
    public static final String CLOTH_CONFIG = "cloth-config";
    public static final String TRINKETS = "trinkets";

    public static void onEnqueue() {
        checkModLoad(PATCHOULI, PatchouliCompat::init);
    }

    private static void checkModLoad(String modId, Runnable runnable) {
        if (FabricLoader.getInstance().isModLoaded(modId)) {
            runnable.run();
        }
    }
}
