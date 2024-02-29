package org.pantouflemc.economy.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class EconomyTopCommand extends EconomyCommandExecutor {

    private final org.pantouflemc.economy.Economy plugin;

    public EconomyTopCommand(org.pantouflemc.economy.Economy plugin) {
        super("top");
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