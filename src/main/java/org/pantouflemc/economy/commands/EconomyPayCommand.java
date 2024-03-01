package org.pantouflemc.economy.commands;

import javax.annotation.Nullable;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.pantouflemc.economy.Economy;

public class EconomyPayCommand extends EconomyCommandExecutor {

    public EconomyPayCommand() {
        super("pay");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        try {
            // Check if the sender is a player
            if (!(sender instanceof Player)) {
                return false;
            }

            // Get the player
            Player player = (Player) sender;

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

            // Check if the target player is the same as the sender
            if (player.getUniqueId().equals(targetPlayer.getUniqueId())) {
                sender.sendMessage("You cannot transfer money to yourself");
                return false;
            }

            // Transfer the balance
            var result = Economy.getPlugin().transferMoney(player.getUniqueId(), targetPlayer.getUniqueId(), amount);

            if (result.isErr()) {
                switch (result.unwrapErrOrElseThrow()) {
                    case INVALID_AMOUNT:
                        sender.sendMessage("Amount must be positive");
                        break;
                    case INSUFFICIENT_BALANCE:
                        sender.sendMessage("You do not have enough balance");
                        break;
                    default:
                        sender.sendMessage("An error occurred");
                        break;
                }
                return false;
            }

            sender.sendMessage("$" + amount + " transferred to " + targetPlayer.getName());

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
