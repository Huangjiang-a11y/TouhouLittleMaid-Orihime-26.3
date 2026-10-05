package com.github.tartaricacid.touhoulittlemaid.client.book;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 读贴图的真实像素尺寸（PNG 头里的 IHDR 宽高），带缓存。
 * 书里的插图尺寸不统一，必须按真实宽高等比缩放，否则会被当成正方形裁掉。
 */
public final class BookImages {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Identifier, int[]> CACHE = new HashMap<>();
    private static final int[] FALLBACK = {256, 256};

    private BookImages() {
    }

    /** @return {宽, 高}，读不到时退回 256x256。 */
    public static int[] size(Identifier id) {
        return CACHE.computeIfAbsent(id, BookImages::readSize);
    }

    private static int[] readSize(Identifier id) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);
        if (resource.isEmpty()) {
            return FALLBACK;
        }
        try (InputStream in = resource.get().open()) {
            byte[] head = in.readNBytes(24);
            boolean isPng = head.length >= 24 && (head[0] & 0xFF) == 0x89
                    && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
            if (isPng) {
                int w = readInt(head, 16);
                int h = readInt(head, 20);
                if (w > 0 && h > 0) {
                    return new int[]{w, h};
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[TLM Book] 读取图片尺寸失败: {}", id, e);
        }
        return FALLBACK;
    }

    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
    }
}
