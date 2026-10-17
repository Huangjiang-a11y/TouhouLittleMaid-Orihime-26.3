package com.github.tartaricacid.touhoulittlemaid.api.task;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.data.AttackListData;
import com.github.tartaricacid.touhoulittlemaid.entity.item.AbstractEntityFromItem;
import com.github.tartaricacid.touhoulittlemaid.entity.misc.DefaultMonsterType;
import com.github.tartaricacid.touhoulittlemaid.entity.misc.MonsterType;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.AbstractMaidContainer;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.task.AttackTaskConfigContainer;
import com.github.tartaricacid.touhoulittlemaid.util.TaskEquipUtil;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

import static com.github.tartaricacid.touhoulittlemaid.init.InitDataAttachment.ATTACK_LIST;

public interface IAttackTask extends IMaidTask {
    String MAID_NO_ATTACK_TAG = "MaidNoAttack";

    /**
     * 寻找合适的第一个攻击目标
     *
     * @param maid 女仆
     * @return 合适的攻击目标
     */
    static Optional<? extends LivingEntity> findFirstValidAttackTarget(EntityMaid maid) {
        // 谁刚打了女仆自己，就先打回去，而不是傻傻只挑最近的
        Optional<? extends LivingEntity> revenge = findRevengeTarget(maid);
        if (revenge.isPresent()) {
            return revenge;
        }
        return maid.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).flatMap(
                mobs -> mobs.findClosest((e) -> maid.canAttack(e) && maid.isWithinHome(e.blockPosition())));
    }

    /**
     * 复仇记忆的时效（tick）。
     * <p>
     * 原版 {@code HurtByTargetGoal} 用 300 tick 判断"这口气还值不值得记"，
     * 这里对齐同一个语义：{@code getLastHurtByMob()} 返回的是永不过期的字段，
     * 不自己判时效的话，女仆会把"很久以前打过她的东西"当成终身追杀目标，
     * 于是永远退不出攻击状态 —— 枪械女仆的走位任务（GunAttackStrafingTask）会因此
     * 一直接管移动（strafe / stopInPlace），表现就是"一动不动、根本没从枪械 AI 里切出来"。
     */
    int REVENGE_MEMORY_TICKS = 300;

    /**
     * 找"该报复的人"：只有"刚打了女仆自己的"那份仇才算数。
     * <p>
     * 原版近战只挑最近的敌人，于是女仆被远程怪 / 边上的怪偷袭时会一直无动于衷；
     * 这里把上游远程那条自我复仇推广到近战，但**不含主人那边** ——
     * 护主会让锁定面变得太宽（任何打过主人的东西都会被她当成目标），收益不值这个风险。
     */
    static Optional<? extends LivingEntity> findRevengeTarget(EntityMaid maid) {
        LivingEntity selfAttacker = maid.getLastHurtByMob();
        if (isValidRevengeTarget(maid, selfAttacker)
            && isRevengeFresh(maid.tickCount, maid.getLastHurtByMobTimestamp())) {
            return Optional.of(selfAttacker);
        }
        return Optional.empty();
    }

    static boolean isRevengeFresh(int nowTick, int hurtTimestamp) {
        // 必须用女仆自己的 tickCount：lastHurtByMobTimestamp 也是在女仆的 tickCount 基准上写入的。
        // 之前误用了攻击者的 tickCount，两个实体计数器原点不同，几乎永远判定为"过期"，复仇目标永远拿不到。
        return nowTick - hurtTimestamp <= REVENGE_MEMORY_TICKS;
    }

    static boolean isValidRevengeTarget(EntityMaid maid, LivingEntity attacker) {
        return attacker != null && attacker.isAlive() && attacker != maid
                && maid.canAttack(attacker) && maid.canSee(attacker)
                && maid.isWithinHome(attacker.blockPosition());
    }

    /**
     * 能否攻击该对象
     *
     * @param maid   女仆
     * @param target 攻击的目标
     * @return 能否攻击？
     */
    default boolean canAttack(EntityMaid maid, LivingEntity target) {
        // 获取实体 ID
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());

        // 排除一些盔甲架，还有本模组的实体，以及玩家
        if (target instanceof ArmorStand || target instanceof AbstractEntityFromItem || target instanceof Player) {
            return false;
        }
        // 有主的宠物也不攻击
        if (target instanceof TamableAnimal tamableAnimal && tamableAnimal.getOwnerReference() != null) {
            return false;
        }
        // 特殊命名的怪物，因为有的玩家会使用怪物做刷怪塔，会被女仆误杀
        if (target.getCustomName() != null && target.getCustomName().getString().startsWith(MAID_NO_ATTACK_TAG)) {
            return false;
        }

        // 判断配置文件的
        if (MaidConfig.MAID_ATTACK_IGNORE.get().contains(id.toString())) {
            return false;
        }

        MonsterType monsterType;
        AttackListData attackListData = maid.getAttachedOrCreate(ATTACK_LIST);
        if (attackListData.attackGroups().containsKey(id)) {
            monsterType = attackListData.attackGroups().get(id);
        } else {
            // 那如果没有呢？走默认配置
            monsterType = DefaultMonsterType.getMonsterType(target);
        }
        return DefaultMonsterType.canAttack(maid, target, monsterType);
    }

    /**
     * 是否拥有额外攻击方式，用于一些额外增伤的设计
     * 比如女仆副手持有灭火器，会额外对下界生物造成二次伤害
     *
     * @param maid   女仆
     * @param target 攻击目标
     * @return 是否有额外攻击方式
     */
    default boolean hasExtraAttack(EntityMaid maid, Entity target) {
        return false;
    }

    /**
     * 执行额外伤害
     *
     * @param maid   女仆
     * @param target 攻击目标
     * @return 是否成功造成伤害
     */
    default boolean doExtraAttack(EntityMaid maid, Entity target) {
        return false;
    }

    /**
     * 是适合的攻击武器么，用于女仆 AI 判断当前武器在当前模式下是否能正常使用
     *
     * @param maid  女仆
     * @param stack 检查的物品
     * @return 在当前模式下是否能正常使用
     */
    default boolean isWeapon(EntityMaid maid, ItemStack stack) {
        return false;
    }

    /**
     * 攻击类任务在 Function Call 触发的默认切换逻辑：确保主手持有合适的武器。
     */
    @Override
    default FunctionCallSwitchResult onFunctionCallSwitch(EntityMaid maid) {
        if (isWeapon(maid, maid.getMainHandItem())) {
            return FunctionCallSwitchResult.NO_CHANGE;
        }
        if (TaskEquipUtil.tryEquipFromBackpack(maid, item -> isWeapon(maid, item))) {
            return FunctionCallSwitchResult.OK;
        }
        return FunctionCallSwitchResult.MISSING_REQUIRED_ITEM;
    }

    @Override
    default MenuProvider getTaskConfigGuiProvider(EntityMaid maid) {
        final int entityId = maid.getId();
        return new ExtendedMenuProvider<Integer>() {
            @Override
            public Integer getScreenOpeningData(ServerPlayer player) {
                return entityId;
            }

            @Override
            public Component getDisplayName() {
                return Component.literal("Maid Attack Config Container");
            }

            @Override
            public AbstractMaidContainer createMenu(int index, Inventory playerInventory, Player player) {
                return new AttackTaskConfigContainer(index, playerInventory, entityId);
            }

            @Override
            public boolean shouldCloseCurrentScreen() {
                return false;
            }
        };
    }

    @Override
    default boolean enablePanic(EntityMaid maid) {
        return false;
    }
}
