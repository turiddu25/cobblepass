package com.cobblemon.mdks.cobblepass.command;

import com.cobblemon.mdks.cobblepass.command.subcommand.AddLevelsCommand;
import com.cobblemon.mdks.cobblepass.command.subcommand.ClaimCommand;
import com.cobblemon.mdks.cobblepass.command.subcommand.PremiumCommand;
import com.cobblemon.mdks.cobblepass.command.subcommand.ReloadCommand;
import com.cobblemon.mdks.cobblepass.command.subcommand.ViewCommand;
import com.cobblemon.mdks.cobblepass.util.BaseCommand;
import com.cobblemon.mdks.cobblepass.util.Constants;
import com.cobblemon.mdks.cobblepass.util.Permissions;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

public class BattlePassCommand extends BaseCommand {
    public BattlePassCommand() {
        super("battlepass",
                Constants.COMMAND_ALIASES,
                new Permissions().getPermission(Constants.PERM_COMMAND_BASE),
                Arrays.asList(
                        new ViewCommand(),
                        new ClaimCommand(),
                        new AddLevelsCommand(),
                        new PremiumCommand(),
                        new ReloadCommand()
                )
        );
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        if (!context.getSource().isPlayer()) {
            context.getSource().sendSystemMessage(
                    Component.literal(Constants.ERROR_PREFIX + "This command must be run by a player!")
            );
            return 1;
        }

        ServerPlayer player = context.getSource().getPlayer();
        ViewCommand.showBattlePassInfo(player);
        return 1;
    }
}
