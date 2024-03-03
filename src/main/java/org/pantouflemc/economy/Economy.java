package org.pantouflemc.economy;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.pantouflemc.economy.commands.EconomyAddCommand;
import org.pantouflemc.economy.commands.EconomyBalanceCommand;
import org.pantouflemc.economy.commands.EconomyBalanceTopCommand;
import org.pantouflemc.economy.commands.EconomyCommand;
import org.pantouflemc.economy.commands.EconomyCommandExecutor;
import org.pantouflemc.economy.commands.EconomyPayCommand;
import org.pantouflemc.economy.commands.EconomyRemoveCommand;
import org.pantouflemc.economy.commands.EconomySetCommand;
import org.pantouflemc.economy.database.DatabaseManager;
import org.pantouflemc.economy.exceptions.EconomyAccountNotFoundError;
import org.pantouflemc.economy.exceptions.EconomyDatabaseError;
import org.pantouflemc.economy.exceptions.EconomyDriverNotFoundException;
import org.pantouflemc.economy.exceptions.EconomyInsufficientBalance;
import org.pantouflemc.economy.exceptions.EconomyInvalidAmountError;

import com.google.common.primitives.UnsignedInteger;

public final class Economy extends JavaPlugin implements Listener {

    private static @NotNull Economy plugin;
    private static @NotNull Logger logger;
    private static @NotNull FileConfiguration config;
    private static @NotNull DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        plugin = this;
        logger = this.getLogger();
        config = this.getConfig();
        try {
            databaseManager = new DatabaseManager();
        } catch (EconomyDriverNotFoundException | EconomyDatabaseError e) {
            logger.severe("An error occurred while trying to connect to the database.");
            throw new RuntimeException(e);
        }

        // Initialize the configuration file
        this.initConfig();

        // Register listeners
        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(this, this);

        // Register commands
        var economyCommand = new EconomyCommand();
        var economyBalanceCommand = new EconomyBalanceCommand();
        var economyBalanceTopCommand = new EconomyBalanceTopCommand();
        var economyPayCommand = new EconomyPayCommand();
        var economySetCommand = new EconomySetCommand();
        var economyAddCommand = new EconomyAddCommand();
        var economyRemoveCommand = new EconomyRemoveCommand();

        economyCommand.registerSubCommand(economyBalanceCommand);
        economyCommand.registerSubCommand(economyBalanceTopCommand);
        economyCommand.registerSubCommand(economyPayCommand);
        economyCommand.registerSubCommand(economySetCommand);
        economyCommand.registerSubCommand(economyAddCommand);
        economyCommand.registerSubCommand(economyRemoveCommand);

