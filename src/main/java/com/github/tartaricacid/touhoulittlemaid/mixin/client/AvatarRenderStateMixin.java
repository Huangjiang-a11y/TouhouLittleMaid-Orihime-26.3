package com.github.tartaricacid.touhoulittlemaid.mixin.client;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements ICarryingMaidRenderState {
    @Unique
    private boolean tlm$carryingMaid;

    @Override
    public boolean tlm$isCarryingMaid() {
        return tlm$carryingMaid;
    }

    @Override
    public void tlm$setCarryingMaid(boolean carryingMaid) {
        this.tlm$carryingMaid = carryingMaid;
    }
}
