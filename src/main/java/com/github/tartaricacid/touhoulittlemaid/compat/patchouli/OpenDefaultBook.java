package com.github.tartaricacid.touhoulittlemaid.compat.patchouli;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.event.client.OpenPatchouliBookEvent;
import com.github.tartaricacid.touhoulittlemaid.client.book.SelfBookOpen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import vazkii.patchouli.api.PatchouliAPI;

public final class OpenDefaultBook {
    public static void onPatchouliBookEvent(OpenPatchouliBookEvent event) {
        Identifier uid = event.getTask().getUid();
        if (uid.getNamespace().equals(TouhouLittleMaid.MOD_ID)) {
            Identifier location = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "memorizable_gensokyo");
            if (FabricLoader.getInstance().isModLoaded("patchouli")) {
                PatchouliAPI.get().openBookGUI(location);
            } else {
                SelfBookOpen.openDefault();
            }
        }
    }
}
