package com.github.tartaricacid.touhoulittlemaid.client.event;

import com.github.tartaricacid.touhoulittlemaid.client.renderer.item.ChairItemRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.item.GarageKitItemRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.item.ReplaceableSpriteProperty;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.special.SpecialModelRenderers;

public class RegisterSpecialModelEvent {
    public static void registerSpecialModelRenderers() {
        var mapper = SpecialModelRenderers.ID_MAPPER;
        mapper.put(ChairItemRenderer.CHAIR_ITEM_RENDERER, ChairItemRenderer.Unbaked.MAP_CODEC);
        mapper.put(GarageKitItemRenderer.GARAGE_KIT_ITEM_RENDERER, GarageKitItemRenderer.Unbaked.MAP_CODEC);
    }

    /**
     * 注册读 VanillaConfig 的物品模型属性（原版 minecraft:condition 模型使用）。
     * <p>
     * 属性注册表是 LateBoundIdMapper，its codec 捕获的是活引用（idResolverCodec 用的是同一
     * 个 BiMap），所以可以在这里晚注册，不需要混入原版 bootstrap。
     */
    public static void registerItemModelProperties() {
        ConditionalItemModelProperties.ID_MAPPER
                .put(ReplaceableSpriteProperty.REPLACE_TOTEM_TEXTURE, ReplaceableSpriteProperty.TOTEM_CODEC);
        ConditionalItemModelProperties.ID_MAPPER
                .put(ReplaceableSpriteProperty.REPLACE_XP_BOTTLE_TEXTURE, ReplaceableSpriteProperty.XP_BOTTLE_CODEC);
    }
}
