package com.github.tartaricacid.touhoulittlemaid.command.subcommand;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * /tlm roulette &lt;动画名&gt;：让附近自己的女仆播放 YSM 模型里的指定动画（轮盘动画）。
 * <p>
 * 传 "stop" 或 "empty" 用于停止。
 */
public final class RouletteCommand {
    private static final String ROULETTE_NAME = "roulette";
    private static final String ANIMATION_NAME = "animation";
    private static final double RADIUS = 8;

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal(ROULETTE_NAME)
                .then(Commands.argument(ANIMATION_NAME, StringArgumentType.string())
                        .executes(RouletteCommand::run));
    }

    private static int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String animation = StringArgumentType.getString(context, ANIMATION_NAME);
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<EntityMaid> maids = player.level().getEntitiesOfClass(EntityMaid.class,
                player.getBoundingBox().inflate(RADIUS), maid -> maid.isOwnedBy(player));
        boolean stop = "stop".equals(animation) || "empty".equals(animation);
        for (EntityMaid maid : maids) {
            if (stop) {
                maid.stopRouletteAnim();
            } else {
                maid.playRouletteAnim(animation);
            }
        }
        int count = maids.size();
        context.getSource().sendSuccess(() -> Component.translatable(
                "commands.touhou_little_maid.roulette.success", animation, count), false);
        return count > 0 ? Command.SINGLE_SUCCESS : 0;
    }
}
