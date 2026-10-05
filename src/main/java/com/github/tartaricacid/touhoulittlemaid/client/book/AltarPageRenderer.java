package com.github.tartaricacid.touhoulittlemaid.client.book;

import com.github.tartaricacid.touhoulittlemaid.client.event.ClientRecipeEvent;
import com.github.tartaricacid.touhoulittlemaid.crafting.AltarRecipe;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityBox;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.GuiTools;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 祭坛配方页。
 * <p>
 * 版面**严格照 Patchouli 模板** {@code en_us/templates/altar_recipe.json} 来（虚拟页面 116x156）：
 * 顶部 #output_desc 标题 + 分隔线 → 6 个输入位（13/65/40/40/65/90 那个左右两列 + 上排的摆法）
 * → 正中 power_point 图标与 #power_cost → 底部自绘箭头 → #output_item（物品产物）
 * 或 #output_entity（实体产物，如生成女仆）。
 * <p>
 * 数据来源等同 Patchouli 的 {@code AltarRecipeComponent}：{@code ClientRecipeEvent.ALTAR_RECIPES}。
 */
public final class AltarPageRenderer {
    /** 虚拟页面尺寸，与 Patchouli 页面一致。 */
    private static final int V_W = 116;
    private static final int V_H = 156;
    /** 6 个输入位（模板的 input1..input6）。 */
    private static final int[][] INPUTS = {
            {13, 90}, {13, 65}, {38, 40}, {63, 40}, {88, 65}, {88, 90}
    };
    private static final Identifier POWER_POINT = IdentifierUtil.modLoc("textures/item/power_point.png");
    private static final int TEXT_COLOR = 0xFF000000;
    private static final int POWER_COLOR = 0xFF777777;
    private static final int SLOT_LIGHT = 0xFFB4B4B4;
    private static final int SLOT_DARK = 0xFF8B8B8B;
    private static final int ARROW_COLOR = 0xFF6E6E6E;

    private AltarPageRenderer() {
    }

    public static void render(GuiGraphicsExtractor graphics, BookPage page, int x, int y, int width,
                              Font font, int textColor) {
        Identifier recipeId = Identifier.tryParse(page.str("recipe_id"));
        AltarRecipe recipe = recipeId == null ? null : find(recipeId);
        if (recipe == null) {
            graphics.text(font, Component.literal("<altar recipe: " + page.str("recipe_id") + ">"), x, y, textColor, false);
            return;
        }
        // 虚拟页面整体居中，按 1:1 画（内容区宽 254、高约 158，装得下 116x156）
        int ox = x + (width - V_W) / 2;
        int oy = y;

        // 顶部标题（模板 header #output_desc）+ 分隔线
        String descKey = recipe.getLangKey();
        Component desc = descKey == null || descKey.isEmpty() ? Component.empty() : BookPage.translate(descKey);
        graphics.centeredText(font, desc, ox + V_W / 2, oy + 5, TEXT_COLOR);
        graphics.fill(ox, oy + 20, ox + V_W, oy + 21, 0x33000000);

        // 6 个输入位
        List<Ingredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < INPUTS.length; i++) {
            ItemStack stack = i < ingredients.size() ? firstStack(ingredients.get(i)) : ItemStack.EMPTY;
            drawSlot(graphics, font, stack, ox + INPUTS[i][0], oy + INPUTS[i][1]);
        }

        // 正中：power_point 图标 + power 消耗
        GuiTools.guiBlit(graphics, POWER_POINT, ox + 52, oy + 74, 0, 0, 16, 16, 16, 16);
        String power = String.format("x%.2f", recipe.getPower());
        graphics.text(font, Component.literal(power), ox + 57, oy + 89, POWER_COLOR, false);

        // 底部：箭头 → 产物
        drawArrow(graphics, ox + 35, oy + 124);
        if (recipe.isItemCraft()) {
            drawSlot(graphics, font, recipe.getResult().create(), ox + 50, oy + 120);
        } else {
            // 实体产物（生成女仆等）：留一个稍大的框把实体画出来，16px 的框根本看不清
            Identifier entityId = recipe.getEntityType();
            // 特判：生成女仆实际产物是盒子，这里纠正为女仆（同 AltarRecipeComponent）
            if (EntityBox.ENTITY_ID.equals(entityId)) {
                entityId = EntityMaid.ENTITY_ID;
            }
            EntityRenderState state = EntityPageRenderer.state(entityId == null ? "" : entityId.toString());
            if (state != null) {
                int bx = ox + 44;
                int by = oy + 108;
                graphics.fill(bx - 1, by - 1, bx + 42, by + 42, SLOT_LIGHT);
                graphics.fill(bx, by, bx + 41, by + 41, SLOT_DARK);
                EntityPageRenderer.drawCentered(graphics, state, 30.0F, bx + 20, by + 21);
            }
        }
    }

    /** 画一个「带框的物品位」，空格子只画框。 */
    private static void drawSlot(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_LIGHT);
        graphics.fill(x, y, x + 16, y + 16, SLOT_DARK);
        if (!stack.isEmpty()) {
            graphics.item(stack, x, y);
            graphics.itemDecorations(font, stack, x, y);
        }
    }

    /** 自绘一个小箭头（模板用的是 Patchouli 内置贴图 patchouli:textures/gui/crafting.png，没装 Patchouli 拿不到）。 */
    private static void drawArrow(GuiGraphicsExtractor graphics, int x, int y) {
        for (int i = 0; i < 9; i++) {
            int w = i <= 4 ? i + 1 : 9 - i;
            graphics.fill(x + 3, y + i, x + 3 + w, y + i + 1, ARROW_COLOR);
        }
        graphics.fill(x, y + 4, x + 3, y + 5, ARROW_COLOR);
    }

    private static ItemStack firstStack(Ingredient ingredient) {
        if (ingredient.isEmpty()) {
            return ItemStack.EMPTY;
        }
        for (Holder<Item> holder : ingredient.values) {
            return new ItemStack(holder.value());
        }
        return ItemStack.EMPTY;
    }

    private static @Nullable AltarRecipe find(Identifier recipeId) {
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, recipeId);
        for (RecipeHolder<AltarRecipe> holder : ClientRecipeEvent.ALTAR_RECIPES) {
            if (holder.id().equals(key)) {
                return holder.value();
            }
        }
        return null;
    }
}
