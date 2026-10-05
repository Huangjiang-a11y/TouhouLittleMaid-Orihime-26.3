package com.github.tartaricacid.touhoulittlemaid.client.book;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 自包含书壳的加载器（不依赖 Patchouli）。
 * 全部走客户端资源管理器（assets 根）：
 *   <ns>:patchouli_books/&lt;book&gt;/book.json                      书定义（assets 里放了一份副本，data 里的那份给 Patchouli 用）
 *   <ns>:patchouli_books/&lt;book&gt;/&lt;lang&gt;/{categories,entries}  分类 / 条目
 * 注意：getResource / listResources 的 path 是「命名空间内的路径」，
 * 绝不能带 assets/ 或 data/ 前缀（带了会被当成命名空间，永远找不到 → 书空白）。
 */
public final class BookContentLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String FALLBACK_LANG = "en_us";

    private BookContentLoader() {
    }

    public static BookContent load(Identifier bookId) {
        return load(bookId, Minecraft.getInstance().getLanguageManager().getSelected());
    }

    public static BookContent load(Identifier bookId, String lang) {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        String namespace = bookId.getNamespace();
        String root = "patchouli_books/" + bookId.getPath() + "/";
        JsonObject definition = readJson(manager, Identifier.fromNamespaceAndPath(namespace, root + "book.json"));
        if (definition == null) {
            LOGGER.warn("[TLM Book] 找不到书定义 {}{}book.json", namespace, ":" + root);
            definition = new JsonObject();
        }
        String usedLang = lang;
        List<BookCategory> categories = loadCategories(manager, namespace, root, usedLang);
        List<BookEntry> entries = loadEntries(manager, namespace, root, usedLang);
        if (categories.isEmpty() && entries.isEmpty() && !FALLBACK_LANG.equals(usedLang)) {
            // 内容只放在 en_us 目录里，其它语言靠条目内的 lang key 本地化
            usedLang = FALLBACK_LANG;
            categories = loadCategories(manager, namespace, root, usedLang);
            entries = loadEntries(manager, namespace, root, usedLang);
        }
        LOGGER.info("[TLM Book] 载入 {} (lang={}) 分类 {} 个 / 条目 {} 个", bookId, usedLang, categories.size(), entries.size());
        return new BookContent(bookId.toString(), definition, categories, entries);
    }

    private static List<BookCategory> loadCategories(ResourceManager manager, String namespace, String root, String lang) {
        List<BookCategory> out = new ArrayList<>();
        for (Map.Entry<String, JsonObject> entry : readDir(manager, namespace, root + lang + "/categories").entrySet()) {
            out.add(BookCategory.fromJson(namespace + ":" + entry.getKey(), entry.getValue()));
        }
        return out;
    }

    private static List<BookEntry> loadEntries(ResourceManager manager, String namespace, String root, String lang) {
        List<BookEntry> out = new ArrayList<>();
        for (Map.Entry<String, JsonObject> entry : readDir(manager, namespace, root + lang + "/entries").entrySet()) {
            out.add(BookEntry.fromJson(namespace + ":" + entry.getKey(), entry.getValue()));
        }
        return out;
    }

    /** 读一个目录下所有 .json，返回 相对路径(不含 .json) -> json。 */
    private static Map<String, JsonObject> readDir(ResourceManager manager, String namespace, String dir) {
        Map<String, JsonObject> out = new LinkedHashMap<>();
        try {
            Map<Identifier, Resource> found = manager.listResources(dir,
                    id -> id.getNamespace().equals(namespace) && id.getPath().endsWith(".json"));
            for (Map.Entry<Identifier, Resource> entry : found.entrySet()) {
                JsonObject json = readJson(entry.getValue());
                if (json == null) {
                    continue;
                }
                String path = entry.getKey().getPath();
                String name = path.substring(dir.length() + 1, path.length() - ".json".length());
                out.put(name, json);
            }
        } catch (Exception e) {
            LOGGER.error("[TLM Book] 读取书目录失败: {}", dir, e);
        }
        return out;
    }

    private static JsonObject readJson(ResourceManager manager, Identifier id) {
        Optional<Resource> resource = manager.getResource(id);
        return resource.map(BookContentLoader::readJson).orElse(null);
    }

    private static JsonObject readJson(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.error("[TLM Book] 解析书 json 失败", e);
            return null;
        }
    }
}
