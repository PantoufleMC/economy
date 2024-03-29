package org.pantouflemc.economy.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.pantouflemc.economy.Economy;

public class EconomyBalanceTopCommand extends EconomyCommandExecutor {

    public EconomyBalanceTopCommand() {
        super("balancetop");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("economy.balancetop")) {
            sender.sendMessage("You don't have permission to use this command");
            return false;
        }

        try {
            // Check if the command has no arguments
            if (args.length != 0) {
                return false;
            }

            // Get the accounts
            var accounts = Economy.getPlugin().getTopPlayerAccounts(10, 0);
            for (int i = 0; i < accounts.size(); i++) {
                var account = accounts.get(i);
                sender.sendMessage((i + 1) + ". " + account.getLeft() + " - $" + account.getRight());
            }

            return true;
        } catch (Exception e) {
            sender.sendMessage("An error occurred");
            return false;
        }
    }

}
