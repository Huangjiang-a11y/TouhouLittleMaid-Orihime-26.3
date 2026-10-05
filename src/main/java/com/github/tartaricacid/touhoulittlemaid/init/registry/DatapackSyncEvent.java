package com.github.tartaricacid.touhoulittlemaid.init.registry;

import com.github.tartaricacid.touhoulittlemaid.init.InitRecipes;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;

public class DatapackSyncEvent {
    public static void onDatapackSyncEvent() {
        // 只同步本模组自己的祭坛配方。
        // 手册 crafting 页要显示的原版合成配方**不走这里**：Fabric 的配方同步是按序列化器
        // 逐项 opt-in 的，登记原版 Shaped/Shapeless 等于把**所有模组**的合成配方都推给客户端
        // （登录时一次性传输，modpack 下可能 MB 级）。手册改用烤进资源的静态配方数据
        // （见 tools/gen_book_recipes.py 与 client/book/BookRecipeData）。
        RecipeSynchronization.synchronizeRecipeSerializer(InitRecipes.ALTAR_RECIPE_SERIALIZER);
    }
}
