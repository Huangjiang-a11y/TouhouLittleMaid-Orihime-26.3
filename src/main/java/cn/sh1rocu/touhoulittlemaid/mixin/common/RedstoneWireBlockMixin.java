package cn.sh1rocu.touhoulittlemaid.mixin.common;

import cn.sh1rocu.touhoulittlemaid.api.extension.IRedstoneConnect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * From Porting_Lib
 */
@Mixin(RedstoneWireBlock.class)
public class RedstoneWireBlockMixin {
    @Inject(
            method = "shouldConnectTo(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void tlm$shouldConnectTo(BlockState state, BlockGetter level, BlockPos pos, Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof IRedstoneConnect connect) {
            // Passing null for world and pos here just for extra upstream compat, not properly implementing it because
            // 1. world and pos are never used in Create
            // 2. extra work :help_me:
            cir.setReturnValue(connect.tlm$canConnectRedstone(state, level, pos, side));
        }
    }
}