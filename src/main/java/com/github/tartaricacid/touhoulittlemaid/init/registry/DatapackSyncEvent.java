package com.github.tartaricacid.touhoulittlemaid.init.registry;

import com.github.tartaricacid.touhoulittlemaid.init.InitRecipes;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public class DatapackSyncEvent {
    public static void onDatapackSyncEvent() {
        RecipeSynchronization.synchronizeRecipeSerializer(InitRecipes.ALTAR_RECIPE_SERIALIZER);
        // 26.3 的客户端不再随 ClientboundUpdateRecipesPacket 收到完整配方表
        //（该包只带"可合成物品集合"与切石机配方），而 Fabric 的配方同步是**按序列化器逐项 opt-in** 的。
        // 手册的 crafting 页要显示合成配方，所以这里把原版合成序列化器也登记上，
        // 否则多人游戏下客户端没有配方数据，只能退化成显示配方 id。
        RecipeSynchronization.synchronizeRecipeSerializer(ShapedRecipe.SERIALIZER);
        RecipeSynchronization.synchronizeRecipeSerializer(ShapelessRecipe.SERIALIZER);
    }
}
