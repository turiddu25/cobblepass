package com.cobblemon.mdks.cobblepass.command.subcommand;

import com.cobblemon.mdks.cobblepass.CobblePass;
import com.cobblemon.mdks.cobblepass.battlepass.BattlePassTier;
import com.cobblemon.mdks.cobblepass.battlepass.PlayerBattlePass;
import com.cobblemon.mdks.cobblepass.util.Constants;
import com.cobblemon.mdks.cobblepass.util.Subcommand;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ClaimCommand extends Subcommand {
    public ClaimCommand() {
        super("§9Usage: §3/battlepass claim <level> [premium]");
    }

    @Override
    public CommandNode<CommandSourceStack> build() {
        return Commands.literal("claim")
            .then(Commands.argument("level", IntegerArgumentType.integer(1))
                .executes(context -> run(context, false))
                .then(Commands.argument("premium", BoolArgumentType.bool())
                    .executes(context -> run(context, true))))
            .build();
    }

    private int run(CommandContext<CommandSourceStack> context, boolean hasPremiumArg) {
        if (!context.getSource().isPlayer()) {
            context.getSource().sendSystemMessage(
                Component.literal(Constants.ERROR_PREFIX + "This command must be run by a player!")
            );
            return 1;
        }

        ServerPlayer player = context.getSource().getPlayer();
        int level = IntegerArgumentType.getInteger(context, "level");
        boolean premium = hasPremiumArg && BoolArgumentType.getBool(context, "premium");

        PlayerBattlePass pass = CobblePass.battlePass.getPlayerPass(player);
        BattlePassTier tier = CobblePass.battlePass.getTier(level);

        // Check if tier exists
        if (tier == null) {
            player.sendSystemMessage(Component.literal(Constants.ERROR_PREFIX + "Invalid level!"));
            return 1;
        }

        // Handle premium rewards
        if (premium) {
            // Check if player has reached this level
            if (level > pass.getLevel()) {
                player.sendSystemMessage(Component.literal(Constants.ERROR_PREFIX + "You haven't reached level " + level + " yet!"));
                return 1;
            }

            if (!pass.isPremium()) {
                player.sendSystemMessage(Component.literal(Constants.MSG_NOT_PREMIUM));
                return 1;
            }

            if (pass.hasClaimedPremiumReward(level)) {
                player.sendSystemMessage(Component.literal(Constants.MSG_ALREADY_CLAIMED));
                return 1;
            }

            if (!tier.hasPremiumReward()) {
                player.sendSystemMessage(Component.literal(Constants.MSG_NO_REWARD));
                return 1;
            }

            tier.grantPremiumReward(player);
            pass.claimPremiumReward(level);
            player.sendSystemMessage(Component.literal(String.format(
                Constants.MSG_REWARD_CLAIM,
                level
            )));
            return 1;
        }

        // Handle free rewards
        // Check if player has reached this level
        if (level > pass.getLevel()) {
            player.sendSystemMessage(Component.literal(Constants.ERROR_PREFIX + "You haven't reached level " + level + " yet!"));
            return 1;
        }

        if (pass.hasClaimedFreeReward(level)) {
            player.sendSystemMessage(Component.literal(Constants.MSG_ALREADY_CLAIMED));
            return 1;
        }

        if (!tier.hasFreeReward()) {
            player.sendSystemMessage(Component.literal(Constants.MSG_NO_REWARD));
            return 1;
        }

        tier.grantFreeReward(player);
        pass.claimFreeReward(level);
        player.sendSystemMessage(Component.literal(String.format(
            Constants.MSG_REWARD_CLAIM,
            level
        )));
        return 1;
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        return run(context, false);
    }
}
