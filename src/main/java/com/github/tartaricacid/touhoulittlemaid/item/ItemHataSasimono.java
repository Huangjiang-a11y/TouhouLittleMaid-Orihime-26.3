package com.github.tartaricacid.touhoulittlemaid.item;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
 * 与 1.12.2 原版的差异：装备后女仆<b>不受玩家任何伤害</b>（原版是"受玩家伤害减少"），
 * 见 MaidCombatManager#hurtServer。样式在女仆 GUI 的"女仆配置"页切换。
 */
public class ItemHataSasimono extends Item {
    public ItemHataSasimono(Identifier id) {
        super(new Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .stacksTo(16));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof EntityMaid maid) || !maid.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        // 已经装备过就不再消耗
        if (maid.getConfigManager().getSasimonoStyle() > 0) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide()) {
            // 默认给第一个样式，具体样式在女仆配置页切换
            maid.getConfigManager().setSasimonoStyle(1);
            // 沿用老版行为：装备后清掉当前目标
            maid.setTarget(null);
            stack.shrink(1);
            maid.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
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
