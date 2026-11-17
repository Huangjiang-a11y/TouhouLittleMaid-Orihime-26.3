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
import org.joml.Vector3fc;

import java.util.HashMap;
import java.util.Map;
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

    // 同 ChairItemRenderer：这个渲染器每帧都会被重调，而 extractArgument 里一次 copyTag()
    // 加一次完整实体 load + extractEntity 才是真开销。手办的数据挂在不可变组件上，同一个
    // ItemStack 反复取到的是同一个 CustomData 实例，按它缓存即可。
    private static final int CACHE_LIMIT = 128;
    private static final Map<CustomData, GarageKitRenderState> STATE_CACHE = new HashMap<>();
    private static Level cachedLevel;

    public GarageKitItemRenderer() {
        this.baseModel = InternalBedrockModelRegistry.getModel(STATUE_BASE);
    }

    @Override
    public GarageKitRenderState extractArgument(ItemStack stack) {
        CustomData data = ItemGarageKit.getMaidData(stack);

        Level currentLevel = Minecraft.getInstance().level;
        if (currentLevel != cachedLevel) {
            clearCache();
            cachedLevel = currentLevel;
        }
        if (currentLevel != null) {
            GarageKitRenderState cached = STATE_CACHE.get(data);
            if (cached != null) {
                return cached;
            }
        }

        GarageKitRenderState state = new GarageKitRenderState();
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
        EntityTypeUtil.byString(id.get()).ifPresent(type -> {
            try {
                extractEntityRenderState(state, state.extraData, world, type);
            } catch (ExecutionException e) {
                TouhouLittleMaid.LOGGER.error("Failed to extract garage kit item entity render state", e);
            }
        });
        if (state.entityRenderState != null && currentLevel != null) {
            if (STATE_CACHE.size() >= CACHE_LIMIT) {
                STATE_CACHE.clear();
            }
            STATE_CACHE.put(data, state);
        }
        return state;
    }

    /** 资源包重载会换掉手办模型与实体渲染器，缓存的实体渲染状态必须作废。 */
    public static void clearCache() {
        STATE_CACHE.clear();
        cachedLevel = null;
    }

    @SuppressWarnings("unchecked,rawtypes")
    private void extractEntityRenderState(GarageKitRenderState state, CompoundTag data,
                                          Level world, EntityType<?> type) throws ExecutionException {
        Entity entity;
        if (type.equals(InitEntities.MAID)) {
            entity = EntityCacheUtil.getMaid(world, EntitySpawnReason.LOAD);
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
