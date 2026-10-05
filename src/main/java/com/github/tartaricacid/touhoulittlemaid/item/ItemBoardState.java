package com.github.tartaricacid.touhoulittlemaid.item;

import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.inventory.tooltip.BoardStateTooltip;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 棋类残局道具（五子棋/中国象棋/国际象棋）。
 * <p>
 * 1.21.1 就有这个功能，26.x 那条线把「实体占位符」以外的这套也整个丢了，
 * 这里按原实现搬回来：数据存在 {@link InitDataComponent#BOARD_STATE_TAG} 组件里，
 * 对着棋类方块右键即可载入残局（见三个棋类方块），按住 Shift 还能预览棋盘。
 */
public class ItemBoardState extends Item {
    public static final String DATA_TAG = "BoardStateData";
    public static final String DESC_TAG = "BoardStateDesc";
    public static final String AUTHOR_TAG = "BoardStateAuthor";

    public ItemBoardState(Identifier id) {
        super(new Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, id)));
    }

    public static void setState(ItemStack stack, String data, String desc, String author) {
        stack.set(InitDataComponent.BOARD_STATE_TAG, new BoardStateInfo(data, desc, author));
    }

    @Nullable
    public static String[] getState(ItemStack stack) {
        BoardStateInfo info = stack.get(InitDataComponent.BOARD_STATE_TAG);
        if (info == null) {
            return null;
        }
        return new String[]{info.data(), info.description(), info.author()};
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (!Minecraft.getInstance().hasShiftDown()) {
            return Optional.empty();
        }
        String[] state = getState(stack);
        if (state == null || StringUtils.isBlank(state[0])) {
            return Optional.empty();
        }
        if (stack.is(InitItems.GOMOKU_BOARD_STATE)) {
            return Optional.of(BoardStateTooltip.ofGomoku(state[0]));
        } else if (stack.is(InitItems.CCHESS_BOARD_STATE)) {
            return Optional.of(BoardStateTooltip.ofXiangqi(state[0]));
        } else if (stack.is(InitItems.WCHESS_BOARD_STATE)) {
            return Optional.of(BoardStateTooltip.ofChess(state[0]));
        }
        return Optional.empty();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        String[] state = getState(stack);
        if (state == null) {
            tooltip.accept(Component.translatable("tooltips.touhou_little_maid.board_state.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (StringUtils.isNotBlank(state[1])) {
            tooltip.accept(Component.translatable(state[1]).withStyle(ChatFormatting.GRAY));
        }
        if (StringUtils.isNotBlank(state[2])) {
            tooltip.accept(Component.translatable("tooltips.touhou_little_maid.board_state.author", state[2]).withStyle(ChatFormatting.GRAY));
        }
        if (!Minecraft.getInstance().hasShiftDown()) {
            tooltip.accept(Component.translatable("board_state.touhou_little_maid.show_picture")
                    .withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC));
        }
    }

    public record BoardStateInfo(String data, String description, String author) {
        public static final Codec<BoardStateInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("data").forGetter(BoardStateInfo::data),
                Codec.STRING.fieldOf("description").forGetter(BoardStateInfo::description),
                Codec.STRING.fieldOf("author").forGetter(BoardStateInfo::author)
        ).apply(instance, BoardStateInfo::new));

        public static final StreamCodec<ByteBuf, BoardStateInfo> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, BoardStateInfo::data,
                ByteBufCodecs.STRING_UTF8, BoardStateInfo::description,
                ByteBufCodecs.STRING_UTF8, BoardStateInfo::author,
                BoardStateInfo::new
        );
    }
}
