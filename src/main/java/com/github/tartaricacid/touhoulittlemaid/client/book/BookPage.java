package com.github.tartaricacid.touhoulittlemaid.client.book;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** 书里的一页：type 决定用哪个渲染器，其余字段原样留着给渲染器取。 */
public class BookPage {
    private final String type;
    private final JsonObject data;

    public BookPage(String type, JsonObject data) {
        this.type = type;
        this.data = data;
    }

    public static BookPage fromJson(JsonObject json) {
        String type = json.has("type") ? json.get("type").getAsString() : "text";
        return new BookPage(type, json);
    }

    /** patchouli 约定：name/text 既可能是字面量，也可能是 lang key。 */
    public static Component translate(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        // 26.3 的 I18n 没有 exists()：get() 找不到时原样返回 key
        String value = I18n.get(raw);
        return value.equals(raw) ? Component.literal(raw) : Component.translatable(raw);
    }

    public String type() {
        return this.type;
    }

    public JsonObject data() {
        return this.data;
    }

    public boolean has(String key) {
        return this.data.has(key);
    }

    public String str(String key) {
        return this.data.has(key) ? this.data.get(key).getAsString() : "";
    }

    public int integer(String key, int def) {
        return this.data.has(key) ? this.data.get(key).getAsInt() : def;
    }

    public float decimal(String key, float def) {
        return this.data.has(key) ? this.data.get(key).getAsFloat() : def;
    }

    public boolean bool(String key, boolean def) {
        return this.data.has(key) ? this.data.get(key).getAsBoolean() : def;
    }

    public Component text(String key) {
        return translate(str(key));
    }

    /** 文本页的每一页正文：pages.N.text 可能直接是 lang key。 */
    public List<Component> lines(String key) {
        List<Component> out = new ArrayList<>();
        if (this.data.has(key) && this.data.get(key).isJsonArray()) {
            JsonArray array = this.data.getAsJsonArray(key);
            array.forEach(e -> out.add(translate(e.getAsString())));
        }
        return out;
    }

    public List<String> strList(String key) {
        List<String> out = new ArrayList<>();
        if (this.data.has(key) && this.data.get(key).isJsonArray()) {
            this.data.getAsJsonArray(key).forEach(e -> out.add(e.getAsString()));
        }
        return out;
    }
}
