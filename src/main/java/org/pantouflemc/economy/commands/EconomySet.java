package org.pantouflemc.economy.commands;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.pantouflemc.economy.EconomyError;

import com.hubspot.algebra.Result;

public class EconomySet implements CommandExecutor {

    private final org.pantouflemc.economy.Economy plugin;

    public EconomySet(org.pantouflemc.economy.Economy plugin) {
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

        Result<Void, EconomyError> result = this.plugin.getMainAccount(targetPlayer.getPlayer()).match(
                error -> Result.err(error),
                accountId -> this.plugin.setBalance(accountId, amount));

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
