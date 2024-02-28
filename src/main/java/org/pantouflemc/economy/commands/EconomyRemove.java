package org.pantouflemc.economy.commands;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class EconomyRemove implements CommandExecutor {

    private final org.pantouflemc.economy.Economy plugin;

    public EconomyRemove(org.pantouflemc.economy.Economy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        // Parse the arguments
        ImmutablePair<String, Double> arguments = this.parseArguments(args);
        if (arguments == null) {
            return false;
        }

        // Get the target player
        @Nullable
        OfflinePlayer targetPlayer = sender.getServer().getOfflinePlayerIfCached(arguments.getLeft());
        if (targetPlayer == null) {
            sender.sendMessage("Target not found");
            return false;
        }

        double amount = arguments.getRight();

        // Check if the amount is positive
        if (amount <= 0) {
            sender.sendMessage("Amount must be positive");
            return false;
        }

        // Add the amount to the balance of the target player
        @Nullable
        Integer mainPlayerAccountId = null;
        mainPlayerAccountId = this.plugin.getMainAccount(targetPlayer.getPlayer());
        // assert mainPlayerAccountId != null : "Player does not have a main account";
        if (mainPlayerAccountId == null) {
            sender.sendMessage("Player does not have a main account");
            return false;
        }

        this.plugin.removeBalance(mainPlayerAccountId, amount); // TODO: handle if the balance is not enough
        sender.sendMessage("$" + amount + " removed from the balance of " + targetPlayer.getName());

        return true;
    }

    private @Nullable ImmutablePair<String, Double> parseArguments(String[] args) {
        if (args.length != 2) {
            return null;
        }

        try {
            String playerName = args[0];
            double amount = Double.parseDouble(args[1]);
            return new ImmutablePair<>(playerName, amount);
        } catch (Exception e) {
            return null;
        }
    }

}
