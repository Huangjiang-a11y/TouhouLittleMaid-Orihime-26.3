package com.github.tartaricacid.touhoulittlemaid.event.maid;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.event.InteractMaidEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 旗指物：主手右击女仆时装备（走 {@link InteractMaidEvent}，与本 mod 其他可交互物品一致，
 * 例如 {@link SlabClickEvent}（魂符）、{@link UseFavorabilityToolEvent}（好感度工具））。
 */
public final class HataSasimonoClickEvent {
    public static void onInteract(InteractMaidEvent event) {
        Player player = event.getPlayer();
        EntityMaid maid = event.getMaid();
        ItemStack stack = event.getStack();

        if (!stack.is(InitItems.HATA_SASIMONO)) {
            return;
        }

        if (!maid.isOwnedBy(player)) {
            if (!event.getWorld().isClientSide()) {
                player.sendOverlayMessage(Component
                        .translatable("message.touhou_little_maid.hata_sasimono.not_owner")
                        .withStyle(ChatFormatting.RED));
            }
            // 不取消：把后续判定（驯服等）留给女仆自己
            return;
        }

        if (!event.getWorld().isClientSide()) {
            if (maid.getConfigManager().getSasimonoStyle() > 0) {
                TouhouLittleMaid.LOGGER.info("[Sasimono] 女仆 {} 已装备 style={}",
                        maid.getUUID(), maid.getConfigManager().getSasimonoStyle());
                player.sendOverlayMessage(Component
                        .translatable("message.touhou_little_maid.hata_sasimono.already")
                        .withStyle(ChatFormatting.YELLOW));
            } else {
                // 默认给第一个样式，具体样式在女仆配置页切换
                maid.getConfigManager().setSasimonoStyle(1);
                // 沿用老版行为：装备后清掉当前目标
                maid.setTarget(null);
                stack.shrink(1);
                maid.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
                TouhouLittleMaid.LOGGER.info("[Sasimono] {} 给女仆 {} 装备旗指物 style=1",
                        player.getName().getString(), maid.getUUID());
                player.sendOverlayMessage(Component
                        .translatable("message.touhou_little_maid.hata_sasimono.equipped")
                        .withStyle(ChatFormatting.GREEN));
            }
        }

        // 取消后续所有右击判定（物品交互、开女仆 GUI）
        event.setCanceled(true);
    }
}
