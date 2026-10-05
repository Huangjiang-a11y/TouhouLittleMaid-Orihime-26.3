package com.github.tartaricacid.touhoulittlemaid.client.renderer.item;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.VanillaConfig;

/**
 * 原版替换的开关来源，对应 {@link VanillaConfig} 里的两个物品贴图选项
 * （史莱姆模型/经验球纹理是渲染器级别的，不走这里）。
 */
public enum SpriteSwitch {
    TOTEM,
    XP_BOTTLE;

    public boolean isEnabled() {
        return switch (this) {
            case TOTEM -> VanillaConfig.REPLACE_TOTEM_TEXTURE.get();
            case XP_BOTTLE -> VanillaConfig.REPLACE_XP_BOTTLE_TEXTURE.get();
        };
    }
}
