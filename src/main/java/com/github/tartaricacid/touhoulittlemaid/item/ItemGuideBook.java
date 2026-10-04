package com.github.tartaricacid.touhoulittlemaid.item;

import com.github.tartaricacid.touhoulittlemaid.client.book.SelfBookOpen;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** 自包含手册物品：右键打开自研书壳（没装 Patchouli 也能看）。 */
public class ItemGuideBook extends Item {
    public ItemGuideBook(Identifier id) {
        super(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .stacksTo(1));
    }

    @Override
    public InteractionResult use(Level worldIn, Player playerIn, InteractionHand handIn) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            SelfBookOpen.openDefault();
            return InteractionResult.SUCCESS;
        }
        return super.use(worldIn, playerIn, handIn);
    }
}
