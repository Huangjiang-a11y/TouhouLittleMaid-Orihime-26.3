package com.github.tartaricacid.touhoulittlemaid.client.book;

import com.github.tartaricacid.touhoulittlemaid.util.migrate.EntityTypeUtil;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * entity 页：页内渲染一个实体，如 {@code touhou_little_maid:fairy{OnGround:true}}。
 * <p>
 * 两个坑：
 * <ol>
 *   <li>渲染状态建好后必须缓存，**失败也要记下来别再试**。否则建不出来时会每帧重来一遍
 *       （客户端曾因此掉到 ~0.6 FPS：p99 帧耗时 1.7 秒）。</li>
 *   <li>构建方式照抄仓库里已经在跑的 {@code AbstractModelDetailsGui#renderEntity}：
 *       {@code renderer.createRenderState(entity, 1.0F)} + 清阴影 + 归一化 LivingEntity 的
 *       boundingBox/scale，再配 {@code graphics.entity(...)}。</li>
 * </ol>
 * 注意：文字由 BookScreen#renderBody 统一绘制，这里**不要**再画一遍（会重影）。
 */
public final class EntityPageRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float DEFAULT_SCALE = 45.0F;
    private static final float YAW = -28.0F;
    private static final float PITCH = 0.0F;
    private static final Map<String, EntityRenderState> CACHE = new HashMap<>();
    private static final Set<String> FAILED = new HashSet<>();

    private EntityPageRenderer() {
    }

    /** 画实体，返回内容区新的 y（不画文字）。 */
    public static int render(GuiGraphicsExtractor graphics, BookPage page, int x, int y, int width) {
        EntityRenderState state = state(page.str("entity"));
        if (state == null) {
            return y;
        }
        float scale = DEFAULT_SCALE * page.decimal("scale", 1.0F);
        float offset = page.decimal("offset", 0.0F);
        int entityH = Math.max(32, Math.round(state.boundingBoxHeight * scale) + 8);
        int cx = x + width / 2;
        int centerY = y + entityH / 2;
        int half = Math.max(8, Math.round(scale / 2.0F));
        drawCentered(graphics, state, scale, cx - half, centerY - half, cx + half, centerY + half, offset);
        return y + entityH + 6;
    }

    /**
     * 在给定方块 (x0,y0)-(x1,y1) 内居中画一个已建好的实体状态（祭坛页复用）。
     * 实体的原点在方块中心、向上生长，所以 translate.y 要补上半个身高才正好居中。
     */
    public static void drawCentered(GuiGraphicsExtractor graphics, EntityRenderState state, float scale,
                                    int x0, int y0, int x1, int y1, float yOffset) {
        if (state == null) {
            return;
        }
        Vector3f translation = new Vector3f(0.0F, yOffset + state.boundingBoxHeight / 2.0F, 0.0F);
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        pose.mul(new Quaternionf().rotateY((float) Math.toRadians(YAW)).rotateX(PITCH));
        graphics.enableScissor(x0, y0, x1, y1);
        graphics.entity(state, scale, translation, pose, null, x0, y0, x1, y1);
        graphics.disableScissor();
    }

    public static EntityRenderState state(String spec) {
        if (spec == null || spec.isEmpty()) {
            return null;
        }
        EntityRenderState cached = CACHE.get(spec);
        if (cached != null) {
            return cached;
        }
        if (FAILED.contains(spec)) {
            // 已经失败过就不再尝试：每帧重建会把渲染线程拖死
            return null;
        }
        EntityRenderState built = build(spec);
        if (built != null) {
            CACHE.put(spec, built);
        } else {
            FAILED.add(spec);
        }
        return built;
    }

    private static EntityRenderState build(String spec) {
        try {
            Level level = Minecraft.getInstance().level;
            if (level == null) {
                return null;
            }
            int brace = spec.indexOf('{');
            String idPart = (brace < 0 ? spec : spec.substring(0, brace)).trim();
            EntityType<?> type = EntityTypeUtil.byString(idPart).orElse(null);
            if (type == null) {
                LOGGER.warn("[TLM Book] 实体页找不到实体类型: {}", idPart);
                return null;
            }
            Entity entity = type.create(level, EntitySpawnReason.LOAD);
            if (entity == null) {
                LOGGER.warn("[TLM Book] 实体页创建实体失败: {}", idPart);
                return null;
            }
            if (brace >= 0) {
                CompoundTag tag = TagParser.parseCompoundFully(spec.substring(brace));
                entity.load(TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), tag));
            }
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            EntityRenderer<? super Entity, ?> renderer = dispatcher.getRenderer(entity);
            EntityRenderState state = renderer.createRenderState(entity, 1.0F);
            state.shadowPieces.clear();
            state.outlineColor = 0;
            if (state instanceof LivingEntityRenderState living) {
                living.bodyRot = 180.0F + YAW;
                living.yRot = YAW;
                living.xRot = PITCH;
                living.boundingBoxWidth /= living.scale;
                living.boundingBoxHeight /= living.scale;
                living.scale = 1.0F;
            }
            state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
            return state;
        } catch (Exception e) {
            LOGGER.warn("[TLM Book] 实体页 {} 渲染状态创建失败，本页将不显示实体", spec, e);
            return null;
        }
    }
}
