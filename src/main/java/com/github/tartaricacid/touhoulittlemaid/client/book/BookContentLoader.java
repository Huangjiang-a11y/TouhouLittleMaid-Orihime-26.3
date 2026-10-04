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
 * 按 Patchouli 的约定读两处：
 *   data/<ns>/patchouli_books/<book>/book.json      书定义（数据包）
 *   assets/<ns>/patchouli_books/<book>/<lang>/...   分类 / 条目 / 模板（客户端资源）
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
        String book = bookId.getPath();
        JsonObject definition = readJson(manager,
                "data/" + namespace + "/patchouli_books/" + book + "/book.json",
                "assets/" + namespace + "/patchouli_books/" + book + "/book.json");
        if (definition == null) {
            definition = new JsonObject();
        }
        List<BookCategory> categories = loadCategories(manager, namespace, book, lang);
        List<BookEntry> entries = loadEntries(manager, namespace, book, lang);
        if (categories.isEmpty() && entries.isEmpty() && !FALLBACK_LANG.equals(lang)) {
            // 内容只在 en_us 目录里，其余语言靠 lang key 本地化
            categories = loadCategories(manager, namespace, book, FALLBACK_LANG);
            entries = loadEntries(manager, namespace, book, FALLBACK_LANG);
        }
        return new BookContent(bookId.toString(), definition, categories, entries);
    }

    private static List<BookCategory> loadCategories(ResourceManager manager, String namespace, String book, String lang) {
        List<BookCategory> out = new ArrayList<>();
        for (Map.Entry<String, JsonObject> entry : readDir(manager, dir(namespace, book, lang, "categories")).entrySet()) {
            out.add(BookCategory.fromJson(namespace + ":" + entry.getKey(), entry.getValue()));
        }
        return out;
    }

    private static List<BookEntry> loadEntries(ResourceManager manager, String namespace, String book, String lang) {
        List<BookEntry> out = new ArrayList<>();
        for (Map.Entry<String, JsonObject> entry : readDir(manager, dir(namespace, book, lang, "entries")).entrySet()) {
            out.add(BookEntry.fromJson(namespace + ":" + entry.getKey(), entry.getValue()));
        }
        return out;
    }

    private static String dir(String namespace, String book, String lang, String kind) {
        return "assets/" + namespace + "/patchouli_books/" + book + "/" + lang + "/" + kind;
    }

    /** 读一个目录下所有 .json，返回 相对路径(不含 .json) -> json。 */
    private static Map<String, JsonObject> readDir(ResourceManager manager, String dir) {
        Map<String, JsonObject> out = new LinkedHashMap<>();
        try {
            Map<Identifier, Resource> found = manager.listResources(dir, id -> id.getPath().endsWith(".json"));
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

    private static JsonObject readJson(ResourceManager manager, String... candidates) {
        for (String candidate : candidates) {
            Identifier id = Identifier.tryParse(candidate);
            if (id == null) {
                continue;
            }
            Optional<Resource> resource = manager.getResource(id);
            if (resource.isPresent()) {
                JsonObject json = readJson(resource.get());
                if (json != null) {
                    return json;
                }
            }
        }
        return null;
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
