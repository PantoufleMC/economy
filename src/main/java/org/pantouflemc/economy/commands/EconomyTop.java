package org.pantouflemc.economy.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class EconomyTop implements CommandExecutor {

    private final org.pantouflemc.economy.Economy plugin;

    public EconomyTop(org.pantouflemc.economy.Economy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        // Check if the command has no arguments
        if (args.length != 0) {
            return false;
        }

        // Get the accounts
        var result = this.plugin.getTopPlayerAccounts(10, 0);

        // Internal error: return false
        if (result.isErr()) {
            return false;
        }

        var accounts = result.unwrapOrElseThrow();
        for (int i = 0; i < accounts.size(); i++) {
            var account = accounts.get(i);
            sender.sendMessage((i + 1) + ". " + account.getLeft() + " - $" + account.getRight());
        }

        return true;
    }

}
