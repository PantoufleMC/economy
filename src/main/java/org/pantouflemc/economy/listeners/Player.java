package org.pantouflemc.economy.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.pantouflemc.economy.database.DatabaseManager;

public class Player implements Listener {

    private final org.pantouflemc.economy.Economy plugin;
    private final DatabaseManager databaseManager;

    public Player(org.pantouflemc.economy.Economy plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws Exception {
        org.bukkit.entity.Player player = event.getPlayer();

        // Check if the player already has a main account
        if (this.plugin.getMainAccount(player).isOk()) {
            return;
        }

        // Add the player to the database
        this.databaseManager.addPlayer(player.getUniqueId(), player.getName());

        // If the player does not have a main account, create one
        this.plugin.createAccount(player, true);
    }
}
