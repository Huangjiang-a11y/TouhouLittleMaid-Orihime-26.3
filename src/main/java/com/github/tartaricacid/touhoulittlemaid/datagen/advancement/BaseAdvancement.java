package com.github.tartaricacid.touhoulittlemaid.datagen.advancement;

import com.github.tartaricacid.touhoulittlemaid.advancements.maid.MaidEventTrigger;
import com.github.tartaricacid.touhoulittlemaid.advancements.maid.TriggerType;
import com.github.tartaricacid.touhoulittlemaid.datagen.LootTableGenerator;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.*;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.KilledTrigger;
import net.minecraft.advancements.triggers.RecipeCraftedTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import java.util.List;
import net.minecraft.world.level.ItemLike;



import static com.github.tartaricacid.touhoulittlemaid.datagen.advancement.AdvancementHelper.displayInfo;
import static com.github.tartaricacid.touhoulittlemaid.datagen.advancement.AdvancementHelper.recipeSet;

public class BaseAdvancement {
    public static void generate(HolderLookup.Provider registries, BootstrapContext<Advancement> saver) {
        // craft_gohei / craft_chair / build_altar 需要引用配方或战利品表的 holder，
        // 而 Fabric datagen 的注册表集合只含 bootstrap 注册表（数据包注册表不在内），
        // 因此这几条改为静态 JSON：src/main/resources/data/touhou_little_maid/advancement/
        generateAltar(registries, saver);

        generateChair(saver);
    }

    private static void generateChair(BootstrapContext<Advancement> saver) {
        make(InitItems.CHANGE_CHAIR_MODEL, "change_chair_model").parent(id("base/craft_chair"))
                .addCriterion("maid_event", MaidEventTrigger.create(TriggerType.CHANGE_CHAIR_MODEL))
                .save(saver, id("base/change_chair_model").toString());
    }

    private static void generateMaid(BootstrapContext<Advancement> saver, AdvancementHolder root) {
//        ItemStack stack = ItemEntityPlaceholder.setRecipeId(new ItemStack(InitItems.ENTITY_PLACEHOLDER), "spawn_box");
//        AdvancementHolder spawnMaid = make(stack, "spawn_maid").parent(root)
//                .addCriterion("altar_craft", AltarCraftTrigger.Instance.recipe(id("altar_recipe/spawn_box")))
//                .rewards(AdvancementRewards.Builder.loot(lootTable(registries, LootTableGenerator.CAKE)))
//                .save(saver, id("base/spawn_maid").toString());
//
//        makeGoal(Items.CAKE, "tamed_maid").parent(spawnMaid)
//                .addCriterion("maid_event", MaidEventTrigger.create(TriggerType.TAMED_MAID))
//                .save(saver, id("base/tamed_maid").toString());
//
//        make(InitItems.CHANGE_MAID_MODEL, "change_maid_model").parent(spawnMaid)
//                .addCriterion("maid_event", MaidEventTrigger.create(TriggerType.CHANGE_MAID_MODEL))
//                .save(saver, id("base/change_maid_model").toString());
//
//        make(Items.JUKEBOX, "change_maid_sound").parent(spawnMaid)
//                .addCriterion("maid_event", MaidEventTrigger.create(TriggerType.CHANGE_MAID_SOUND))
//                .save(saver, id("base/change_maid_sound").toString());
    }

    private static void generateAltar(HolderLookup.Provider registries, BootstrapContext<Advancement> saver) {
        HolderGetter<EntityType<?>> entityTypes = registries.lookupOrThrow(Registries.ENTITY_TYPE);

        EntityPredicate.Builder predicate = EntityPredicate.Builder.entity().of(entityTypes, InitEntities.FAIRY);
        make(InitItems.FAIRY_SPAWN_EGG, "kill_maid_fairy").parent(id("base/build_altar"))
                .addCriterion("killed_entity", KilledTrigger.TriggerInstance.playerKilledEntity(predicate))
                .save(saver, id("base/kill_maid_fairy").toString());

        make(InitItems.POWER_POINT, "pickup_power_point").parent(id("base/build_altar"))
                .addCriterion("maid_event", MaidEventTrigger.create(TriggerType.PICKUP_POWER_POINT))
                .save(saver, id("base/pickup_power_point").toString());
    }

    private static Advancement.Builder make(ItemLike item, String key) {
        MutableComponent title = Component.translatable(String.format("advancements.touhou_little_maid.base.%s.title", key));
        MutableComponent desc = Component.translatable(String.format("advancements.touhou_little_maid.base.%s.description", key));

        return Advancement.Builder.advancement().display(displayInfo(item, title, desc,
                null,
                AdvancementType.TASK, true, true, false));
    }

    private static Advancement.Builder makeRoot(ItemLike item, String key) {
        MutableComponent title = Component.translatable(String.format("advancements.touhou_little_maid.base.%s.title", key));
        MutableComponent desc = Component.translatable(String.format("advancements.touhou_little_maid.base.%s.description", key));

        return Advancement.Builder.advancement().display(displayInfo(item, title, desc,
                IdentifierUtil.modLoc("advancements/backgrounds/stone"),
                AdvancementType.TASK, true, true, false));
    }

    private static Advancement.Builder make(ItemStack item, String key) {
        MutableComponent title = Component.translatable(String.format("advancements.touhou_little_maid.base.%s.title", key));
        MutableComponent desc = Component.translatable(String.format("advancements.touhou_little_maid.base.%s.description", key));

        return Advancement.Builder.advancement().display(displayInfo(ItemStackTemplate.fromNonEmptyStack(item), title, desc,
                null,
                AdvancementType.TASK, true, true, false));
    }

    private static Advancement.Builder makeGoal(ItemLike item, String key) {
        MutableComponent title = Component.translatable(String.format("advancements.touhou_little_maid.base.%s.title", key));
        MutableComponent desc = Component.translatable(String.format("advancements.touhou_little_maid.base.%s.description", key));

        return Advancement.Builder.advancement().display(displayInfo(item, title, desc,
                null,
                AdvancementType.GOAL, true, true, false));
    }

    private static Identifier id(String id) {
        return IdentifierUtil.modLoc(id);
    }

    private static ResourceKey<Recipe<?>> recipeKey(String id) {
        return ResourceKey.create(Registries.RECIPE, id(id));
    }
}
