package com.github.tartaricacid.touhoulittlemaid.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.TooltipDisplay;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * 旗指物：手持右击女仆为其装备一面背旗。
 * <p>
 * 装备方式与判定见 {@link com.github.tartaricacid.touhoulittlemaid.event.maid.HataSasimonoClickEvent}；
 * 装备后玩家无法攻击该女仆（见 EntityMaid#skipAttackInteraction）。样式在女仆配置页切换。
 */
public class ItemHataSasimono extends Item {
    public ItemHataSasimono(Identifier id) {
        super(new Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .stacksTo(16));
    }

    /**
     * 样式 → 旗面底色。0（未装备）返回 null 表示不渲染。
     * 目前样式就是不同底色，以后要加图案在这里扩展即可。
     */
    public static @Nullable DyeColor getFlagColor(int style) {
        return switch (style) {
            case 1 -> DyeColor.RED;
            case 2 -> DyeColor.WHITE;
            case 3 -> DyeColor.BLUE;
            case 4 -> DyeColor.BLACK;
            default -> null;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component
                .translatable("tooltips.touhou_little_maid.hata_sasimono.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
