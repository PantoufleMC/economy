package org.pantouflemc.economy;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.pantouflemc.economy.database.DatabaseError;
import org.pantouflemc.economy.database.DatabaseManager;

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
        pluginManager.registerEvents(new org.pantouflemc.economy.listeners.Player(databaseManager), this);
    }

    @Override
    public void onDisable() {
        databaseManager.disconnect();
    }

    /// The following methods are used to interact with the database.

    /**
     * Create a new account in the database.
     * 
     * @return The ID of the new account.
     */
    public @Nullable Integer createAccount() {
        Result<UnsignedInteger, DatabaseError> result = databaseManager.createAccount();

        if (result.isErr()) {
            logger.warning(result.unwrapErrOrElseThrow().toString());
            return null;
        }

        return result.unwrapOrElseThrow().intValue();
    }

    /**
     * Create a new account in the database.
     * 
     * @param player The player to create the account for.
     * @param main   Whether the account is the main account of the player.
     * @return The ID of the new account.
     */
    public @Nullable Integer createAccount(Player player, boolean main) {
        @Nullable
        Integer accountId = this.createAccount();

        if (accountId == null) {
            return null;
        }

        Result<Void, DatabaseError> result = databaseManager.createPlayerAccountRelation(
                player.getUniqueId(),
                UnsignedInteger.valueOf(accountId),
                main);

        if (result.isErr()) {
            logger.warning(result.unwrapErrOrElseThrow().toString());
            return null;
        }

        return accountId;
    }

    /**
     * Delete an account from the database.
     * 
     * @param accountId The ID of the account to delete.
     */
    public void deleteAccount(int accountId) {
        databaseManager.deleteAccount(UnsignedInteger.valueOf(accountId))
                .ifErr(error -> logger.warning(error.toString()));
    }

    /**
     * Add a player to an account.
     * 
     * @param player    The player to add to the account.
     * @param accountId The ID of the account to add the player to.
     */
    public void addPlayerToAccount(Player player, int accountId) {
        databaseManager.createPlayerAccountRelation(player.getUniqueId(), UnsignedInteger.valueOf(accountId), false)
                .ifErr(error -> logger.warning(error.toString()));
    }

    /**
     * Remove a player from an account.
     * 
     * @param player    The player to remove from the account.
     * @param accountId The ID of the account to remove the player from.
     */
    public void removePlayerFromAccount(Player player, int accountId) {
        databaseManager.deletePlayerAccountRelation(player.getUniqueId(), UnsignedInteger.valueOf(accountId))
                .ifErr(error -> logger.warning(error.toString()));
    }

    /**
     * Transfer money from one account to another.
     * 
     * @param accountId1 The ID of the account to remove money from.
     * @param accountId2 The ID of the account to add money to.
     * @param amount     The amount of money to transfer.
     */
    public void transferMoney(int accountId1, int accountId2, double amount) {
        databaseManager.removeBalance(UnsignedInteger.valueOf(accountId1), amount).match(
                error -> Result.err(error),
                success -> databaseManager.addBalance(UnsignedInteger.valueOf(accountId2), amount))
                .ifErr(error -> logger.warning(error.toString()));
    }

    /**
     * Transfer money from one player to another.
     * 
     * @param player1 The player to remove money from.
     * @param player2 The player to add money to.
     * @param amount  The amount of money to transfer.
     */
    public void transferMoney(Player player1, Player player2, double amount) {
        Result<Integer, DatabaseError> result1 = databaseManager.getMainAccount(player1.getUniqueId());
        Result<Integer, DatabaseError> result2 = databaseManager.getMainAccount(player2.getUniqueId());

        if (result1.isErr()) {
            logger.warning(result1.unwrapErrOrElseThrow().toString());
            return;
        }

        if (result2.isErr()) {
            logger.warning(result2.unwrapErrOrElseThrow().toString());
            return;
        }

        Integer accountId1 = result1.unwrapOrElseThrow();
        Integer accountId2 = result2.unwrapOrElseThrow();

        this.transferMoney(accountId1, accountId2, amount);
    }

    /**
     * Get the balance of a player.
     * 
     * @param player The player to get the balance of.
     * @return The balance of the player.
     */
    @SuppressWarnings("null")
    public @Nonnull Double getPlayerBalance(Player player) {
        Result<Double, DatabaseError> result = databaseManager.getMainAccount(player.getUniqueId()).match(
                error -> Result.err(error),
                accountId -> databaseManager.getBalance(UnsignedInteger.valueOf(accountId)));

        if (result.isErr()) {
            logger.warning(result.unwrapErrOrElseThrow().toString());
            return 0.0;
        }

        return result.unwrapOrElseThrow();
    }

    /**
     * Get the balance of an account.
     * 
     * @param accountId The ID of the account
     * @return The balance of the account.
     */
    @SuppressWarnings("null")
    public @Nonnull Double getAccountBalance(int accountId) {
        Result<Double, DatabaseError> result = databaseManager.getBalance(UnsignedInteger.valueOf(accountId));

        if (result.isErr()) {
            logger.warning(result.unwrapErrOrElseThrow().toString());
            return 0.0;
        }

        return result.unwrapOrElseThrow();
    }

    /**
     * Add money to an account.
     * 
     * @param accountId The ID of the account to add money to.
     * @param amount    The amount of money to add.
     */
    public void addBalance(int accountId, double amount) {
        databaseManager.addBalance(UnsignedInteger.valueOf(accountId), amount)
                .ifErr(error -> logger.warning(error.toString()));
    }

    /**
     * Remove money from an account.
     * 
     * @param accountId The ID of the account to remove money from.
     * @param amount    The amount of money to remove.
     */
    public void removeBalance(int accountId, double amount) {
        databaseManager.removeBalance(UnsignedInteger.valueOf(accountId), amount)
                .ifErr(error -> logger.warning(error.toString()));
    }

    /**
     * Get every player that is in an account.
     * 
     * @param accountId The ID of the account.
     * @return A list of UUIDs of the players in the account.
     */
    @SuppressWarnings("null")
    public @Nonnull List<UUID> getPlayers(int accountId) {
        Result<List<UUID>, DatabaseError> result = databaseManager.getPlayers(UnsignedInteger.valueOf(accountId));

        if (result.isErr()) {
            logger.warning(result.unwrapErrOrElseThrow().toString());
            return List.of();
        }

        return result.unwrapOrElseThrow();
    }

    /**
     * Get every account of a player.
     * 
     * @param player The player to get the accounts of.
     * @return A list of account IDs of the player.
     */
    @SuppressWarnings("null")
    public @Nonnull List<Integer> getAccounts(Player player) {
        Result<List<Integer>, DatabaseError> result = databaseManager.getAccounts(player.getUniqueId());

        if (result.isErr()) {
            logger.warning(result.unwrapErrOrElseThrow().toString());
            return List.of();
        }

        return result.unwrapOrElseThrow();
    }

    /**
     * Get the main account of a player.
     * 
     * @param player The player to get the main account of.
     * @return The ID of the main account of the player.
     */
    public @Nullable Integer getMainAccount(Player player) {
        Result<Integer, DatabaseError> result = databaseManager.getMainAccount(player.getUniqueId());

        if (result.isErr()) {
            logger.warning(result.unwrapErrOrElseThrow().toString());
            return null;
        }

        return result.unwrapOrElseThrow();
    }

}
