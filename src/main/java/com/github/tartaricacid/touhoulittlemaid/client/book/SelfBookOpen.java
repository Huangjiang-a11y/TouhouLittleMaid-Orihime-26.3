package com.github.tartaricacid.touhoulittlemaid.client.book;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import net.minecraft.resources.Identifier;

/** 没装 Patchouli 时，直接用自包含书壳打开内置手册（两条腿走路的"自研腿"）。 */
public final class SelfBookOpen {
    public static final Identifier DEFAULT_BOOK =
            Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "memorizable_gensokyo");

    private SelfBookOpen() {
    }

    public static void openDefault() {
        BookScreen.open(BookContentLoader.load(DEFAULT_BOOK));
    }
}
