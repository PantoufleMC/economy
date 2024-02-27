package org.pantouflemc.economy;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

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
    }

    @Override
    public void onDisable() {
        databaseManager.disconnect();
    }



    /*
     * The following methods are used to interact with the database.
     */

    /**
     * Create a new account in the database.
     * @return The ID of the new account.
     */

    public void createAccount(UUID player, boolean isMain){
        try {
            int newId = databaseManager.createAccount();
            databaseManager.createPlayerAccountRelation(player, newId, isMain);

        }
        catch (Exception e) {
            logger.info(e.getMessage());
        }
    }

    /**
     * Delete an account from the database.
     * @param accountId The ID of the account to delete.
     */
    public void deleteAccount(int accountId){
        try{
            databaseManager.deleteAccount(accountId);
        }
        catch (Exception e) {
            logger.info(e.getMessage());
        }
    }


    /**
     * Add a player to an account.
     * @param player The player to add to the account.
     * @param accountId The ID of the account to add the player to.
     */
    public void addPlayerToAccount(UUID player, int accountId){
        try{
            databaseManager.createPlayerAccountRelation(player,accountId);
        }
        catch (Exception e) {
            logger.info(e.getMessage());
        }

    }

    /**
     * Remove a player from an account.
     * @param player The player to remove from the account.
     * @param accountId The ID of the account to remove the player from.
     */
    public void removePlayerFromAccount(UUID player, int accountId){
        try{
            databaseManager.deletePlayerAccountRelation(player,accountId);
        }
        catch (Exception e) {
            logger.info(e.getMessage());
        }

    }


    public void transferMoney(int accountId1, int accountId2, double amount) {
        // Transfer money from ACC1 to ACC2.
        try {
            databaseManager.removeBalance(accountId1, amount);
            databaseManager.addBalance(accountId2, amount);
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
    }

    /**
     * Get the balance of a player.
     * @param player The UUID of the player
     * @return The balance of the player.
     */

    public double getPlayerBalance(UUID player){
        double balance=0;
        try{
            int accountId = databaseManager.getMainAccount(player);
            balance = databaseManager.getBalance(accountId);
        }
        catch (Exception e) {
            logger.info(e.getMessage());
        }
        return balance;
    }

    /**
     * Get the balance of an account.
     * @param accountId The ID of the account
     * @return The balance of the account.
     */

    public double getAccountBalance(int accountId){
        double balance=0;
        try{
            balance = databaseManager.getBalance(accountId);
        }
        catch (Exception e) {
            logger.info(e.getMessage());
        }
        return balance;
    }

    /**
     * Add money to an account.
     * @param accountId The ID of the account to add money to.
     * @param amount The amount of money to add.
     */
    public void addBalance(int accountId, double amount){
        try{
            databaseManager.addBalance(accountId, amount);
        }
        catch (Exception e) {
            logger.info(e.getMessage());
        }
    }

    /**
     * Remove money from an account.
     * @param accountId The ID of the account to remove money from.
     * @param amount The amount of money to remove.
     */
    public void removeBalance(int accountId, double amount) {
        try {
            databaseManager.removeBalance(accountId, amount);
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
    }

    /**
     * Get every player that is in an account.
     * @param accountId The ID of the account.
     * @return A list of UUIDs of the players in the account.
     */
    public List<UUID> getPlayers(int accountId){
        List<UUID> players = null;
        try{
            players = databaseManager.getPlayers(accountId);
        }
        catch (Exception e) {
            logger.info(e.getMessage());
        }
        return players;
    }

    /**
     * Get every account of a player.
     * @param playerUuid The UUID of the player.
     * @return A list of account IDs of the player.
     */
    public List<Integer> getAccounts(UUID playerUuid){
        List<Integer> accounts = null;
        try{
            accounts = databaseManager.getAccounts(playerUuid);
        }
        catch (Exception e) {
            logger.info(e.getMessage());
        }
        return accounts;
    }

    /**
     * To one player to pay another
     * @param origin The UUID of the payer
     * @param target The UUID of the receiver
     * @param amount The amount of money to pay.
     */
    public void pay(UUID origin, UUID target, double amount) {
        try {
            int originAccountId = databaseManager.getMainAccount(origin);
            int targetAccountId = databaseManager.getMainAccount(target);
            transferMoney(originAccountId, targetAccountId, amount);
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
    }

}
