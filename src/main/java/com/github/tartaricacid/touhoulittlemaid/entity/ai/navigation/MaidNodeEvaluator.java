package com.github.tartaricacid.touhoulittlemaid.entity.ai.navigation;

import cn.sh1rocu.touhoulittlemaid.util.neoforge.CommonHooks;
import com.github.tartaricacid.touhoulittlemaid.datagen.tag.TagBlock;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 该方法仅修改了栅栏门和梯子的寻路判断
 */
public class MaidNodeEvaluator extends WalkNodeEvaluator {
    /**
     * 复用同一个 MutableBlockPos：getPathType 在寻路过程中会被调用上万次
     * （每个节点都要对女仆碰撞箱内的所有方块求一次），原来每次都 new 一个
     * MutableBlockPos + 两个 BlockPos，是实打实的 GC 压力。
     * <p>
     * 只发给"即时求值、不会留存引用"的原版内部调用（{@code isWithinHome} /
     * {@code PathfindingContext#getBlockState}）；发给可被模组覆写的方法的坐标见
     * {@code getMaidBlockPathTypeRaw} 里的 {@code externalPos}。
     */
    private final BlockPos.MutableBlockPos maidTmpPos = new BlockPos.MutableBlockPos();

    @Override
    public PathType getPathType(PathfindingContext pContext, int pX, int pY, int pZ) {
        return getMaidBlockPathTypeStatic(pContext, pX, pY, pZ);
    }

    @Override
    public int getNeighbors(Node[] outputArray, Node node) {
        int nodeId = super.getNeighbors(outputArray, node);
        return this.createClimbNode(nodeId, outputArray, node);
    }

    // 将可爬行物加入寻路节点里头
    // 一般这些物体都是相连的，所以向上向下搜寻下
    protected int createClimbNode(int nodeId, Node[] nodes, Node origin) {
        // 只有在开启攀爬能力，才将梯子加入寻路节点里
        if (this.mob instanceof EntityMaid maid && maid.getConfigManager().isActiveClimbing()) {
            // 向上搜寻
            BlockPos.MutableBlockPos upPos = new BlockPos.MutableBlockPos(origin.x, origin.y + 1, origin.z);
            if (isMaidCanClimbBlock(upPos, maid)) {
                Node node = this.getNode(upPos);
                if (!node.closed) {
                    node.costMalus = 0;
                    node.type = PathType.WALKABLE;
                    if (nodeId < nodes.length) {
                        nodes[nodeId++] = node;
                    }
                }
            }
            // 向下搜寻
            BlockPos.MutableBlockPos downPos = new BlockPos.MutableBlockPos(origin.x, origin.y - 1, origin.z);
            if (isMaidCanClimbBlock(downPos, maid)) {
                Node node = this.getNode(downPos);
                if (!node.closed) {
                    node.costMalus = 0;
                    node.type = PathType.WALKABLE;
                    if (nodeId < nodes.length) {
                        nodes[nodeId++] = node;
                    }
                }
            }
        }
        return nodeId;
    }

    private PathType getMaidBlockPathTypeStatic(PathfindingContext context, int x, int y, int z) {
        PathType pathType = getMaidBlockPathTypeRaw(context, x, y, z);
        if (pathType == PathType.OPEN && y >= context.level().getMinY() + 1) {
            // 对应 26.3 原版 WalkNodeEvaluator#getPathTypeStatic 的映射：
            // 脚下是"雪/活板门"时原版返回的是 ON_TOP_OF_POWDER_SNOW / ON_TOP_OF_TRAPDOOR，
            // 移植时错写成了 DAMAGE_CAUTIOUS / TRAPDOOR（虽然 malus 一样，但语义不同）。
            return switch (getMaidBlockPathTypeRaw(context, x, y - 1, z)) {
                case OPEN, WATER, LAVA, WALKABLE -> PathType.OPEN;
                case FIRE -> PathType.FIRE;
                case DAMAGING -> PathType.DAMAGING;
                case STICKY_HONEY -> PathType.STICKY_HONEY;
                case POWDER_SNOW -> PathType.ON_TOP_OF_POWDER_SNOW;
                case DAMAGE_CAUTIOUS -> PathType.DAMAGE_CAUTIOUS;
                case TRAPDOOR -> PathType.ON_TOP_OF_TRAPDOOR;
                default -> checkNeighbourBlocks(context, x, y, z, PathType.WALKABLE);
            };
        } else {
            return pathType;
        }
    }

