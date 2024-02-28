package org.pantouflemc.economy;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.pantouflemc.economy.database.DatabaseError;
import org.pantouflemc.economy.database.DatabaseManager;
import org.pantouflemc.economy.commands.EconomyBalance;
import org.pantouflemc.economy.commands.EconomySet;
import org.pantouflemc.economy.commands.EconomyAdd;
import org.pantouflemc.economy.commands.EconomyRemove;

import com.google.common.primitives.UnsignedInteger;
import com.hubspot.algebra.Result;

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
        pluginManager.registerEvents(new org.pantouflemc.economy.listeners.Player(this), this);

        // Register commands
        this.getCommand("economy").setExecutor(new org.pantouflemc.economy.commands.Economy());
        this.getCommand("economy.balance").setExecutor(new EconomyBalance(this));
        this.getCommand("economy.pay").setExecutor(new org.pantouflemc.economy.commands.EconomyPay());
        this.getCommand("economy.set").setExecutor(new EconomySet(this));
        this.getCommand("economy.add").setExecutor(new EconomyAdd(this));
        this.getCommand("economy.remove").setExecutor(new EconomyRemove(this));
        this.getCommand("economy.top").setExecutor(new org.pantouflemc.economy.commands.EconomyTop());
    }

    @Override
    public void onDisable() {
        databaseManager.disconnect().ifErr(
                error -> logger.warning(error.toString()));
    }

    /// The following methods are used to interact with the database.

    /**
     * Create a new account in the database.
     * 
     * @return The ID of the new account.
     */
    public Result<Integer, EconomyError> createAccount() {
        return databaseManager.createAccount()
                .mapErr(error -> EconomyError.valueOf(error))
                .mapOk(accountId -> accountId.intValue());
    }

    /**
     * Create a new account in the database.
     * 
     * @param player The player to create the account for.
     * @param main   Whether the account is the main account of the player.
     * @return The ID of the new account.
     */
    public Result<Integer, EconomyError> createAccount(Player player, boolean main) {
        Result<Integer, EconomyError> result1 = this.createAccount();

        if (result1.isErr()) {
            return result1;
        }

        Integer accountId = result1.unwrapOrElseThrow();

        Result<Void, DatabaseError> result2 = databaseManager.createPlayerAccountRelation(
                player.getUniqueId(),
                UnsignedInteger.valueOf(accountId),
                main);

        if (result2.isErr()) {
            return result2
                    .mapErr(error -> EconomyError.valueOf(error))
                    .mapOk(success -> (Integer) null); // Cast to Integer to match return type
        }

        return Result.ok(accountId);
    }

    /**
     * Delete an account from the database.
     * 
     * @param accountId The ID of the account to delete.
     */
    public Result<Void, EconomyError> deleteAccount(int accountId) {
        return databaseManager.deleteAccount(UnsignedInteger.valueOf(accountId))
                .mapErr(error -> EconomyError.valueOf(error));
    }

    /**
     * Add a player to an account.
     * 
     * @param player    The player to add to the account.
     * @param accountId The ID of the account to add the player to.
     */
    public Result<Void, EconomyError> addPlayerToAccount(Player player, int accountId) {
        return databaseManager.createPlayerAccountRelation(
                player.getUniqueId(),
                UnsignedInteger.valueOf(accountId),
                false)
                .mapErr(error -> EconomyError.valueOf(error));
    }

    /**
     * Remove a player from an account.
     * 
     * @param player    The player to remove from the account.
     * @param accountId The ID of the account to remove the player from.
     */
    public Result<Void, EconomyError> removePlayerFromAccount(Player player, int accountId) {
        return databaseManager.deletePlayerAccountRelation(player.getUniqueId(), UnsignedInteger.valueOf(accountId))
                .mapErr(error -> EconomyError.valueOf(error));
    }

    /**
     * Transfer money from one account to another.
     * 
     * @param accountId1 The ID of the account to remove money from.
     * @param accountId2 The ID of the account to add money to.
     * @param amount     The amount of money to transfer.
     */
    public Result<Void, EconomyError> transferMoney(int accountId1, int accountId2, double amount) {
        return databaseManager.removeBalance(UnsignedInteger.valueOf(accountId1), amount)
                .match(
                        error -> Result.err(error).mapOk(success -> (Void) null), // Cast to Void to match return type
                        success -> databaseManager.addBalance(UnsignedInteger.valueOf(accountId2), amount))
                .mapErr(error -> EconomyError.valueOf(error));
    }

    /**
     * Transfer money from one player to another.
     * 
     * @param player1 The player to remove money from.
     * @param player2 The player to add money to.
     * @param amount  The amount of money to transfer.
     */
    public Result<Void, EconomyError> transferMoney(Player player1, Player player2, double amount) {
        Result<Integer, DatabaseError> result1 = databaseManager.getMainAccount(player1.getUniqueId());
        Result<Integer, DatabaseError> result2 = databaseManager.getMainAccount(player2.getUniqueId());

        if (result1.isErr()) {
            return result1
                    .mapErr(error -> EconomyError.valueOf(error))
                    .mapOk(success -> (Void) null); // Cast to Void to match return type
        }

        if (result2.isErr()) {
            return result2
                    .mapErr(error -> EconomyError.valueOf(error))
                    .mapOk(success -> (Void) null); // Cast to Void to match return type
        }

        Integer accountId1 = result1.unwrapOrElseThrow();
        Integer accountId2 = result2.unwrapOrElseThrow();

        return this.transferMoney(accountId1, accountId2, amount);
    }

    /**
     * Get the balance of a player.
     * 
     * @param player The player to get the balance of.
     * @return The balance of the player.
     */
    public Result<Double, EconomyError> getPlayerBalance(Player player) {
        return databaseManager.getMainAccount(player.getUniqueId())
                .match(
                        error -> Result.err(error).mapOk(success -> (Double) null), // Cast to Void to match return type
                        accountId -> databaseManager.getBalance(UnsignedInteger.valueOf(accountId)))
                .mapErr(error -> EconomyError.valueOf(error));
    }

    /**
     * Get the balance of an account.
     * 
     * @param accountId The ID of the account
     * @return The balance of the account.
     */
    public Result<Double, EconomyError> getAccountBalance(int accountId) {
        return databaseManager.getBalance(UnsignedInteger.valueOf(accountId))
                .mapErr(error -> EconomyError.valueOf(error));
    }

    /**
     * Set the balance of an account.
     * 
     * @param accountId The ID of the account.
     * @param amount    The new balance of the account.
     */
    public Result<Void, EconomyError> setBalance(int accountId, double amount) {
        return databaseManager.setBalance(UnsignedInteger.valueOf(accountId), amount)
                .mapErr(error -> EconomyError.valueOf(error));
    }

    /**
     * Set the balance of an account.
     * 
     * @param accountId The ID of the account to set the balance of.
     * @param amount    The amount of money to set the balance to.
     */
    public void setBalance(int accountId, double amount) {
        databaseManager.setBalance(UnsignedInteger.valueOf(accountId), amount)
                .ifErr(error -> logger.warning(error.toString()));
    }

    /**
     * Add money to an account.
     * 
     * @param accountId The ID of the account to add money to.
     * @param amount    The amount of money to add.
     */
    public Result<Void, EconomyError> addBalance(int accountId, double amount) {
        return databaseManager.addBalance(UnsignedInteger.valueOf(accountId), amount)
                .mapErr(error -> EconomyError.valueOf(error));
    }

    /**
     * Remove money from an account.
     * 
     * @param accountId The ID of the account to remove money from.
     * @param amount    The amount of money to remove.
     */
    public Result<Void, EconomyError> removeBalance(int accountId, double amount) {
        return databaseManager.removeBalance(UnsignedInteger.valueOf(accountId), amount)
                .mapErr(error -> EconomyError.valueOf(error));
    }

    /**
     * Get every player that is in an account.
     * 
     * @param accountId The ID of the account.
     * @return A list of UUIDs of the players in the account.
     */
    public Result<List<UUID>, EconomyError> getPlayers(int accountId) {
        return databaseManager.getPlayers(UnsignedInteger.valueOf(accountId))
                .mapErr(error -> EconomyError.valueOf(error));
    }

    /**
     * Get every account of a player.
     * 
     * @param player The player to get the accounts of.
     * @return A list of account IDs of the player.
     */
    public Result<List<Integer>, EconomyError> getAccounts(Player player) {
        return databaseManager.getAccounts(player.getUniqueId())
                .mapErr(error -> EconomyError.valueOf(error));
    }

    /**
     * Get the main account of a player.
     * 
     * @param player The player to get the main account of.
     * @return The ID of the main account of the player.
     */
    public Result<Integer, EconomyError> getMainAccount(Player player) {
        return databaseManager.getMainAccount(player.getUniqueId())
                .mapErr(error -> EconomyError.valueOf(error))
                .mapOk(accountId -> accountId.intValue());
    }

}
