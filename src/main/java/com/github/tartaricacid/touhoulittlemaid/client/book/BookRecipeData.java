package com.github.tartaricacid.touhoulittlemaid.client.book;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 手册 crafting 页用的**静态配方数据**（由 tools/gen_book_recipes.py 生成）。
 * <p>
 * 为什么不向服务端要配方：26.3 客户端不再持有完整配方表，而 Fabric 的配方同步是按
 * RecipeSerializer 逐项 opt-in 的，照搬会把所有模组的合成配方都推给客户端（登录时一次性传输）。
 * 手册只是文档、配方又是本模组自己的，烤进资源里既零网络开销、也不受服务端影响，
 * 而且仍然走实时渲染，TLM 那些特殊 3D 模型物品（如 chair）的图标也不会画错。
 */
public final class BookRecipeData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier RESOURCE =
            Identifier.fromNamespaceAndPath("touhou_little_maid", "book_recipes.json");

    public record Entry(int width, int height, List<ItemStack> grid, ItemStack result) {
    }

    private static Map<String, Entry> cache;

    private BookRecipeData() {
    }

    /** 资源重载时清缓存。 */
    public static void invalidate() {
        cache = null;
    }

    public static Entry get(Identifier recipeId) {
        if (recipeId == null) {
            return null;
        }
        if (cache == null) {
            cache = load();
        }
        return cache.get(recipeId.toString());
    }

    private static Map<String, Entry> load() {
        Map<String, Entry> out = new HashMap<>();
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(RESOURCE);
        if (resource.isEmpty()) {
            LOGGER.warn("[TLM Book] 找不到静态配方数据 {}", RESOURCE);
            return out;
        }
        try (Reader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> element : root.entrySet()) {
                JsonObject json = element.getValue().getAsJsonObject();
                List<ItemStack> grid = new ArrayList<>();
                for (JsonElement cell : json.getAsJsonArray("grid")) {
                    grid.add(stack(cell.getAsString()));
                }
                out.put(element.getKey(), new Entry(
                        json.get("width").getAsInt(),
                        json.get("height").getAsInt(),
                        grid,
                        stack(json.get("result").getAsString())));
            }
            LOGGER.info("[TLM Book] 载入静态配方数据 {} 个", out.size());
        } catch (Exception e) {
            LOGGER.error("[TLM Book] 解析 {} 失败", RESOURCE, e);
        }
        return out;
    }

    private static ItemStack stack(String id) {
        if (id == null || id.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Identifier location = Identifier.tryParse(id);
        return location == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.getValue(location));
    }
}
