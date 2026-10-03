package com.github.tartaricacid.touhoulittlemaid.client.entity;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.EntityStateTracker;
import net.minecraft.world.entity.LivingEntity;

public class GeckoMaidStateTracker<T extends LivingEntity> extends EntityStateTracker<T> {
    public GeckoMaidStateTracker(T entity) {
        super(entity);
    }

    @Override
    public void reset() {
        super.reset();
    }
}
