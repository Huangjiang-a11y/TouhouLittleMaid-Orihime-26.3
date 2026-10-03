package com.github.tartaricacid.touhoulittlemaid.datagen.advancement;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 26.3 起 {@code Advancement.Builder#save} 只接受 {@link BootstrapContext}，
 * 而 Fabric 的 FabricAdvancementProvider 仍然回调 {@code Consumer<AdvancementHolder>}。
 * 此适配器把两者桥接：register 时直接转交给 Consumer。
 */
public class AdvancementSaverContext implements BootstrapContext<Advancement> {
    private final Consumer<AdvancementHolder> consumer;

    public AdvancementSaverContext(Consumer<AdvancementHolder> consumer) {
        this.consumer = consumer;
    }

    @Override
    public Holder.Reference<Advancement> register(ResourceKey<Advancement> key, Advancement value) {
        this.consumer.accept(new AdvancementHolder(key.identifier(), value));
        return null;
    }

    @Override
    public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registryKey) {
        throw new UnsupportedOperationException("datagen 适配器不支持注册表查询");
    }

    @Override
    public <S> Stream<Holder.Reference<S>> listContextElements(ResourceKey<? extends Registry<? extends S>> registryKey) {
        throw new UnsupportedOperationException("datagen 适配器不支持注册表查询");
    }
}
