package com.github.tartaricacid.touhoulittlemaid.client.renderer.texture;

import com.github.tartaricacid.touhoulittlemaid.api.client.decoder.GifDecoder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TickableTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * gif 表情动图。
 * <p>
 * 26.x 移植时把原来那份 {@code GifTexture} 删了（当时 {@code SizeTexture}/{@code Tickable} 那套 API 不在了），
 * 于是 gif 表情只会去 blit 一个根本没有注册的纹理。这里按 26.3 的 API 重写：
 * {@link DynamicTexture} + {@link TickableTexture}（由 {@code TextureManager#tick} 每 tick 驱动），
 * 帧数据依旧交给仓库里现成的 {@link GifDecoder} 解码。
 */
public class GifTexture extends DynamicTexture implements TickableTexture {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** 已注册过的 gif（TextureManager 里 {@code byPath} 字段 26.3 已不公开，这里自己记一份）。 */
    private static final Set<Identifier> REGISTERED = new HashSet<>();

    private final Identifier texturePath;
    private NativeImage[] frames;
    private int[] frameDelays;
    private int currentFrame = 0;
    private int currentFrameDelay = 0;

    private GifTexture(Identifier texturePath, NativeImage firstFrame) {
        super(texturePath::toString, firstFrame);
        this.texturePath = texturePath;
    }

    /**
     * 把 gif 注册成动态纹理（可重复调用，已注册过的会被忽略）。
     * 解码与纹理上传都必须在渲染线程做，不在渲染线程时交给 {@link Minecraft#execute} 排队。
     */
    public static void register(Identifier texturePath) {
        if (REGISTERED.contains(texturePath)) {
            return;
        }
        REGISTERED.add(texturePath);
        if (RenderSystem.isOnRenderThread()) {
            doRegister(texturePath);
        } else {
            Minecraft.getInstance().execute(() -> doRegister(texturePath));
        }
    }

    private static void doRegister(Identifier texturePath) {
        try (InputStream stream = Minecraft.getInstance().getResourceManager().open(texturePath)) {
            GifDecoder decoder = new GifDecoder();
            if (decoder.read(stream) != GifDecoder.STATUS_OK) {
                LOGGER.warn("[TLM] gif 表情解码失败: {}", texturePath);
                return;
            }
            int total = decoder.getFrameCount();
            Dimension size = decoder.getFrameSize();
            NativeImage[] frames = new NativeImage[total];
            int[] delays = new int[total];
            for (int i = 0; i < total; i++) {
                frames[i] = toNativeImage(decoder.getFrame(i), size.width, size.height);
                // gif 的 delay 单位是 ms，原版按 50ms 一 tick 走
                delays[i] = Math.max(decoder.getDelay(i) / 50, 1);
            }
            GifTexture texture = new GifTexture(texturePath, frames[0]);
            texture.frames = frames;
            texture.frameDelays = delays;
            Minecraft.getInstance().getTextureManager().register(texturePath, texture);
        } catch (Exception e) {
            LOGGER.error("[TLM] gif 表情加载失败: {}", texturePath, e);
        }
    }

    private static NativeImage toNativeImage(BufferedImage image, int width, int height) {
        NativeImage nativeImage = new NativeImage(width, height, true);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = y < image.getHeight() && x < image.getWidth() ? image.getRGB(x, y) : 0;
                // BufferedImage#getRGB 就是 ARGB，NativeImage#setPixel 也吃 ARGB（内部转 ABGR）
                nativeImage.setPixel(x, y, argb);
            }
        }
        return nativeImage;
    }

    @Override
    public void tick() {
        if (this.frames == null || this.frames.length == 0) {
            return;
        }
        if (++this.currentFrameDelay < this.frameDelays[this.currentFrame]) {
            return;
        }
        this.currentFrameDelay = 0;
        this.currentFrame = (this.currentFrame + 1) % this.frames.length;
        this.setPixels(this.frames[this.currentFrame]);
        this.upload();
    }

    @Override
    public void close() {
        super.close();
        if (this.frames != null) {
            for (NativeImage frame : this.frames) {
                if (frame != null) {
                    frame.close();
                }
            }
            this.frames = null;
        }
    }
}
