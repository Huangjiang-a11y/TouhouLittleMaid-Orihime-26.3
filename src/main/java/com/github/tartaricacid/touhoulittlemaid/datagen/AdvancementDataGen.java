package com.github.tartaricacid.touhoulittlemaid.datagen;

import com.github.tartaricacid.touhoulittlemaid.advancements.rewards.GiveSmartSlabConfigTrigger;
import com.github.tartaricacid.touhoulittlemaid.datagen.advancement.AdvancementHelper;
import com.github.tartaricacid.touhoulittlemaid.datagen.advancement.AdvancementSaverContext;
import com.github.tartaricacid.touhoulittlemaid.datagen.advancement.BaseAdvancement;
import com.github.tartaricacid.touhoulittlemaid.datagen.advancement.ChallengeAdvancement;
import com.github.tartaricacid.touhoulittlemaid.datagen.advancement.FavorabilityAdvancement;
import com.github.tartaricacid.touhoulittlemaid.datagen.advancement.MaidBaseAdvancement;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.worldgen.BootstrapContext;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AdvancementDataGen extends FabricAdvancementProvider {
    public AdvancementDataGen(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generateAdvancement(HolderLookup.Provider provider, Consumer<AdvancementHolder> saver) {
        // 26.3: Advancement.Builder#save 需要 BootstrapContext，这里用适配器桥接 Fabric 的 Consumer
        BootstrapContext<Advancement> context = new AdvancementSaverContext(saver);
        // give_smart_slab 需要战利品表 holder，改为静态 JSON（src/main/resources/.../advancement/give_smart_slab.json）
        genMainAdvancement(provider, context);
    }

    private static void genMainAdvancement(HolderLookup.Provider registries, BootstrapContext<Advancement> saver) {
        BaseAdvancement.generate(registries, saver);
        MaidBaseAdvancement.generate(registries, saver);
        FavorabilityAdvancement.generate(saver);
        ChallengeAdvancement.generate(saver);
    }
}
