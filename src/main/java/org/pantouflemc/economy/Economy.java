package org.pantouflemc.economy;

import java.util.logging.Logger;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.pantouflemc.economy.commands.EconomyBalance;
import org.pantouflemc.economy.commands.EconomySet;

import org.pantouflemc.economy.database.DatabaseManager;

public final class Economy extends JavaPlugin {

    private static Economy instance;
    private static Logger logger;
    private static DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;
        logger = this.getLogger();
        databaseManager = new DatabaseManager();

        // Register listeners
        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(new org.pantouflemc.economy.listeners.Player(databaseManager), this);

        // Register commands
        this.getCommand("economy").setExecutor(new org.pantouflemc.economy.commands.Economy());
        this.getCommand("economy.balance").setExecutor(new EconomyBalance(databaseManager));
        this.getCommand("economy.pay").setExecutor(new org.pantouflemc.economy.commands.EconomyPay());
        this.getCommand("economy.set").setExecutor(new EconomySet(databaseManager));
        this.getCommand("economy.add").setExecutor(new org.pantouflemc.economy.commands.EconomyAdd());
        this.getCommand("economy.remove").setExecutor(new org.pantouflemc.economy.commands.EconomyRemove());
        this.getCommand("economy.top").setExecutor(new org.pantouflemc.economy.commands.EconomyTop());
    }

    @Override
    public void onDisable() {
        databaseManager.disconnect();
    }

    /**
     * Get the instance of the plugin
     * 
     * @return The instance of the plugin
     */
    public static Economy getInstance() {
        return instance;
    }

    /**
     * Get the database manager of the plugin
     * 
     * @return The database manager of the plugin
     */
    public static DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
