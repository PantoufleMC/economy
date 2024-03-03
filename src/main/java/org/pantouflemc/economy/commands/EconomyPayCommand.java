package org.pantouflemc.economy.commands;

import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.pantouflemc.economy.Economy;
import org.pantouflemc.economy.exceptions.EconomyInsufficientBalance;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class EconomyPayCommand extends EconomyCommandExecutor {

    private final Component messageUsage = Component
            .text("Usage: /pay <player> <amount>")
            .color(NamedTextColor.RED);

    public EconomyPayCommand() {
        super("pay");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("economy.pay")) {
            sender.sendMessage(messageNoPermission);
            return true;
        }

        // Check if the sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(messagePlayerOnly);
            return true;
        }

        // Parse the arguments
        if (args.length != 2) {
            sender.sendMessage(messageUsage);
            return true;
        }
        final @NotNull String targetName = args[0];
        final @Nullable Double amount = parseAmount(args[1]);

        // Check if the amount is valid
        if (amount == null || amount == 0) {
            sender.sendMessage(messageInvalidAmount);
            return true;
        } else if (amount < 0) {
            sender.sendMessage(messageAmountMustBePositive);
            return true;
        }

        // Get the target player
        final @Nullable OfflinePlayer targetPlayer = sender.getServer().getOfflinePlayerIfCached(targetName);
        if (targetPlayer == null) {
            sender.sendMessage(messageTargetNotFound);
            return true;
        }

        // Get the player
        final @NotNull Player player = (Player) sender;

        // Check if the target player is the same as the sender
        if (player.getUniqueId().equals(targetPlayer.getUniqueId())) {
            sender.sendMessage(messageTargetIsSender);
            return true;
        }

        try {
            // Transfer the balance
            Economy.getPlugin().transferMoney(player.getUniqueId(), targetPlayer.getUniqueId(), amount);
        } catch (EconomyInsufficientBalance e) {
            sender.sendMessage(messageInsufficientBalance);
            return true;
        } catch (Exception e) {
            sender.sendMessage(messageErrorOccurred);
            return true;
        }

        final Component message = Component.text()
                .content("You transferred ").color(NamedTextColor.GRAY)
                .append(Component.text("$" + formatCurrency(amount), NamedTextColor.WHITE))
                .append(Component.text(" to ", NamedTextColor.GRAY))
                .append(Component.text(targetName, NamedTextColor.WHITE))
                .build();
        sender.sendMessage(message);
        return true;
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
