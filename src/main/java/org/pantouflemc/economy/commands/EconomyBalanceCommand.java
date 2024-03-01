package org.pantouflemc.economy.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.pantouflemc.economy.Economy;

public class EconomyBalanceCommand extends EconomyCommandExecutor {

    public EconomyBalanceCommand() {
        super("balance");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        // Check if the sender is a player
        if (!(sender instanceof Player)) {
            return false;
        }

        // Check if the command has no arguments
        if (args.length != 0) {
            return false;
        }

        // Get the player
        Player player = (Player) sender;

        // Get the balance of the player
        var result = Economy.getPlugin().getBalance(player);

        // Internal error: return false
        if (result.isErr()) {
            return false;
        }

        double balance = result.unwrapOrElseThrow();
        player.sendMessage("Your balance is $" + balance);

        return true;
    }

}
