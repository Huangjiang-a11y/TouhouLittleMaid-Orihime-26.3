package com.github.tartaricacid.touhoulittlemaid.advancements.altar;

import com.github.tartaricacid.touhoulittlemaid.init.InitTrigger;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class AltarCraftTrigger extends SimpleCriterionTrigger<AltarCraftTrigger.Instance> {
    public static Criterion<Instance> create(Identifier recipeId) {
        Instance instance = new Instance(Optional.empty(), recipeId);
        return InitTrigger.ALTAR_CRAFT.createCriterion(instance);
    }

    public void trigger(ServerPlayer serverPlayer, Identifier recipeId) {
        super.trigger(serverPlayer, instance -> instance.matches(recipeId));
    }

    @Override
    public Codec<AltarCraftTrigger.Instance> codec() {
        return AltarCraftTrigger.Instance.CODEC;
    }

    public record Instance(Optional<Holder<LootItemCondition>> player, Identifier recipeId) implements SimpleInstance {
        public static final Codec<AltarCraftTrigger.Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        LootItemCondition.CODEC.optionalFieldOf("player").forGetter(AltarCraftTrigger.Instance::player),
                        Identifier.CODEC.fieldOf("recipe_id").forGetter(AltarCraftTrigger.Instance::recipeId))
                .apply(instance, AltarCraftTrigger.Instance::new));

        public boolean matches(Identifier recipeIdIn) {
            return this.recipeId.equals(recipeIdIn);
        }
    }
}
