package org.pantouflemc.economy.commands;

import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.pantouflemc.economy.Economy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class EconomyBalanceTopCommand extends EconomyCommandExecutor {

    private final Component messageUsage = Component
            .text("Usage: /balancetop", NamedTextColor.RED);

    public EconomyBalanceTopCommand() {
        super("balancetop");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("economy.balancetop")) {
            sender.sendMessage(messageNoPermission);
            return true;
        }

        // Check if the command has no arguments
        if (args.length != 0) {
            sender.sendMessage(messageUsage);
            return true;
        }

        // Get the accounts
        final @NotNull List<ImmutablePair<String, Double>> accounts;
        try {
            accounts = Economy.getPlugin().getTopPlayerAccounts(10, 0);
        } catch (Exception e) {
            sender.sendMessage(messageErrorOccurred);
            return true;
        }

        // Get the max length of the account names
        int maxLength = 0;
        for (final ImmutablePair<String, Double> account : accounts) {
            maxLength = Math.max(maxLength, account.getLeft().length());
        }

        // Show the accounts
        for (int i = 0; i < accounts.size(); i++) {
            final ImmutablePair<String, Double> account = accounts.get(i);

            final String padding = " ".repeat(maxLength - account.getLeft().length());
            final Component message = Component.text()
                    .content((i + 1) + ". ").color(NamedTextColor.GRAY)
                    .append(Component.text(account.getLeft() + padding, NamedTextColor.WHITE))
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(Component.text("$" + formatCurrency(account.getRight()), NamedTextColor.WHITE))
                    .build();
            sender.sendMessage(message);
        }
        return true;
    }

}
