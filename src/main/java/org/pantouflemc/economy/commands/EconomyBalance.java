package org.pantouflemc.economy.commands;

import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EconomyBalance implements CommandExecutor {

    private final org.pantouflemc.economy.Economy plugin;

    public EconomyBalance() {
        this.plugin = org.pantouflemc.economy.Economy.getPlugin(org.pantouflemc.economy.Economy.class);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        // Check if the sender is a player
        if (!(sender instanceof Player)) {
            return false;
        }

        // Get the player
        Player player = (Player) sender;

        // Get the balance of the player
        UUID playerUuid = player.getUniqueId();
        double balance = this.plugin.getPlayerBalance(playerUuid);
        player.sendMessage("Your balance is $" + balance);

        return true;
    }

}
