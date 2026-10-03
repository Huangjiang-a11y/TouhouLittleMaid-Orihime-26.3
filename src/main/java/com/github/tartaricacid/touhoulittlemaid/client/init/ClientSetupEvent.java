package com.github.tartaricacid.touhoulittlemaid.client.init;

import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.AnimationRegister;
import com.github.tartaricacid.touhoulittlemaid.client.event.ShowOptifineScreen;
import com.github.tartaricacid.touhoulittlemaid.client.overlay.BroomTipsOverlay;
import com.github.tartaricacid.touhoulittlemaid.client.overlay.MaidTipsOverlay;
import com.github.tartaricacid.touhoulittlemaid.client.overlay.ShowPowerOverlay;
import com.github.tartaricacid.touhoulittlemaid.compat.iris.IrisCompat;
import com.github.tartaricacid.touhoulittlemaid.compat.sodium.SodiumCompat;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

public class ClientSetupEvent {
    public static void onClientSetup() {
        AnimationRegister.registerAnimationState();
        MaidTipsOverlay.init();
        ShowOptifineScreen.checkOptifineIsLoaded();
        KeyMappingRegister.onRegisterKeyMappings();

        // 客户端兼容
        SodiumCompat.init();
    }

    public static void onRegisterGuiLayers() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, modLoc("tlm_maid_tips"), MaidTipsOverlay.INSTANCE);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, modLoc("tlm_broom_tips"), BroomTipsOverlay.INSTANCE);
        HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR, modLoc("tlm_show_power"), ShowPowerOverlay.INSTANCE);
    }
}