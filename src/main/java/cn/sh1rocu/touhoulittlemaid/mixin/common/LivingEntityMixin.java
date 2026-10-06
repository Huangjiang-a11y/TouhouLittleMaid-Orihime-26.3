package cn.sh1rocu.touhoulittlemaid.mixin.common;

import cn.sh1rocu.touhoulittlemaid.util.neoforge.EventHooks;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntitySit;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow
    public abstract void setLastHurtByPlayer(UUID player, int timeToRemember);

    @Shadow
    public abstract ItemStack getUseItem();

    @Shadow
    public abstract int getUseItemRemainingTicks();

    @Shadow
    @Nullable
    protected EntityReference<Player> lastHurtByPlayer;

    @Shadow
    protected int lastHurtByPlayerMemoryTime;

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }


    @WrapOperation(method = "completeUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    public ItemStack tlm$onItemUseFinish(ItemStack instance, Level level, LivingEntity livingEntity, Operation<ItemStack> original) {
        return EventHooks.onItemUseFinish((LivingEntity) (Object) this, this.getUseItem().copy(), this.getUseItemRemainingTicks(), original.call(instance, level, livingEntity));
    }

    /**
     * 玩家坐在坐垫/座椅上入睡时，乘客坐标每帧都会被载具覆盖，摄像机会停在座椅高度（视角偏下，
     * 上游 issue #48）。入睡前先离开 TLM 的座椅实体，让玩家正常落在床上。
     */
    @Inject(method = "startSleeping", at = @At("HEAD"))
    private void tlm$leaveSeatBeforeSleep(net.minecraft.core.BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player) {
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof EntitySit || vehicle instanceof EntityChair) {
                player.stopRiding();
                vehicle.discard();
            }
        }
    }

    // 女仆攻击完成后，调用setLastHurtByPlayer，便于经验掉落等计算
    @Inject(
            method = "resolvePlayerResponsibleForDamage",
            at = @At("HEAD")
    )
    private void tlm$hurt(DamageSource source, CallbackInfoReturnable<Player> cir) {
        Entity attacker = source.getEntity();
        if (attacker instanceof EntityMaid maid && maid.isTame()) {
            if (maid.getOwnerReference() != null) {
                this.setLastHurtByPlayer(maid.getOwnerReference().getUUID(), 100);
            } else {
                this.lastHurtByPlayer = null;
                this.lastHurtByPlayerMemoryTime = 0;
            }
        }
    }

}
