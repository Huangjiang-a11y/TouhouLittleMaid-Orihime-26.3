package com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz;

import cn.sh1rocu.touhoulittlemaid.util.compat.tacz.ItemHandlerUtil;
import cn.sh1rocu.touhoulittlemaid.util.transfer.ItemUtil;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.ammo.AmmoSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 女仆专用的弹药来源，实现 TaCZ 1.1.8 起的官方下游扩展点
 * {@link com.tacz.guns.api.item.ammo.AmmoSourceRegistry#EVENT}。
 *
 * <p>TaCZ 会在 {@code canReload}/{@code hasInventoryAmmo}/{@code consumeAmmoFromPlayer}/
 * {@code hasAmmoToConsume} 等处统一查询本来源；javadoc 明确要求客户端动画预测与服务端实际
 * 消耗共用同一来源，因此这里只需在 common 初始化时注册一次，无需任何 mixin。
 * 女仆之外的实体返回 {@code null}，回退 TaCZ 自身的 {@code tacz$getItemHandler} 逻辑。</p>
 */
public enum MaidAmmoSource implements AmmoSource {
    INSTANCE;

    @Nullable
    public static AmmoSource find(LivingEntity shooter, ItemStack gunItem) {
        return shooter instanceof EntityMaid ? INSTANCE : null;
    }

    @Override
    public boolean hasAmmo(LivingEntity shooter, ItemStack gunItem) {
        if (!(shooter instanceof EntityMaid maid)) {
            return false;
        }
        var inv = maid.getAllInv();
        for (int i = 0; i < inv.getSlotCount(); i++) {
            ItemStack stack = ItemUtil.getStack(inv, i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(gunItem, stack)) {
                return true;
            }
            if (stack.getItem() instanceof IAmmoBox ammoBox && ammoBox.isAmmoBoxOfGun(gunItem, stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int consumeAmmo(LivingEntity shooter, ItemStack gunItem, int requestedAmount) {
        if (!(shooter instanceof EntityMaid maid)) {
            return 0;
        }
        return ItemHandlerUtil.findAndExtractInventoryAmmo(maid.getAllInv(), gunItem, requestedAmount);
    }
}
