package com.github.tartaricacid.touhoulittlemaid.event.maid;

import com.github.tartaricacid.touhoulittlemaid.advancements.maid.TriggerType;
import com.github.tartaricacid.touhoulittlemaid.api.event.InteractMaidEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitTrigger;
import com.github.tartaricacid.touhoulittlemaid.util.migrate.ScreenUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class SaddleMaidEvent {
    public static void onInteract(InteractMaidEvent event) {
        Player player = event.getPlayer();
        EntityMaid maid = event.getMaid();
        ItemStack stack = event.getStack();
        if (stack.is(Items.SADDLE)) {
            if (player.getPassengers().isEmpty() && maid.getPassengers().isEmpty()) {
                // 女仆可能正坐在椅子/座椅实体（EntitySit）身上，此时 startRiding 会静默失败，
                // 表现就是"抱起后女仆留在原地悬空"。先让它下座再抱。
                if (maid.isPassenger()) {
                    maid.stopRiding();
                }
                boolean success = maid.startRiding(player);
                if (!success) {
                    // 没抱起来就不算交互成功：不给成就、不关家模式
                    event.setCanceled(true);
                    return;
                }
                // 停掉残留寻路与动量，避免客户端上看起来还停在原地
                maid.getNavigation().stop();
                maid.setDeltaMovement(Vec3.ZERO);
                if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                    SaddleMaidEvent.showTips();
                }
                if (maid.isHomeModeEnable()) {
                    maid.setHomeModeEnable(false);
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    InitTrigger.MAID_EVENT.trigger(serverPlayer, TriggerType.PICKUP_MAID);
                }
                event.setCanceled(true);
                return;
            }
            if (!player.getPassengers().isEmpty()) {
                player.ejectPassengers();
                event.setCanceled(true);
            }
        }
    }

    public static void showTips() {
        Component component = Component.translatable("message.touhou_little_maid.saddle.how_to_eject");
        ScreenUtil.setOverlayMessage(component, false);
    }
}
