package org.pantouflemc.economy.listeners;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import org.pantouflemc.economy.database.DatabaseManager;

public class Player implements Listener {

    private final DatabaseManager database;

    public Player(DatabaseManager database) {
        this.database = database;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws Exception {
        UUID playerUuid = event.getPlayer().getUniqueId();

        // Check if the player already has a main account
        if (this.database.getMainAccount(playerUuid) != null) {
            return;
        }

        // If the player does not have a main account, create one
        int accountId = this.database.createAccount();
        this.database.createPlayerAccountRelation(playerUuid, accountId, true);
    }
}
