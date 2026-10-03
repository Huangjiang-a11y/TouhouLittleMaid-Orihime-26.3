package cn.sh1rocu.touhoulittlemaid.mixin.common;

import cn.sh1rocu.touhoulittlemaid.api.extension.IBlockExploded;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {
    @WrapOperation(
            method = "onExplosionHit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;wasExploded(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;)V"
            )
    )
    private void tlm$onBlockExploded(Block instance, ServerLevel level, BlockPos blockPos, Explosion explosion, Operation<Void> original, @Local(argsOnly = true) BlockState state) {
        if (state.getBlock() instanceof IBlockExploded block) {
            block.tlm$onBlockExploded(state, level, blockPos, explosion);
        } else {
            original.call(instance, level, blockPos, explosion);
        }
    }

    // 26.3：原版 onExplosionHit 里写回空气的调用由 setBlock(pos, state, flags) 改为 setBlockAndUpdate(pos, state)
    @WrapOperation(
            method = "onExplosionHit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean tlm$dontJust2Air(ServerLevel instance, BlockPos pos, BlockState airState, Operation<Boolean> original, @Local(argsOnly = true) BlockState state) {
        if (!(state.getBlock() instanceof IBlockExploded)) {
            return original.call(instance, pos, airState);
        } else {
            return false;
        }
    }
}