package com.github.tartaricacid.touhoulittlemaid.client.renderer.item;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.VanillaConfig;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * 可切换贴图的物品渲染器：对应 1.21.1 的 ReplaceableBakedModel。
 * <p>
 * 26.3 没有烘焙期条件替换，改为在渲染时读 {@link VanillaConfig}，
 * 决定画 TLM 贴图（enabled）还是原版贴图（disabled）。
 */
public class ReplaceableSpriteSpecialRenderer implements SpecialModelRenderer<Boolean> {
    public static final Identifier ID = IdentifierUtil.modLoc("replaceable_sprite");

    private final SpriteSwitch spriteSwitch;
    private final Identifier enabledTexture;
    private final Identifier disabledTexture;

    public ReplaceableSpriteSpecialRenderer(SpriteSwitch spriteSwitch, Identifier enabledTexture, Identifier disabledTexture) {
        this.spriteSwitch = spriteSwitch;
        this.enabledTexture = enabledTexture;
        this.disabledTexture = disabledTexture;
    }

    @Override
    public Boolean extractArgument(ItemStack stack) {
        return Boolean.TRUE;
    }

    @Override
    public void submit(Boolean value, PoseStack poseStack, SubmitNodeCollector collector,
                       int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        Identifier texture = this.spriteSwitch.isEnabled() ? this.enabledTexture : this.disabledTexture;
        RenderType renderType = RenderTypes.itemCutout(texture);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            vertex(buffer, pose, -0.5F, 0.5F, 0.0F, 0.0F, lightCoords, overlayCoords);
            vertex(buffer, pose, 0.5F, 0.5F, 1.0F, 0.0F, lightCoords, overlayCoords);
            vertex(buffer, pose, 0.5F, -0.5F, 1.0F, 1.0F, lightCoords, overlayCoords);
            vertex(buffer, pose, -0.5F, -0.5F, 0.0F, 1.0F, lightCoords, overlayCoords);
        });
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(-0.5F, -0.5F, 0.0F));
        output.accept(new Vector3f(0.5F, 0.5F, 0.0625F));
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y,
                               float u, float v, int lightCoords, int overlayCoords) {
        buffer.addVertex(pose, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(overlayColor(overlayCoords))
                .setLight(lightCoords)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    private static int overlayColor(int overlayCoords) {
        return overlayCoords;
    }

    /**
     * 开关来源，对应 VanillaConfig 里的四个选项中的两个（另外两个是渲染器级别的，不走这里）
     */
    public enum SpriteSwitch {
        TOTEM("totem"),
        XP_BOTTLE("xp_bottle");

        public static final Codec<SpriteSwitch> CODEC = Codec.STRING.xmap(SpriteSwitch::byName, SpriteSwitch::getName);

        private final String name;

        SpriteSwitch(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static SpriteSwitch byName(String name) {
            for (SpriteSwitch value : values()) {
                if (value.name.equals(name)) {
                    return value;
                }
            }
            return TOTEM;
        }

        public boolean isEnabled() {
            return switch (this) {
                case TOTEM -> VanillaConfig.REPLACE_TOTEM_TEXTURE.get();
                case XP_BOTTLE -> VanillaConfig.REPLACE_XP_BOTTLE_TEXTURE.get();
            };
        }
    }

    public record Unbaked(SpriteSwitch spriteSwitch, Identifier enabled, Identifier disabled)
            implements SpecialModelRenderer.Unbaked<Boolean> {
        public static final MapCodec<ReplaceableSpriteSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        SpriteSwitch.CODEC.fieldOf("switch").forGetter(Unbaked::spriteSwitch),
                        Identifier.CODEC.fieldOf("enabled").forGetter(Unbaked::enabled),
                        Identifier.CODEC.fieldOf("disabled").forGetter(Unbaked::disabled)
                ).apply(instance, Unbaked::new));

        @Override
        public MapCodec<ReplaceableSpriteSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<Boolean> bake(SpecialModelRenderer.BakingContext context) {
            return new ReplaceableSpriteSpecialRenderer(this.spriteSwitch, this.enabled, this.disabled);
        }
    }
}