    private PathType getMaidBlockPathTypeRaw(PathfindingContext context, int pX, int pY, int pZ) {
        BlockPos pos = this.maidTmpPos.set(pX, pY, pZ);
        // 女仆在限定范围内寻路寻到了范围外，失败
        if (this.mob instanceof EntityMaid maid && maid.isWithinHome() && !maid.isWithinHome(pos)) {
            return PathType.BLOCKED;
        }

        BlockState blockState = context.getBlockState(pos);
        // 先检查方块是否在黑名单中
        if (blockState.is(TagBlock.MAID_AVOID_BLOCK)) {
            return PathType.DAMAGING;
        }

        // 下面两处会把坐标交给"可能被其它模组覆写"的方法：
        //   canClimb -> CommonHooks.isLadder(BlockState, Level, BlockPos, Entity)
        //   BlockState#getCollisionShape(BlockGetter, BlockPos)
        // 原版这边每次都是新对象，所以有些模组会把引用留下来事后读（原版允许它们这么写）。
        // 因此这两处只能用独立的一份，不能和上面的 maidTmpPos 共用。
        // 一次调用内共享同一份即可 —— 它不会被修改，同时发给两个调用也安全。
        BlockPos externalPos = new BlockPos(pX, pY, pZ);

        PathType pathType;
        if (blockState.getBlock() instanceof FenceGateBlock) {
            pathType = blockState.getValue(FenceGateBlock.OPEN) ? PathType.DOOR_OPEN : PathType.DOOR_WOOD_CLOSED;
        } else if (this.mob instanceof EntityMaid maid && this.canClimb(blockState, externalPos, maid)) {
            // 将楼梯视为可行走方块，便于后续将楼梯加入路径节点
            pathType = PathType.WALKABLE;
        } else {
            pathType = context.getPathTypeFromState(pX, pY, pZ);
            // 判断目标方块的碰撞高度。有些半透明方块拥有超过 0.5（台阶）的高度，此时女仆是不能从其中穿过的，需要将其视为不可通行方块
            if (!heightCheckExclusions(pathType)) {
                VoxelShape shape = blockState.getCollisionShape(context.level(), externalPos);
                double maxY = shape.max(Direction.Axis.Y);
                if (pathType != PathType.BLOCKED) {
                    if (maxY > 0.5) {
                        pathType = PathType.BLOCKED;
                    }
                }
            }
        }
        if (pathType == PathType.DOOR_WOOD_CLOSED && this.mob instanceof EntityMaid maid && !this.canOpenDoor(blockState.getBlock(), maid)) {
            pathType = PathType.DOOR_IRON_CLOSED;
        }
        return pathType;
    }

    private boolean heightCheckExclusions(PathType pathType) {
        // 26.3 新增了 WALKABLE_DOOR（原版对"打开的门"返回这个），漏掉它会让打开的门
        // 被下面的高度检查当成实心方块
        return pathType == PathType.DOOR_OPEN || pathType == PathType.DOOR_WOOD_CLOSED
                || pathType == PathType.WALKABLE_DOOR;
    }

    private boolean canOpenDoor(Block block, EntityMaid maid) {
        if (block instanceof DoorBlock) {
            return maid.getConfigManager().isOpenDoor();
        }
        if (block instanceof FenceGateBlock) {
            return maid.getConfigManager().isOpenFenceGate();
        }
        return true;
    }

    private boolean canClimb(BlockState blockState, BlockPos blockPos, EntityMaid maid) {
        if (isMaidCanClimbBlock(blockState, blockPos, maid)) {
            return maid.getConfigManager().isActiveClimbing();
        }
        return false;
    }

    public static boolean isMaidCanClimbBlock(BlockPos blockPos, EntityMaid maid) {
        Level level = maid.level;
        BlockState blockState = level.getBlockState(blockPos);
        return isMaidCanClimbBlock(blockState, blockPos, maid);
    }

    public static boolean isMaidCanClimbBlock(BlockState blockState, BlockPos blockPos, EntityMaid maid) {
        return CommonHooks.isLadder(blockState, maid.level, blockPos, maid);
    }
}
