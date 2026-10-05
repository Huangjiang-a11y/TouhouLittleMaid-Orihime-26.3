package com.github.tartaricacid.touhoulittlemaid.client.book;

import com.github.tartaricacid.touhoulittlemaid.util.migrate.EntityTypeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * entity 页：页内渲染一个实体，如 {@code touhou_little_maid:fairy{OnGround:true}}。
 * 渲染状态建好后缓存，避免每帧重建实体。
 */
public final class EntityPageRenderer {
    private static final int BOX_HEIGHT = 92;
    private static final float DEFAULT_SCALE = 45.0F;
    private static final float YAW = -28.0F;
    private static final Map<String, EntityRenderState> CACHE = new HashMap<>();

    private EntityPageRenderer() {
    }

    public static int render(GuiGraphicsExtractor graphics, BookPage page, int x, int y, int width,
                             Font font, int textColor, List<BookRichText.ClickRegion> clicks) {
        EntityRenderState state = state(page.str("entity"));
        int cy = y;
        if (state != null) {
            float scale = DEFAULT_SCALE * page.decimal("scale", 1.0F);
            float offset = page.decimal("offset", 0.0F);
            Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
            pose.mul(new Quaternionf().rotateY((float) Math.toRadians(YAW)));
            int box = Math.min(width, 120);
            int cx = x + width / 2;
            graphics.entity(state, scale, new Vector3f(0.0F, offset, 0.0F), pose, null,
                    cx - box / 2, cy, cx + box / 2, cy + BOX_HEIGHT);
            cy += BOX_HEIGHT + 4;
        }
        if (page.has("text")) {
            BookRichText body = BookRichText.of(page.plain("text"), textColor, font, width);
            body.render(graphics, font, x, cy, clicks);
            cy += body.height();
        }
        return cy;
    }

    public static EntityRenderState state(String spec) {
        if (spec == null || spec.isEmpty()) {
            return null;
        }
        EntityRenderState cached = CACHE.get(spec);
        if (cached != null) {
            return cached;
        }
        EntityRenderState built = build(spec);
        if (built != null) {
            CACHE.put(spec, built);
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
                return null;
            }
            Entity entity = type.create(level, EntitySpawnReason.LOAD);
            if (entity == null) {
                return null;
            }
            if (brace >= 0) {
                CompoundTag tag = TagParser.parseCompoundFully(spec.substring(brace));
                entity.load(TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), tag));
            }
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            EntityRenderState state = dispatcher.extractEntity(entity, 0.0F);
            state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
            state.shadowPieces.clear();
            return state;
        } catch (Exception e) {
            return null;
        }
    }
}
