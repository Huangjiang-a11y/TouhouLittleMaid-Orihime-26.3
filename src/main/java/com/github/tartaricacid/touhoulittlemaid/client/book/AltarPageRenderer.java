package com.github.tartaricacid.touhoulittlemaid.client.book;

import com.github.tartaricacid.touhoulittlemaid.client.event.ClientRecipeEvent;
import com.github.tartaricacid.touhoulittlemaid.crafting.AltarRecipe;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 祭坛配方页（43 页全同布局）。
 * 数据来源等同 Patchouli 的 AltarRecipeComponent：ClientRecipeEvent.ALTAR_RECIPES。
 */
public final class AltarPageRenderer {
    /** 3x3 外圈 8 个输入位。 */
    private static final int[][] OFFSETS = {
            {-18, -18}, {0, -18}, {18, -18},
            {-18, 0}, {18, 0},
            {-18, 18}, {0, 18}, {18, 18}
    };

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

        int centerX = x + width / 2 - 9;
        int centerY = y + 18;
        List<Ingredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < OFFSETS.length; i++) {
            int slotX = centerX + OFFSETS[i][0];
            int slotY = centerY + OFFSETS[i][1];
            ItemStack stack = i < ingredients.size() ? firstStack(ingredients.get(i)) : ItemStack.EMPTY;
            if (stack.isEmpty()) {
                continue;
            }
            graphics.item(stack, slotX, slotY);
            graphics.itemDecorations(font, stack, slotX, slotY);
        }

        ItemStack output = recipe.getResult().create();
        graphics.item(output, centerX, centerY);
        graphics.itemDecorations(font, output, centerX, centerY);

        String power = String.format("x%.2f", recipe.getPower());
        graphics.text(font, Component.literal(power), x + width / 2 - font.width(power) / 2, centerY + 22, textColor, false);

        String descKey = recipe.getLangKey();
        if (descKey != null && !descKey.isEmpty()) {
            MultiLineLabel label = MultiLineLabel.create(font, BookPage.translate(descKey), width);
            label.visitLines(TextAlignment.LEFT, x, centerY + 34, 9, graphics.textRenderer());
        }
    }

    private static ItemStack firstStack(Ingredient ingredient) {
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
