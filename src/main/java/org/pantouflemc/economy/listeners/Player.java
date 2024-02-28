package org.pantouflemc.economy.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class Player implements Listener {

    private final org.pantouflemc.economy.Economy plugin;

    public Player(org.pantouflemc.economy.Economy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws Exception {
        org.bukkit.entity.Player player = event.getPlayer();

        // Check if the player already has a main account
        if (this.plugin.getMainAccount(player).isOk()) {
            return;
        }

        // If the player does not have a main account, create one
        this.plugin.createAccount(player, true);
    }
}
