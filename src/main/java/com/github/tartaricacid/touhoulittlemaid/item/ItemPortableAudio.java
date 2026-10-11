package com.github.tartaricacid.touhoulittlemaid.item;

import com.github.tartaricacid.touhoulittlemaid.client.gui.item.PortableAudioGui;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * 手提音响：手持右击打开界面，填入链接（http(s):// 或 file://）即可播放。
 * <p>
 * 播放位置绑定在玩家身上，链接会通过服务端同步给周围玩家——各自拉取各自的链接播放。
 */
public class ItemPortableAudio extends Item {
    public ItemPortableAudio(Identifier id) {
        super(new Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .stacksTo(1)
                .rarity(Rarity.RARE));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            PortableAudioGui.open(player.getItemInHand(hand));
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        String url = stack.get(InitDataComponent.PORTABLE_AUDIO_URL);
        if (url != null && !url.isEmpty()) {
            tooltip.accept(Component.literal(url).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("tooltips.touhou_little_maid.portable_audio.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.accept(Component.translatable("tooltips.touhou_little_maid.portable_audio.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
