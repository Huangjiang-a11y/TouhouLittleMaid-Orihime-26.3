package com.github.tartaricacid.touhoulittlemaid.compat.gun.common;

import com.github.tartaricacid.touhoulittlemaid.compat.gun.common.ai.GunShootTargetTask;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.common.task.TaskGunAttack;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.TacCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 枪械通用层。26.3 上只有 TaCZ 有 Fabric 版（TACZ-Refabricated 非官方移植），
 * 卓越前线（SBW）没有 Fabric 构建，故此处只保留 TaCZ 那条路。
 */
public class GunCommonUtil {
    public static void initAndAddTask(TaskManager manager) {
        if (TacCompat.init()) {
            manager.add(new TaskGunAttack());
        }
    }

    public static boolean isInstalled() {
        return TacCompat.isInstalled();
    }

    public static boolean isGun(ItemStack stack) {
        return TacCompat.isGun(stack);
    }

    /**
     * 不单单判断枪械，还判断女仆是否在载具上（女仆在载具上时也可以开火）
     */
    public static boolean canStartAttacking(EntityMaid maid) {
        return isGun(maid.getMainHandItem());
    }

    @Nullable
    public static Identifier getGunId(ItemStack stack) {
        return TacCompat.isGun(stack) ? TacCompat.getGunId(stack) : null;
    }

    public static Optional<Boolean> canSee(EntityMaid maid, LivingEntity target) {
        ItemStack handItem = maid.getMainHandItem();
        return TacCompat.isGun(handItem) ? Optional.of(TacCompat.canSee(maid, target)) : Optional.empty();
    }

    public static void tick(EntityMaid shooter, LivingEntity target, ItemStack gunItem) {
        // TaCZ 的射击推进由其自身的 IGunOperator 处理，这里无需额外 tick
    }

    public static int performGunAttack(EntityMaid shooter, LivingEntity target, ItemStack gunItem) throws Exception {
        return TacCompat.isGun(gunItem) ? TacCompat.performGunAttack(shooter, target, gunItem) : 100;
    }

    public static void onStop(EntityMaid maid, GunShootTargetTask task) {
        TacCompat.stopAim(maid);
    }
}
