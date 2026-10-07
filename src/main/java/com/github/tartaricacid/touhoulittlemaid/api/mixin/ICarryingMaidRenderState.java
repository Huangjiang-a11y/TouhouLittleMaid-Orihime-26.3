package com.github.tartaricacid.touhoulittlemaid.api.mixin;

/**
 * 由 AvatarRenderStateMixin 实现的 duck 接口，供 HumanoidModelMixin 读取"玩家正驮着女仆"标记。
 * <p>
 * 注意：**必须放在 mixin 包之外**。com.github.tartaricacid.touhoulittlemaid.mixin.* 是被
 * touhou_little_maid.mixins.json 声明的 mixin 包，包内未被注册的类会被 Mixin 拒绝直接引用
 * （IllegalClassLoadError: ... is in a defined mixin package ... cannot be referenced directly），
 * 客户端启动到 Minecraft.&lt;init&gt; 时就会崩。与 api.mixin.IPlayerMixin 同一模式。
 */

public interface ICarryingMaidRenderState {
    boolean tlm$isCarryingMaid();

    void tlm$setCarryingMaid(boolean carryingMaid);
}