        this.registerCommand(economyCommand);
        this.registerCommand(economyBalanceCommand);
        this.registerCommand(economyBalanceTopCommand);
        this.registerCommand(economyPayCommand);
    }

    @Override
    public void onDisable() {
        // We still need to check for null because the plugin can fail and call
        // onDisable prematurely
        if (databaseManager != null)
            databaseManager.close();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws Exception {
        // Create the main account of the player if it does not exist
        org.bukkit.entity.Player player = event.getPlayer();

        // Check if the player already has a main account
        if (Economy.plugin.hasMainAccount(player)) {
            return;
        }

        // Add the player to the database
        Economy.databaseManager.addPlayer(player.getUniqueId(), player.getName());

        // If the player does not have a main account, create one
        Economy.plugin.createAccount(player, true);
    }

    /**
     * Get the instance of the plugin.
     */
    public static Economy getPlugin() {
        return plugin;
    }

    /**
     * Initialize the configuration file.
     */
    private void initConfig() {
        config.addDefault("database.url", "jdbc:sqlite:plugins/economy/database.db");
        config.addDefault("database.username", "username");
        config.addDefault("database.password", "password");
        config.options().copyDefaults(true);
        saveConfig();
    }

    /**
     * Register a command executor.
     * 
     * @param executor the executor to register
     */
    private void registerCommand(@NotNull EconomyCommandExecutor executor) {
        Economy.getPlugin().getCommand(executor.getCommandName()).setExecutor(executor);
    }

    /// The following methods are used to interact with the database.

    /**
     * Create a new account in the database.
     * 
     * @return The ID of the new account.
     */
    public @NotNull UnsignedInteger createAccount() throws EconomyDatabaseError {
        return databaseManager.createAccount();
    }

    /**
     * Create a new account in the database.
     * 
     * @param playerUuid The UUID of the player to create the account for.
     * @param main       Whether the account is the main account of the player.
     * @return The ID of the new account.
     */
    public @NotNull UnsignedInteger createAccount(@NotNull UUID playerUuid, @NotNull boolean main)
            throws EconomyAccountNotFoundError, EconomyDatabaseError {
        // Create the account
        UnsignedInteger accountId = this.createAccount();

        // Add the player to the account
        databaseManager.createPlayerAccountRelation(playerUuid, accountId, main);

        // Return the ID of the new account
        return accountId;
    }

    /**
     * Create a new account in the database.
     * 
     * @param player The player to create the account for.
     * @param main   Whether the account is the main account of the player.
     * @return The ID of the new account.
     */
    public @NotNull UnsignedInteger createAccount(@NotNull Player player, @NotNull boolean main)
            throws EconomyAccountNotFoundError, EconomyDatabaseError {
        return this.createAccount(player.getUniqueId(), main);
    }

    /**
     * Delete an account from the database.
     * 
     * @param accountId The ID of the account to delete.
     */
    public void deleteAccount(@NotNull UnsignedInteger accountId)
            throws EconomyAccountNotFoundError, EconomyDatabaseError {
        databaseManager.deleteAccount(accountId);
    }

    /**
     * Add a player to an account.
     * 
     * @param playerUuid The UUID of the player to add to the account.
     * @param accountId  The ID of the account to add the player to.
     */
    public void addPlayerToAccount(@NotNull UUID playerUuid, @NotNull UnsignedInteger accountId)
            throws EconomyAccountNotFoundError, EconomyDatabaseError {
        databaseManager.createPlayerAccountRelation(
                playerUuid,
                accountId,
                false);
    }

    /**
     * Add a player to an account.
     * 
     * @param player    The player to add to the account.
     * @param accountId The ID of the account to add the player to.
     */
    public void addPlayerToAccount(@NotNull Player player, @NotNull UnsignedInteger accountId)
            throws EconomyAccountNotFoundError, EconomyDatabaseError {
        this.addPlayerToAccount(player.getUniqueId(), accountId);
    }

    /**
     * Remove a player from an account.
     * 
     * @param playerUuid The UUID of the player to remove from the account.
     * @param accountId  The ID of the account to remove the player from.
     */
    public void removePlayerFromAccount(@NotNull UUID playerUuid, @NotNull UnsignedInteger accountId)
            throws EconomyAccountNotFoundError, EconomyDatabaseError {
        databaseManager.deletePlayerAccountRelation(playerUuid, accountId);
    }

    /**
     * Remove a player from an account.
     * 
     * @param player    The player to remove from the account.
     * @param accountId The ID of the account to remove the player from.
     */
    public void removePlayerFromAccount(@NotNull Player player, @NotNull UnsignedInteger accountId)
            throws EconomyAccountNotFoundError, EconomyDatabaseError {
        this.removePlayerFromAccount(player.getUniqueId(), accountId);
    }

    /**
     * Transfer money from one account to another.
     * 
     * @param accountId1 The ID of the account to remove money from.
     * @param accountId2 The ID of the account to add money to.
     * @param amount     The amount of money to transfer.
     */
    public void transferMoney(@NotNull UnsignedInteger accountId1, @NotNull UnsignedInteger accountId2,
            @NotNull double amount)
            throws EconomyAccountNotFoundError, EconomyInsufficientBalance, EconomyInvalidAmountError,
            EconomyDatabaseError {
        databaseManager.removeBalance(accountId1, amount);
        databaseManager.addBalance(accountId2, amount);
    }

    /**
     * Transfer money from one player to another.
     * 
     * @param playerUuid1 The UUID of the player to remove money from.
     * @param playerUuid2 The UUID of the player to add money to.
     * @param amount      The amount of money to transfer.
     */
    public void transferMoney(@NotNull UUID playerUuid1, @NotNull UUID playerUuid2, @NotNull double amount)
            throws EconomyAccountNotFoundError, EconomyInsufficientBalance, EconomyInvalidAmountError,
            EconomyDatabaseError {
        UnsignedInteger accountId1 = this.getMainAccount(playerUuid1);
        UnsignedInteger accountId2 = this.getMainAccount(playerUuid2);

        this.transferMoney(accountId1, accountId2, amount);
    }

    /**
     * Transfer money from one player to another.
     * 
     * @param player1 The player to remove money from.
     * @param player2 The player to add money to.
     * @param amount  The amount of money to transfer.
     */
    public void transferMoney(@NotNull Player player1, @NotNull Player player2, @NotNull double amount)
            throws EconomyAccountNotFoundError, EconomyInsufficientBalance, EconomyInvalidAmountError,
            EconomyDatabaseError {
        this.transferMoney(player1.getUniqueId(), player2.getUniqueId(), amount);
    }

    /**
     * Get the balance of an account.
     * 
     * @param accountId The ID of the account
     * @return The balance of the account.
     */
    public @NotNull double getBalance(@NotNull UnsignedInteger accountId)
            throws EconomyAccountNotFoundError, EconomyDatabaseError {
        return databaseManager.getBalance(accountId);
    }

    /**
     * Get the balance of a player.
     * 
     * @param playerUuid The UUID of the player to get the balance of.
     * @return The balance of the player.
     */
    public @NotNull double getBalance(@NotNull UUID playerUuid)
            throws EconomyAccountNotFoundError, EconomyDatabaseError {
        UnsignedInteger accountId = this.getMainAccount(playerUuid);
        return this.getBalance(accountId);
    }

    /**
     * Get the balance of a player.
     * 
     * @param player The player to get the balance of.
     * @return The balance of the player.
     */
    public @NotNull double getBalance(@NotNull Player player)
            throws EconomyAccountNotFoundError, EconomyDatabaseError {
        return this.getBalance(player.getUniqueId());
    }

    /**
     * Set the balance of an account.
     * 
     * @param accountId The ID of the account.
     * @param amount    The new balance of the account.
     */
    public void setBalance(@NotNull UnsignedInteger accountId, @NotNull double amount)
            throws EconomyInvalidAmountError, EconomyAccountNotFoundError, EconomyDatabaseError {
        databaseManager.setBalance(accountId, amount);
    }

    /**
     * Set the balance of a player.
     * 
     * @param playerUuid The UUID of the player to set the balance of.
     * @param amount     The new balance of the player.
     */
    public void setBalance(@NotNull UUID playerUuid, @NotNull double amount)
            throws EconomyInvalidAmountError, EconomyAccountNotFoundError, EconomyDatabaseError {
        UnsignedInteger accountId = this.getMainAccount(playerUuid);
        this.setBalance(accountId, amount);
    }

    /**
     * Set the balance of a player.
     * 
     * @param player The player to set the balance of.
     * @param amount The new balance of the player.
     */
    public void setBalance(@NotNull Player player, @NotNull double amount)
            throws EconomyInvalidAmountError, EconomyAccountNotFoundError, EconomyDatabaseError {
        this.setBalance(player.getUniqueId(), amount);
    }

    /**
     * Add money to an account.
     * 
     * @param accountId The ID of the account to add money to.
     * @param amount    The amount of money to add.
     */
    public void addBalance(@NotNull UnsignedInteger accountId, @NotNull double amount)
            throws EconomyInvalidAmountError, EconomyAccountNotFoundError, EconomyDatabaseError {
        databaseManager.addBalance(accountId, amount);
    }

    /**
     * Add money to a player.
     * 
     * @param playerUuid The UUID of the player to add money to.
     * @param amount     The amount of money to add.
     */
    public void addBalance(@NotNull UUID playerUuid, @NotNull double amount)
            throws EconomyInvalidAmountError, EconomyAccountNotFoundError, EconomyDatabaseError {
        UnsignedInteger accountId = this.getMainAccount(playerUuid);
        this.addBalance(accountId, amount);
    }

    /**
     * Add money to a player.
     * 
     * @param player The player to add money to.
     * @param amount The amount of money to add.
     */
    public void addBalance(@NotNull Player player, @NotNull double amount)
            throws EconomyInvalidAmountError, EconomyAccountNotFoundError, EconomyDatabaseError {
        this.addBalance(player.getUniqueId(), amount);
    }

    /**
     * Remove money from an account.
     * 
     * @param accountId The ID of the account to remove money from.
     * @param amount    The amount of money to remove.
     */
    public void removeBalance(@NotNull UnsignedInteger accountId, @NotNull double amount)
            throws EconomyInsufficientBalance, EconomyInvalidAmountError, EconomyDatabaseError {
        databaseManager.removeBalance(accountId, amount);
    }

    /**
     * Remove money from a player.
     * 
     * @param playerUuid The UUID of the player to remove money from.
     * @param amount     The amount of money to remove.
     */
    public void removeBalance(@NotNull UUID playerUuid, @NotNull double amount)
            throws EconomyInsufficientBalance, EconomyInvalidAmountError, EconomyDatabaseError {
        UnsignedInteger accountId = this.getMainAccount(playerUuid);
        this.removeBalance(accountId, amount);
    }

    /**
     * Remove money from a player.
     * 
     * @param player The player to remove money from.
     * @param amount The amount of money to remove.
     */
    public void removeBalance(@NotNull Player player, @NotNull double amount)
            throws EconomyInsufficientBalance, EconomyInvalidAmountError, EconomyDatabaseError {
        this.removeBalance(player.getUniqueId(), amount);
    }

    /**
     * Get every player that is in an account.
     * 
     * @param accountId The ID of the account.
     * @return A list of UUIDs of the players in the account.
     */
    public @NotNull List<UUID> getPlayers(@NotNull UnsignedInteger accountId) throws EconomyDatabaseError {
        return databaseManager.getPlayers(accountId);
    }

    /**
     * Get every account of a player.
     * 
     * @param playerUuid The UUID of the player to get the accounts of.
     * @return A list of account IDs of the player.
     */
    public @NotNull List<Integer> getAccounts(@NotNull UUID playerUuid) throws EconomyDatabaseError {
        return databaseManager.getAccounts(playerUuid);
    }

    /**
     * Get every account of a player.
     * 
     * @param player The player to get the accounts of.
     * @return A list of account IDs of the player.
     */
    public @NotNull List<Integer> getAccounts(@NotNull Player player) throws EconomyDatabaseError {
        return this.getAccounts(player.getUniqueId());
    }

    /**
     * Get the top player accounts.
     * 
     * @param limit  the maximum number of accounts to return
     * @param offset the number of accounts to skip
     * @return A list of pairs of player names and their account balances.
     */
    public @NotNull List<ImmutablePair<String, Double>> getTopPlayerAccounts(@NotNull int limit, @NotNull int offset)
            throws EconomyDatabaseError {
        return databaseManager.getTopPlayerAccounts(limit, offset);
    }

    /**
     * Get the main account of a player.
     * 
     * @param playerUuid The UUID of the player to get the main account of.
     * @return The ID of the main account of the player.
     */
    public @NotNull UnsignedInteger getMainAccount(@NotNull UUID playerUuid)
            throws EconomyAccountNotFoundError, EconomyDatabaseError {
        return databaseManager.getMainAccount(playerUuid);
    }

    /**
     * Get the main account of a player.
     * 
     * @param player The player to get the main account of.
     * @return The ID of the main account of the player.
     */
    public @NotNull UnsignedInteger getMainAccount(@NotNull Player player)
            throws EconomyAccountNotFoundError, EconomyDatabaseError {
        return this.getMainAccount(player.getUniqueId());
    }

    /**
     * Check if a player has a main account.
     * 
     * @param playerUuid The UUID of the player to check.
     * @return Whether the player has a main account.
     */
    public @NotNull boolean hasMainAccount(@NotNull UUID playerUuid) throws EconomyDatabaseError {
        try {
            this.getMainAccount(playerUuid);
            return true;
        } catch (EconomyAccountNotFoundError e) {
            return false;
        }
    }

    /**
     * Check if a player has a main account.
     * 
     * @param player The player to check.
     * @return Whether the player has a main account.
     */
    public @NotNull boolean hasMainAccount(@NotNull Player player) throws EconomyDatabaseError {
        return this.hasMainAccount(player.getUniqueId());
    }

}
