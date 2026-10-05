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

    public static void onRecipeReceived(Minecraft client, SynchronizedRecipes recipes) {
        ALTAR_RECIPES = Lists.newArrayList(recipes.getAllOfType(InitRecipes.ALTAR_RECIPE));
    }

    /**
     * 按 id 查完整配方。
     * <p>
     * 注意：Fabric 的配方同步是按 RecipeSerializer 逐项 opt-in 的（本模组只同步了祭坛配方），
     * 而 26.3 的原版客户端也只收到「可合成物品集合」，拿不到完整配方表，
     * 所以这里只能向集成服务端（单机）要；多人游戏下返回 null。
     */
    public static RecipeHolder<?> findRecipe(Identifier id) {
        if (id == null || !Minecraft.getInstance().hasSingleplayerServer()) {
            return null;
        }
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return null;
        }
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
        return server.getRecipeManager().byKey(key).orElse(null);
    }
}