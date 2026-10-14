package com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidEquipEvent;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidHurtEvent;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.event.GunHurtMaidEvent;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.event.MaidGunEquipEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.item.ammo.AmmoSourceRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * TaCZ 兼容门面（服务端）。
 * <p>
 * 客户端那半（枪械动画、背枪渲染、手臂姿态）在 {@code GunClientUtil} 里，调用点也都在那里，
 * 故此处只保留服务端逻辑。
 */
public class TacCompat {
    public static final Identifier MINIGUN_ID = Identifier.fromNamespaceAndPath("tacz", "minigun");
    private static final String TACZ_ID = "tacz";
    private static boolean INSTALLED = false;

    public static boolean init() {
        if (FabricLoader.getInstance().isModLoaded(TACZ_ID)) {
            GunHurtMaidEvent gunHurtMaidEvent = new GunHurtMaidEvent();
            MaidHurtEvent.CALLBACK.register(gunHurtMaidEvent::onMaidHurt);
            EntityHurtByGunEvent.PRE.register(gunHurtMaidEvent::onGunHurt);

            MaidGunEquipEvent maidGunEquipEvent = new MaidGunEquipEvent();
            MaidEquipEvent.CALLBACK.register(maidGunEquipEvent::onMaidEquip);

            // 女仆背包作为弹药来源：TaCZ 1.1.8 起的官方下游扩展点（AmmoSourceRegistry.EVENT），
            // 客户端动画预测与服务端实际消耗共用同一来源；非女仆返回 null，回退 TaCZ 自身逻辑。
            AmmoSourceRegistry.EVENT.register(MaidAmmoSource::find);

            INSTALLED = true;
        }
        return INSTALLED;
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static boolean isGun(ItemStack stack) {
        return INSTALLED && TacInnerCompat.isGun(stack);
    }

    public static boolean isGrenade(ItemStack itemStack) {
        // TODO 手雷还没有
        return false;
    }

    @Nullable
    public static Identifier getGunId(ItemStack stack) {
        return INSTALLED ? TacInnerCompat.getGunId(stack) : null;
    }

    public static boolean canSee(EntityMaid maid, LivingEntity target) {
        return INSTALLED && TacInnerCompat.canSee(maid, target);
    }

    public static int performGunAttack(EntityMaid shooter, LivingEntity target, ItemStack gunItem) throws Exception {
        return INSTALLED ? TacInnerCompat.performGunAttack(shooter, target, gunItem) : 100;
    }

    public static void stopAim(EntityMaid maid) {
        if (INSTALLED) {
            TacInnerCompat.stopAim(maid);
        }
    }
}
