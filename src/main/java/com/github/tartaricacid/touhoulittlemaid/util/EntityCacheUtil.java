package com.github.tartaricacid.touhoulittlemaid.util;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public final class EntityCacheUtil {
    private static final AtomicInteger PREVIEW_ENTITY_ID = new AtomicInteger(-10000);
    /**
     * 实体缓存，在客户端会大量运用实体渲染，这个缓存可以减少重复创建实体带来的性能问题
     */
    public static final Cache<EntityType<?>, Entity> ENTITY_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES).maximumSize(512).build();

    /**
     * 女仆实体缓存，用于雕像，因为雕像如果共用一个实体，会导致 GeckoLib 动画渲染错误。
     * 过期时间原为 10 秒，会导致雕像/GUI 预览实体被频繁销毁重建（卡顿 + 动画跳变），
     * 现放宽到 2 分钟并用 maximumSize 兜住内存上限。
     */
    public static final Cache<Long, EntityMaid> STATUE_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(2, TimeUnit.MINUTES).maximumSize(256).build();
    /**
     * 女仆实体缓存，用于物品形态的手办，因为如果共用一个实体，会导致 GeckoLib 动画渲染错误。
     * 同上：10 秒过期太短，容易被反复销毁重建。
     */
    public static final Cache<ItemStack, EntityMaid> GARAGE_KIT_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(2, TimeUnit.MINUTES).maximumSize(256).build();
    private static ResourceKey<Level> dimAt;

    /**
     * 创建一个"预览用"女仆：必须显式分配 ID。
     * <p>
     * 26.3 里 {@code Entity.getId()} 在未分配 ID 时会抛
     * {@code IllegalStateException: Tried to access entity ID before ID assignment}，
     * 而 {@code LivingEntityRenderer.extractRenderState} → {@code ItemModelResolver.updateForLiving}
     * 会去读实体 ID。所以任何"直接 new 出来、没有 add 进世界"的预览实体都必须自己塞一个 ID，
     * 否则渲染那一下就会炸（手办图标/雕像/GUI 预览都会中招）。
     */
    public static EntityMaid createPreviewMaid(Level level) {
        EntityMaid maid = new EntityMaid(level);
        maid.setId(PREVIEW_ENTITY_ID.getAndDecrement());
        return maid;
    }

    public static EntityMaid getMaid(Level level, EntitySpawnReason reason) {
        return getEntity(EntityMaid.TYPE, (l, _) -> createPreviewMaid(l), level, reason);
    }

    public static EntityChair getChair(Level level, EntitySpawnReason reason) {
        return getEntity(EntityChair.TYPE, (l, _) -> new EntityChair(l), level, reason);
    }

    @SuppressWarnings("unchecked")
    public static <E extends Entity> E getEntity(EntityType<E> type, BiFunction<Level, EntitySpawnReason, E> fallback, Level level, EntitySpawnReason reason) {
        try {
            return (E) ENTITY_CACHE.get(type, () -> {
                E e = type.create(level, reason);
                E entity = Objects.requireNonNullElseGet(e, () -> fallback.apply(level, reason));
                entity.setId(PREVIEW_ENTITY_ID.getAndDecrement());
                return entity;
            });
        } catch (ExecutionException e) {
            TouhouLittleMaid.LOGGER.error("Failed to create preview entity", e);
            E entity = fallback.apply(level, reason);
            entity.setId(PREVIEW_ENTITY_ID.getAndDecrement());
            return entity;
        }
    }

    public static EntityMaid getMaidInStatue(long key, Level level) throws ExecutionException {
        return STATUE_CACHE.get(key, () -> createPreviewMaid(level));
    }

    public static void clearMaidDataResidue(EntityMaid maid, boolean clearEquipmentData) {
        var animatable = maid.getAttached(GeckoMaidEntity.TYPE);
        if (animatable != null) {
            animatable.waitForAsyncUpdate();
        }

        maid.hurtDuration = 0;
        maid.hurtTime = 0;
        maid.deathTime = 0;
        maid.xRotO = 0;
        maid.yRotO = 0;
        maid.yBodyRotO = 0;
        maid.yHeadRotO = 0;

        maid.setXRot(0);
        maid.setYRot(0);
        maid.setYBodyRot(0);
        maid.setYHeadRot(0);

        maid.setOnGround(true);
        maid.setInSittingPose(false);
        maid.setMaidBackpackType(BackpackManager.getEmptyBackpack());
        maid.setCustomName(Component.empty());

        if (clearEquipmentData) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                maid.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    public static void onChangeDim(Entity entity, ClientLevel level) {
        if (entity == Minecraft.getInstance().player) {
            ResourceKey<Level> dim = entity.level.dimension();
            if (!dim.equals(dimAt)) {
                dimAt = dim;
                EntityCacheUtil.ENTITY_CACHE.invalidateAll();
            }
        }
    }
}
