package com.github.tartaricacid.touhoulittlemaid.datagen.advancement;

import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.loot.LootTable;

import javax.annotation.Nullable;
import java.util.Optional;

/** 26.3 的 datagen 助手：显示信息的背景图参数被移出 display()；战利品奖励需要 Holder。 */
public final class AdvancementHelper {
    private AdvancementHelper() {
    }

    /** background 传 null 表示该 advancement 不是根节点（26.3 起非根节点不允许有 background）。 */
    public static DisplayInfo displayInfo(ItemStackTemplate icon, Component title, Component description, @Nullable Identifier background,
                                         AdvancementType type, boolean showToast, boolean announceToChat, boolean hidden) {
        return new DisplayInfo(icon, title, description,
                background == null ? Optional.empty() : Optional.of(new ClientAsset.ResourceTexture(background)),
                type, showToast, announceToChat, hidden);
    }

    public static DisplayInfo displayInfo(ItemLike icon, Component title, Component description, @Nullable Identifier background,
                                         AdvancementType type, boolean showToast, boolean announceToChat, boolean hidden) {
        return displayInfo(new ItemStackTemplate(icon.asItem()), title, description, background,
                type, showToast, announceToChat, hidden);
    }

    /** datagen 阶段战利品表尚未进入注册表，用按 key 序列化的独立引用即可（运行时从数据包 JSON 重新解析成真实 holder）。 */
    /** 同理，配方在 datagen 阶段也未进注册表，用按 key 序列化的独立 HolderSet。 */
    public static HolderSet<Recipe<?>> recipeSet(HolderLookup.Provider registries, ResourceKey<Recipe<?>> key) {
        return HolderSet.direct(Holder.Reference.createStandAlone(registries.lookupOrThrow(Registries.RECIPE), key));
    }

    public static Holder<LootTable> lootTable(HolderLookup.Provider registries, ResourceKey<LootTable> key) {
        return Holder.Reference.createStandAlone(registries.lookupOrThrow(Registries.LOOT_TABLE), key);
    }
}
