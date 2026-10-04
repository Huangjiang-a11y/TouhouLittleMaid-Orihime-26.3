package com.github.tartaricacid.touhoulittlemaid.client.book;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 一整本书加载后的结果：book.json 原文 + 分类 + 条目。 */
public class BookContent {
    private final String id;
    private final JsonObject definition;
    private final List<BookCategory> categories;
    private final List<BookEntry> entries;

    public BookContent(String id, JsonObject definition, List<BookCategory> categories, List<BookEntry> entries) {
        this.id = id;
        this.definition = definition;
        this.categories = new ArrayList<>(categories);
        this.entries = new ArrayList<>(entries);
        this.categories.sort(Comparator.comparingInt(BookCategory::sortnum).thenComparing(BookCategory::id));
        this.entries.sort(Comparator.comparingInt(BookEntry::sortnum).thenComparing(BookEntry::id));
    }

    public String id() {
        return this.id;
    }

    public JsonObject definition() {
        return this.definition;
    }

    public List<BookCategory> categories() {
        return this.categories;
    }

    public List<BookEntry> entries() {
        return this.entries;
    }

    /** 某分类下的条目（按 sortnum 排好）。 */
    public List<BookEntry> entriesOf(String categoryId) {
        return this.entries.stream().filter(e -> e.category().equals(categoryId)).toList();
    }

    public Component name() {
        return BookPage.translate(this.definition.has("name") ? this.definition.get("name").getAsString() : this.id);
    }

    public Component landingText() {
        String key = this.definition.has("landing_text") ? this.definition.get("landing_text").getAsString() : "";
        return BookPage.translate(key);
    }
}
