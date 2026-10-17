package com.github.tartaricacid.touhoulittlemaid.debug.target;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.RenderHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

@VisibleForDebug
public class DebugClientRenderEvent {
    //AfterOpaqueBlocks
    public static void onRender(LevelRenderContext context) {
        if (!TouhouLittleMaid.DEBUG) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Map<Integer, MaidDebugClientData.Entry> all = MaidDebugClientData.data();
        for (Map.Entry<Integer, MaidDebugClientData.Entry> e : all.entrySet()) {
            Entity entity = mc.level.getEntity(e.getKey());
            if (!(entity instanceof EntityMaid maid)) {
                continue;
            }
            MaidDebugClientData.Entry entry = e.getValue();

            // 女仆本体
            Gizmos.cuboid(maid.getBoundingBox(), GizmoStyle.stroke(0xffffffff)).setAlwaysOnTop();

            // 寻路节点连线
            List<BlockPos> path = entry.path();
            for (int i = 0; i + 1 < path.size(); i++) {
                Gizmos.line(nodeCenter(path.get(i)), nodeCenter(path.get(i + 1)), 0xff00ffff);
            }
            // 当前节点
            int cur = entry.currentNode();
            if (cur >= 0 && cur < path.size()) {
                Gizmos.cuboid(nodeBox(path.get(cur)), GizmoStyle.fill(0x6600ffff)).setAlwaysOnTop();
            }

            // 任务目标点
            for (DebugTarget target : entry.targets()) {
                Gizmos.cuboid(nodeBox(target.pos()), GizmoStyle.stroke(target.color())).setAlwaysOnTop();
                RenderHelper.billboardText(target.text(), nodeCenter(target.pos()).add(0, 1, 0),
                        new TextGizmo.Style(target.color(), 1.2f, OptionalDouble.empty()),
                        Font.DisplayMode.SEE_THROUGH).setAlwaysOnTop();
            }
        }
    }

    private static Vec3 nodeCenter(BlockPos pos) {
        return Vec3.atCenterOf(pos);
    }

    private static AABB nodeBox(BlockPos pos) {
        return new AABB(pos).inflate(0.15);
    }
}
