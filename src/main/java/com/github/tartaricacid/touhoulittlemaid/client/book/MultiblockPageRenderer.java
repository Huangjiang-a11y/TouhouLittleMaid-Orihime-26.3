package com.github.tartaricacid.touhoulittlemaid.client.book;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * multiblock 页：祭坛多方块的等轴测预览（纯 2D 扫描线绘制，不加载区块）。
 * <p>
 * 结构表与 {@code compat/patchouli/MultiblockRegistry} 里的 Patchouli 模板一致，
 * 且 <b>layer[0] 是顶层</b>（用 data/structure/altar_*.nbt 的真实坐标校对过：模板第 3 层是御柱、
 * 第 0/2 层是鸟居横梁，对应结构 NBT 的 y=0..2 柱子 / y=3,5 横梁，即 layer i 对应 y = 5 - i）。
 * <p>
 * 方块颜色取 MapColor（原木=棕、红色羊毛=红），与 Patchouli 用真实方块 3D 渲染的观感接近。
 * 点击预览可 90° 旋转。
 */
public final class MultiblockPageRenderer {
    /** O=御柱(原木)，R=鸟居(红色羊毛)，空格=空气。 */
    private static final String[][] LAYERS = {
            {"        ", "       R", "       R", "       R", "       R", "       R", "       R", "        "},
            {"        ", "        ", "       R", "        ", "        ", "       R", "        ", "        "},
            {"        ", "       R", "       R", "       R", "       R", "       R", "       R", "        "},
            {"  O  O  ", "        ", "O      R", "        ", "        ", "O      R", "        ", "  O  O  "},
            {"  O  O  ", "        ", "O      R", "        ", "        ", "O      R", "        ", "  O  O  "},
            {"  O  O  ", "        ", "O      R", "        ", "        ", "O      R", "        ", "  O  O  "}
    };
    private static final int SIZE_X = 8;
    private static final int SIZE_Z = 8;
    private static final int HALF_W = 8;
    private static final int HALF_H = 4;
    private static final int BLOCK_H = 8;

    private static int rotation;
    private static int[] hitBox;

    private MultiblockPageRenderer() {
    }

    public static void resetRotation() {
        rotation = 0;
    }

    public static void rotate() {
        rotation = (rotation + 1) & 3;
    }

    public static int[] hitBox() {
        return hitBox;
    }

    public static void clearHitBox() {
        hitBox = null;
    }

    public static int render(GuiGraphicsExtractor graphics, BookPage page, int x, int y, int width,
                             Font font, int textColor) {
        int pillar = mapColor(Blocks.OAK_LOG.defaultBlockState(), 0xFF9A6B3F);
        int torii = mapColor(Blocks.WOOL.red().defaultBlockState(), 0xFFB02E26);

        // 收集方块 + 旋转 + 投影
        List<int[]> cells = new ArrayList<>();
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int layer = 0; layer < LAYERS.length; layer++) {
            int by = LAYERS.length - 1 - layer;
            for (int row = 0; row < LAYERS[layer].length; row++) {
                String line = LAYERS[layer][row];
                for (int col = 0; col < line.length(); col++) {
                    char c = line.charAt(col);
                    if (c == ' ') {
                        continue;
                    }
                    int rx = col;
                    int rz = row;
                    for (int i = 0; i < rotation; i++) {
                        int nx = rz;
                        int nz = SIZE_X - 1 - rx;
                        rx = nx;
                        rz = nz;
                    }
                    int px = (rx - rz) * HALF_W;
                    int py = (rx + rz) * HALF_H - by * BLOCK_H;
                    minX = Math.min(minX, px - HALF_W);
                    maxX = Math.max(maxX, px + HALF_W);
                    minY = Math.min(minY, py - HALF_H);
                    maxY = Math.max(maxY, py + HALF_H + BLOCK_H);
                    cells.add(new int[]{px, py, rx + rz, by, c == 'R' ? torii : pillar});
                }
            }
        }
        if (cells.isEmpty()) {
            return y;
        }

        int previewW = maxX - minX;
        int previewH = maxY - minY;
        int originX = x + (width - previewW) / 2 - minX;
        int originY = y + 6 - minY;

        // 背景板
        graphics.fill(x, y, x + width, y + previewH + 12, 0x14000000);

        // 远 → 近：先画 (rx+rz) 小的，再画 y 小的
        cells.sort(Comparator.<int[]>comparingInt(c -> c[2]).thenComparingInt(c -> c[3]));

        for (int[] cell : cells) {
            int cx = originX + cell[0];
            int cy = originY + cell[1];
            int base = cell[4];
            drawTop(graphics, cx, cy, shade(base, 1.0F));
            drawLeft(graphics, cx, cy, shade(base, 0.78F));
            drawRight(graphics, cx, cy, shade(base, 0.58F));
        }

        hitBox = new int[]{x, y, x + width, y + previewH + 12};

        int cy = y + previewH + 18;
        if (page.has("text")) {
            BookRichText body = BookRichText.of(page.plain("text"), textColor, font, width);
            body.render(graphics, font, x, cy, null);
            cy += body.height();
        }
        return cy;
    }

    private static void drawTop(GuiGraphicsExtractor graphics, int cx, int cy, int color) {
        fillQuad(graphics,
                new float[]{cx, cx + HALF_W, cx, cx - HALF_W},
                new float[]{cy - HALF_H, cy, cy + HALF_H, cy},
                color);
    }

    private static void drawLeft(GuiGraphicsExtractor graphics, int cx, int cy, int color) {
        fillQuad(graphics,
                new float[]{cx - HALF_W, cx, cx, cx - HALF_W},
                new float[]{cy, cy + HALF_H, cy + HALF_H + BLOCK_H, cy + BLOCK_H},
                color);
    }

    private static void drawRight(GuiGraphicsExtractor graphics, int cx, int cy, int color) {
        fillQuad(graphics,
                new float[]{cx, cx + HALF_W, cx + HALF_W, cx},
                new float[]{cy + HALF_H, cy, cy + BLOCK_H, cy + HALF_H + BLOCK_H},
                color);
    }

    /** 扫描线填充任意凸四边形。 */
    private static void fillQuad(GuiGraphicsExtractor graphics, float[] xs, float[] ys, int color) {
        float minY = Math.min(Math.min(ys[0], ys[1]), Math.min(ys[2], ys[3]));
        float maxY = Math.max(Math.max(ys[0], ys[1]), Math.max(ys[2], ys[3]));
        int y0 = (int) Math.floor(minY);
        int y1 = (int) Math.ceil(maxY);
        for (int yy = y0; yy < y1; yy++) {
            float sample = yy + 0.5F;
            float lo = Float.MAX_VALUE;
            float hi = -Float.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) & 3;
                float ay = ys[i];
                float by = ys[j];
                if ((ay <= sample && by > sample) || (by <= sample && ay > sample)) {
                    float t = (sample - ay) / (by - ay);
                    float xx = xs[i] + t * (xs[j] - xs[i]);
                    lo = Math.min(lo, xx);
                    hi = Math.max(hi, xx);
                }
            }
            if (hi > lo) {
                graphics.fill((int) Math.floor(lo), yy, (int) Math.ceil(hi), yy + 1, color);
            }
        }
    }

    private static int shade(int color, float factor) {
        int r = Math.min(255, Math.round(((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, Math.round(((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, Math.round((color & 0xFF) * factor));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int mapColor(BlockState state, int fallback) {
        try {
            Level level = Minecraft.getInstance().level;
            if (level != null) {
                return 0xFF000000 | state.getMapColor(level, BlockPos.ZERO).col;
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
