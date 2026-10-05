package com.github.tartaricacid.touhoulittlemaid.client.book;

import com.github.tartaricacid.touhoulittlemaid.client.event.ClientRecipeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * crafting 页：合成配方（3x3 网格 + 箭头 + 产物），支持 recipe 与 recipe2 两个配方。
 * <p>
 * 数据来源说明：26.3 的客户端只从 {@code ClientboundUpdateRecipesPacket} 收到「可合成物品集合」，
 * 不再持有完整配方表，所以这里只能向集成服务端的 RecipeManager 要；多人游戏拿不到 → 退化为显示配方 id。
 */
public final class CraftingPageRenderer {
    private static final int SLOT = 18;
    private static final int ARROW_W = 26;
    private static final int SLOT_BORDER = 0xFF373737;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int ARROW_COLOR = 0xFF505050;

    private CraftingPageRenderer() {
    }

    public static int render(GuiGraphicsExtractor graphics, BookPage page, int x, int y, int width,
                             Font font, int textColor) {
        int cy = y;
        String recipe = page.str("recipe");
        if (!recipe.isEmpty()) {
            cy = renderOne(graphics, recipe, x, cy, width, font, textColor);
        }
        String recipe2 = page.str("recipe2");
        if (!recipe2.isEmpty()) {
            cy = renderOne(graphics, recipe2, x, cy + 6, width, font, textColor);
        }
        if (page.has("text")) {
            BookRichText body = BookRichText.of(page.plain("text"), textColor, font, width);
            body.render(graphics, font, x, cy + 6, null);
            cy += body.height() + 6;
        }
        return cy;
    }

    private static int renderOne(GuiGraphicsExtractor graphics, String idStr, int x, int y, int width,
                                 Font font, int textColor) {
        Level level = Minecraft.getInstance().level;
        Identifier id = Identifier.tryParse(idStr);
        RecipeHolder<?> holder = (level == null || id == null) ? null : ClientRecipeEvent.findRecipe(id);
        if (holder == null) {
            graphics.text(font, Component.literal("<recipe " + idStr + ">"), x, y + 2, textColor, false);
            return y + 16;
        }
        ContextMap context = SlotDisplayContext.fromLevel(level);
        int cy = y;
        for (RecipeDisplay display : holder.value().display()) {
            cy = draw(graphics, display, context, x, cy, width, font);
        }
        return cy;
    }

    private static int draw(GuiGraphicsExtractor graphics, RecipeDisplay display, ContextMap context,
                            int x, int y, int width, Font font) {
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            return drawGrid(graphics, shaped.width(), shaped.height(), shaped.ingredients(),
                    shaped.result(), context, x, y, width, font);
        }
        if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            List<SlotDisplay> ingredients = shapeless.ingredients();
            int cols = Math.min(3, Math.max(1, ingredients.size()));
            int rows = (ingredients.size() + cols - 1) / cols;
            return drawGrid(graphics, cols, rows, ingredients, shapeless.result(), context, x, y, width, font);
        }
        return y;
    }

    private static int drawGrid(GuiGraphicsExtractor graphics, int cols, int rows, List<SlotDisplay> ingredients,
                                SlotDisplay result, ContextMap context, int x, int y, int width, Font font) {
        int gridW = cols * SLOT;
        int gridH = rows * SLOT;
        int totalW = gridW + ARROW_W + SLOT;
        int gx = x + Math.max(0, (width - totalW) / 2);
        int gy = y + 4;

        for (int i = 0; i < cols * rows; i++) {
            int sx = gx + (i % cols) * SLOT;
            int sy = gy + (i / cols) * SLOT;
            drawSlot(graphics, sx, sy);
            if (i < ingredients.size()) {
                drawStack(graphics, font, firstStack(ingredients.get(i), context), sx, sy);
            }
        }

        int arrowY = gy + gridH / 2;
        drawArrow(graphics, gx + gridW + 4, arrowY);

        int rx = gx + gridW + ARROW_W;
        int ry = gy + (gridH - SLOT) / 2;
        drawSlot(graphics, rx, ry);
        drawStack(graphics, font, firstStack(result, context), rx, ry);

        return gy + gridH + 4;
    }

    private static void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT, y + SLOT, SLOT_BORDER);
        graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, SLOT_FILL);
    }

    private static void drawStack(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }
        graphics.item(stack, x + 1, y + 1);
        graphics.itemDecorations(font, stack, x + 1, y + 1);
    }

    /** 手画一个右向箭头：横杠 + 三角头。 */
    private static void drawArrow(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y - 1, x + 10, y + 1, ARROW_COLOR);
        for (int i = 0; i < 5; i++) {
            graphics.fill(x + 10 + i, y - (4 - i), x + 11 + i, y + (4 - i) + 1, ARROW_COLOR);
        }
    }

    private static ItemStack firstStack(SlotDisplay display, ContextMap context) {
        if (display == null) {
            return ItemStack.EMPTY;
        }
        try {
            for (ItemStack stack : display.resolveForStacks(context)) {
                if (!stack.isEmpty()) {
                    return stack;
                }
            }
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }
}
