package org.pantouflemc.economy.commands;

import javax.annotation.Nullable;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.pantouflemc.economy.Economy;
import org.pantouflemc.economy.EconomyError;

import com.hubspot.algebra.Result;

public class EconomyRemoveCommand extends EconomyCommandExecutor {

    public EconomyRemoveCommand() {
        super("remove");
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

            Result<Void, EconomyError> result = Economy.getPlugin().removeBalance(targetPlayer.getUniqueId(), amount);

            if (result.isErr()) {
                switch (result.unwrapErrOrElseThrow()) {
                    case INVALID_AMOUNT:
                        sender.sendMessage("Amount must be positive");
                        break;
                    case INSUFFICIENT_BALANCE:
                        sender.sendMessage("Player does not have enough balance");
                        break;
                    default:
                        sender.sendMessage("An error occurred");
                        break;
                }
                return false;
            }

            sender.sendMessage("$" + amount + " removed from the balance of " + targetPlayer.getName());

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
