package com.github.tartaricacid.touhoulittlemaid.client.book;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;

/** 分类（maid / other / overview）。 */
public record BookCategory(String id, String nameKey, String descKey, String icon, int sortnum) {
    public static BookCategory fromJson(String id, JsonObject json) {
        return new BookCategory(id,
                json.has("name") ? json.get("name").getAsString() : id,
                json.has("description") ? json.get("description").getAsString() : "",
                json.has("icon") ? json.get("icon").getAsString() : "",
                json.has("sortnum") ? json.get("sortnum").getAsInt() : 0);
    }

    public Component name() {
        return BookPage.translate(this.nameKey);
    }

    public Component description() {
        return BookPage.translate(this.descKey);
    }
}
