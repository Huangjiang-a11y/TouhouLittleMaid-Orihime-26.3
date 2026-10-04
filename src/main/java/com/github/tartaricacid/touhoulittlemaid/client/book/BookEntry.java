package com.github.tartaricacid.touhoulittlemaid.client.book;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** 条目。id 形如 touhou_little_maid:maid/backpack，category 指向分类。 */
public record BookEntry(String id, String category, String nameKey, String icon, int sortnum, List<BookPage> pages) {
    public static BookEntry fromJson(String id, JsonObject json) {
        List<BookPage> pages = new ArrayList<>();
        if (json.has("pages") && json.get("pages").isJsonArray()) {
            JsonArray array = json.getAsJsonArray("pages");
            array.forEach(e -> pages.add(BookPage.fromJson(e.getAsJsonObject())));
        }
        return new BookEntry(id,
                json.has("category") ? json.get("category").getAsString() : "",
                json.has("name") ? json.get("name").getAsString() : id,
                json.has("icon") ? json.get("icon").getAsString() : "",
                json.has("sortnum") ? json.get("sortnum").getAsInt() : 0,
                pages);
    }

    public Component name() {
        return BookPage.translate(this.nameKey);
    }
}
