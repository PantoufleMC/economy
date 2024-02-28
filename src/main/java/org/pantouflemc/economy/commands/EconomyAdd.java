package org.pantouflemc.economy.commands;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.pantouflemc.economy.EconomyError;

import com.google.common.base.Optional;
import com.hubspot.algebra.Result;

public class EconomyAdd implements CommandExecutor {

    private final org.pantouflemc.economy.Economy plugin;

    public EconomyAdd(org.pantouflemc.economy.Economy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        // Parse the arguments
        var arguments = this.parseArguments(args);
        if (!arguments.isPresent()) {
            return false;
        }
        String targetName = arguments.get().getLeft();
        double amount = arguments.get().getRight();

        // Get the target player
        @Nullable
        OfflinePlayer targetPlayer = sender.getServer().getOfflinePlayerIfCached(targetName);
        if (targetPlayer == null) {
            sender.sendMessage("Target not found");
            return false;
        }

        Result<Void, EconomyError> result = this.plugin.addBalance(targetPlayer.getUniqueId(), amount);

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

        sender.sendMessage("$" + amount + " added to the balance of " + targetPlayer.getName());

        return true;
    }

    private Optional<ImmutablePair<String, Double>> parseArguments(String[] args) {
        if (args.length != 2) {
            return Optional.absent();
        }

        try {
            String playerName = args[0];
            double amount = Double.parseDouble(args[1]);
            return Optional.of(ImmutablePair.of(playerName, amount));
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
