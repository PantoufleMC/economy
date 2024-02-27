package org.pantouflemc.economy.commands;

import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import org.pantouflemc.economy.database.DatabaseManager;

public class EconomyBalance implements CommandExecutor {

    private final DatabaseManager databaseManager;

    public EconomyBalance(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
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
        try {
            Integer mainPlayerAccountId = this.databaseManager.getMainAccount(playerUuid);
            assert mainPlayerAccountId != null : "Player does not have a main account";

            @SuppressWarnings("null")
            double balance = this.databaseManager.getBalance(mainPlayerAccountId);
            player.sendMessage("Your balance is $" + balance);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

}
