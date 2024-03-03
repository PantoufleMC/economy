package org.pantouflemc.economy.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.pantouflemc.economy.Economy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class EconomyBalanceCommand extends EconomyCommandExecutor {

    private final Component messageUsage = Component
            .text("Usage: /balance")
            .color(NamedTextColor.RED);

    public EconomyBalanceCommand() {
        super("balance");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("economy.balance")) {
            sender.sendMessage(messageNoPermission);
            return true;
        }

        // Check if the sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(messagePlayerOnly);
            return true;
        }

        // Check if the command has no arguments
        if (args.length != 0) {
            sender.sendMessage(messageUsage);
            return true;
        }

        // Get the player
        Player player = (Player) sender;

        // Get the balance of the player
        @NotNull
        double balance;
        try {
            balance = Economy.getPlugin().getBalance(player);
        } catch (Exception e) {
            sender.sendMessage(messageErrorOccurred);
            return true;
        }

        final Component balanceString = Component
                .text("$" + formatCurrency(balance))
                .color(NamedTextColor.WHITE);
        final Component message = Component
                .text("Your balance is ")
                .color(NamedTextColor.GRAY)
                .append(balanceString);
        player.sendMessage(message);
        return true;
    }

}
