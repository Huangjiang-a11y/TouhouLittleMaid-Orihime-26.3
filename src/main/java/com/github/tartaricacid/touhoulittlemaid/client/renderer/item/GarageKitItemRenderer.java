package com.github.tartaricacid.touhoulittlemaid.client.renderer.item;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.client.render.MaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.SimpleBedrockModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.blockentity.state.GarageKitRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.item.ItemGarageKit;
import com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil;
import com.github.tartaricacid.touhoulittlemaid.util.migrate.EntityTypeUtil;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.joml.Vector3fc;
import java.util.IdentityHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry.STATUE_BASE;
import static com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil.clearMaidDataResidue;

/**
 * GarageKit 物品的特殊模型渲染器，替代旧版 BlockEntityWithoutLevelRenderer
 * <p>
 * 参考 GarageKitRenderer 的实体渲染模式实现。
 * 底座模型（STATUE_BASE）通过 submitCustomGeometry 渲染。
 * 实体预览通过 EntityRenderDispatcher.submit() 渲染。
 */
public class GarageKitItemRenderer implements SpecialModelRenderer<GarageKitRenderState> {
    public static final Identifier GARAGE_KIT_ITEM_RENDERER = IdentifierUtil.modLoc("garage_kit_item");
    private static final Identifier TEXTURE = IdentifierUtil.modLoc("textures/bedrock/block/statue_base.png");
    private final SimpleBedrockModel<Unit> baseModel;
    /**
     * 物品缩略图缓存。
     * <p>
     * extractArgument 每帧都会被调用（物品栏/GUI 里每个手办每帧一次），而一次提取要反序列化整份
     * 女仆 NBT、重置预览实体、再提取渲染状态；所有手办共用同一个预览实体时还会互相打断动画，
     * 表现为"图标乱动 + 疯狂重建 + 卡顿"。
     * <p>
     * 这里按"数据组件实例 + 当前 Level"缓存提取结果：同一个手办只做一次，图标成为静态缩略图。
     * 用 IdentityHashMap 是因为同一格的组件实例稳定，身份比较零成本，避免每帧对手办 NBT 做深哈希。
     */
    private static final Map<CustomData, IconEntry> ICON_CACHE = new IdentityHashMap<>();
    private static final int ICON_CACHE_LIMIT = 256;

    private record IconEntry(Level level, CompoundTag extraData, EntityRenderState entityRenderState) {
    }

    /**
     * 模型包/资源包重载后必须调用，否则缩略图会停在旧模型上
     */
    public static void clearIconCache() {
        ICON_CACHE.clear();
    }

    public GarageKitItemRenderer() {
        this.baseModel = InternalBedrockModelRegistry.getModel(STATUE_BASE);
    }

    @Override
    public GarageKitRenderState extractArgument(ItemStack stack) {
        GarageKitRenderState state = new GarageKitRenderState();
        CustomData data = ItemGarageKit.getMaidData(stack);
        state.extraData = data.copyTag();
        state.entityRenderState = null;

        // 提取实体渲染状态
        if (state.extraData.isEmpty()) {
            return state;
        }
        Level world = Minecraft.getInstance().level;
        if (world == null) {
            return state;
        }
        Optional<String> id = state.extraData.getString("id");
        if (id.isEmpty()) {
            return state;
        }

        // 同一个手办（同一格、同一份数据）只提取一次，之后复用快照
        IconEntry cached = ICON_CACHE.get(data);
        if (cached != null && cached.level() == world) {
            state.extraData = cached.extraData();
            state.entityRenderState = cached.entityRenderState();
            return state;
        }

        EntityTypeUtil.byString(id.get()).ifPresent(type -> {
            try {
                extractEntityRenderState(state, stack, state.extraData, world, type);
            } catch (ExecutionException e) {
                TouhouLittleMaid.LOGGER.error("Failed to extract garage kit item entity render state", e);
            }
        });

        if (state.entityRenderState != null) {
            if (ICON_CACHE.size() >= ICON_CACHE_LIMIT) {
                ICON_CACHE.clear();
            }
            ICON_CACHE.put(data, new IconEntry(world, state.extraData, state.entityRenderState));
        }
        return state;
    }

    @SuppressWarnings("unchecked,rawtypes")
    private void extractEntityRenderState(GarageKitRenderState state, ItemStack stack, CompoundTag data,
                                          Level world, EntityType<?> type) throws ExecutionException {
        Entity entity;
        if (type.equals(InitEntities.MAID)) {
            // GARAGE_KIT_CACHE 本来就是"一手办一预览实体"用的（注释里写了共用一个实体会导致
            // GeckoLib 动画渲染错误），26.3 移植里被漏掉了，这里用回去。
            entity = EntityCacheUtil.GARAGE_KIT_CACHE.get(stack.copy(), () -> new EntityMaid(world));
        } else {
            entity = EntityCacheUtil.getEntity((EntityType) type, (l, e) ->
                    new EntityMaid(l), world, EntitySpawnReason.LOAD);
        }

        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), data));
        if (entity instanceof EntityMaid maid) {
            clearMaidDataResidue(maid, true);
            maid.renderState = MaidRenderState.GARAGE_KIT_ITEM;
            maid.tickCount = 0;
        }

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        state.entityRenderState = dispatcher.extractEntity(entity, 0);
        state.entityRenderState.lightCoords = LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    public void submit(
            GarageKitRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor
    ) {
        // 渲染底座模型
        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 0.5f);
        poseStack.translate(1, 1.5, 1);
        poseStack.rotate(Axis.ZN.rotationDegrees(180));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, buffer) -> {
            poseStack.pushPose();
            poseStack.last().set(pose);
            baseModel.renderToBuffer(poseStack, buffer, lightCoords, overlayCoords, -1);
            poseStack.popPose();
        });
        poseStack.popPose();

        // 渲染实体预览
        if (state.entityRenderState != null) {
            renderEntityPart(state, poseStack, collector);
        }
    }

    private void renderEntityPart(GarageKitRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.entityRenderState == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 0.5f);
        poseStack.translate(1, 0.21328125, 1);
        poseStack.rotate(Axis.YP.rotationDegrees(180));

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        CameraRenderState camera = new CameraRenderState();
        dispatcher.submit(state.entityRenderState, camera, 0, 0, 0, poseStack, collector);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        // 从底座模型获取 GUI 范围
        PoseStack poseStack = new PoseStack();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        baseModel.root().getExtentsForGui(poseStack, output);
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<GarageKitRenderState> {
        public static final Identifier ID = IdentifierUtil.modLoc("garage_kit");
        public static final MapCodec<GarageKitItemRenderer.Unbaked> MAP_CODEC = MapCodec.unit(GarageKitItemRenderer.Unbaked::new);

        @Override
        public MapCodec<GarageKitItemRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public GarageKitItemRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new GarageKitItemRenderer();
        }
    }
}
