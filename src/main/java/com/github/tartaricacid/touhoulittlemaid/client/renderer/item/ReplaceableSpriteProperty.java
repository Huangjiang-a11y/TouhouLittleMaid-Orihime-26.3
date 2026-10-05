package com.github.tartaricacid.touhoulittlemaid.client.renderer.item;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 读 {@link VanillaConfig} 的布尔物品模型属性，配合原版 minecraft:condition 模型，
 * 在「TLM 贴图」和「原版贴图」两个普通物品模型之间切换。
 * <p>
 * 走的是原版物品渲染管线（和 fishing_rod / compass 同一套），属性值在渲染时求值，
 * 所以改配置立即生效、不需要重载资源，也不存在自己画四边形画不出来的问题。
 */
public record ReplaceableSpriteProperty(SpriteSwitch spriteSwitch) implements ConditionalItemModelProperty {
    public static final Identifier REPLACE_TOTEM_TEXTURE =
            Identifier.fromNamespaceAndPath("touhou_little_maid", "replace_totem_texture");
    public static final Identifier REPLACE_XP_BOTTLE_TEXTURE =
            Identifier.fromNamespaceAndPath("touhou_little_maid", "replace_xp_bottle_texture");

    public static final MapCodec<ReplaceableSpriteProperty> TOTEM_CODEC =
            MapCodec.unit(new ReplaceableSpriteProperty(SpriteSwitch.TOTEM));
    public static final MapCodec<ReplaceableSpriteProperty> XP_BOTTLE_CODEC =
            MapCodec.unit(new ReplaceableSpriteProperty(SpriteSwitch.XP_BOTTLE));

    @Override
    public boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext context) {
        return this.spriteSwitch.isEnabled();
    }

    @Override
    public MapCodec<? extends ConditionalItemModelProperty> type() {
        return this.spriteSwitch == SpriteSwitch.TOTEM ? TOTEM_CODEC : XP_BOTTLE_CODEC;
    }
}
