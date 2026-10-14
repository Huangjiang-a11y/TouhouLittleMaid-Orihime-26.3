package cn.sh1rocu.touhoulittlemaid.mixin.compat.tacz;

import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityMaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.TacCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 枪械背上显示：26.3 的 extractBackpackState 只在物品带 TOOL 组件时才填 state.backItem。
 * 这里在 extractRenderState 的 TAIL 补充：当背上物品是 TaCZ 枪械时，也用 itemModelResolver
 * 把它填进 backItem，这样 LayerMaidBackItem / GeckoLayerMaidBackItem 就能把它渲染出来。
 */
@Mixin(EntityMaidRenderState.class)
public class EntityMaidRenderStateCompatMixin {
    @Unique
    private static void tlm$fillGunBackItem(EntityMaid maid, EntityMaidRenderState state,
                                            ItemModelResolver resolver) {
        if (maid == null || state == null || resolver == null) {
            return;
        }
        // 与 1.21.1 一致：女仆配置关掉"显示背物"时不补
        if (!maid.getConfigManager().isShowBackItem()) {
            return;
        }
        ItemStack showItem = maid.getBackpackShowItem();
        if (showItem.isEmpty() || !TacCompat.isGun(showItem)) {
            return;
        }
        if (!state.backItem.isEmpty()) {
            return;
        }
        resolver.updateForLiving(state.backItem, showItem, ItemDisplayContext.FIXED, maid);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private static void tlm$afterExtract(EntityMaid maid, EntityMaidRenderState state, float partialTicks,
                                         BlockModelResolver blockModelResolver,
                                         ItemModelResolver itemModelResolver,
                                         GeckoMaidEntity<? extends EntityMaid> geckoEntity,
                                         CallbackInfo ci) {
        tlm$fillGunBackItem(maid, state, itemModelResolver);
    }
}
