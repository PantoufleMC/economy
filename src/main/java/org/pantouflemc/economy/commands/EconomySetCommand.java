package org.pantouflemc.economy.commands;

import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.pantouflemc.economy.Economy;
import org.pantouflemc.economy.EconomyError;

import com.hubspot.algebra.Result;

public class EconomySetCommand extends EconomyCommandExecutor {

    public EconomySetCommand() {
        super("set");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        try {
            // Parse the arguments
            if (args.length != 2)
                return false;
            String targetName = args[0];
            double amount = Double.parseDouble(args[1]);

            // Get the target player
            @Nullable
            OfflinePlayer targetPlayer = sender.getServer().getOfflinePlayerIfCached(targetName);
            if (targetPlayer == null) {
                sender.sendMessage("Target not found");
                return false;
            }

            Result<Void, EconomyError> result = Economy.getPlugin().setBalance(targetPlayer.getUniqueId(), amount);

            if (result.isErr()) {
                switch (result.unwrapErrOrElseThrow()) {
                    case INVALID_AMOUNT:
                        sender.sendMessage("Amount must be positive");
                        break;
                    default:
                        sender.sendMessage("An error occurred");
                        break;
                }
                return false;
            }

            sender.sendMessage("Balance of " + targetPlayer.getName() + " set to $" + amount);

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return Economy.getPlugin().getServer().getOnlinePlayers().stream()
                    .map(player -> player.getName())
                    .toList();
        }

        if (args.length == 2) {
            return List.of("100", "1000", "10000");
        }

        return List.of();
    }

}
