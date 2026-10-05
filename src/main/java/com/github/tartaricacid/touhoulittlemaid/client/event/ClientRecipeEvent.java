package com.github.tartaricacid.touhoulittlemaid.client.event;


import com.github.tartaricacid.touhoulittlemaid.crafting.AltarRecipe;
import com.github.tartaricacid.touhoulittlemaid.init.InitRecipes;
import com.google.common.collect.Lists;
import net.fabricmc.fabric.api.recipe.v1.sync.SynchronizedRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Collections;
import java.util.List;

public class ClientRecipeEvent {
    public static List<RecipeHolder<AltarRecipe>> ALTAR_RECIPES = Collections.emptyList();

    /** Fabric 同步过来的配方（含祭坛配方 + 原版合成配方，见 DatapackSyncEvent）。 */
    private static volatile SynchronizedRecipes syncedRecipes;

    public static void onRecipeReceived(Minecraft client, SynchronizedRecipes recipes) {
        ALTAR_RECIPES = Lists.newArrayList(recipes.getAllOfType(InitRecipes.ALTAR_RECIPE));
        syncedRecipes = recipes;
    }

    /**
     * 按 id 查完整配方（单机 / 多人通用）。
     * <p>
     * 26.3 的原版客户端只从 {@code ClientboundUpdateRecipesPacket} 收到「可合成物品集合」，
     * 拿不到完整配方表，所以走 Fabric 按序列化器同步下来的 {@link SynchronizedRecipes}
     * （见 {@code DatapackSyncEvent} 里 opt-in 的序列化器）。
     * 万一同步里没有（例如服务端未装/版本不符），单机再退回集成服务端查询。
     */
    public static RecipeHolder<?> findRecipe(Identifier id) {
        if (id == null) {
            return null;
        }
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
        SynchronizedRecipes recipes = syncedRecipes;
        if (recipes != null) {
            RecipeHolder<?> holder = recipes.get(key);
            if (holder != null) {
                return holder;
            }
        }
        MinecraftServer server = Minecraft.getInstance().hasSingleplayerServer()
                ? Minecraft.getInstance().getSingleplayerServer() : null;
        return server == null ? null : server.getRecipeManager().byKey(key).orElse(null);
    }
}