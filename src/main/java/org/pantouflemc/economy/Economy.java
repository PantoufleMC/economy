package org.pantouflemc.economy;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.pantouflemc.economy.commands.EconomyAddCommand;
import org.pantouflemc.economy.commands.EconomyBalanceCommand;
import org.pantouflemc.economy.commands.EconomyCommand;
import org.pantouflemc.economy.commands.EconomyCommandExecutor;
import org.pantouflemc.economy.commands.EconomyPayCommand;
import org.pantouflemc.economy.commands.EconomyRemoveCommand;
import org.pantouflemc.economy.commands.EconomySetCommand;
import org.pantouflemc.economy.commands.EconomyTopCommand;
import org.pantouflemc.economy.database.DatabaseManager;
import org.pantouflemc.economy.exceptions.EconomyAccountNotFoundError;
import org.pantouflemc.economy.exceptions.EconomyDatabaseDisconnectionError;
import org.pantouflemc.economy.exceptions.EconomyDatabaseError;
import org.pantouflemc.economy.exceptions.EconomyDriverNotFoundException;
import org.pantouflemc.economy.exceptions.EconomyInsufficientBalance;
import org.pantouflemc.economy.exceptions.EconomyInvalidAmountError;
import org.pantouflemc.economy.listeners.PlayerListener;

import com.google.common.primitives.UnsignedInteger;
import com.hubspot.algebra.Result;

public final class Economy extends JavaPlugin {

    private static @NotNull Economy plugin;
    private static @NotNull Logger logger;
    private static @NotNull DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        plugin = this;
        logger = this.getLogger();
        try {
            databaseManager = new DatabaseManager();
        } catch (EconomyDriverNotFoundException | EconomyDatabaseError e) {
            throw new RuntimeException(e);
        }

        // Register listeners
        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerListener(this, databaseManager), this);

        // Register commands
        var economyCommand = new EconomyCommand();
        var economyBalanceCommand = new EconomyBalanceCommand();
        var economyPayCommand = new EconomyPayCommand();
        var economySetCommand = new EconomySetCommand();
        var economyAddCommand = new EconomyAddCommand();
        var economyRemoveCommand = new EconomyRemoveCommand();
        var economyTopCommand = new EconomyTopCommand();

        this.registerCommand(economyCommand);
        this.registerSubCommand(economyBalanceCommand, economyCommand);
        this.registerSubCommand(economyPayCommand, economyCommand);
        this.registerSubCommand(economySetCommand, economyCommand);
        this.registerSubCommand(economyAddCommand, economyCommand);
        this.registerSubCommand(economyRemoveCommand, economyCommand);
        this.registerSubCommand(economyTopCommand, economyCommand);
    }

    @Override
    public void onDisable() {
        try {
            databaseManager.disconnect();
        } catch (EconomyDatabaseDisconnectionError e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the instance of the plugin.
     */
    public static Economy getPlugin() {
        return plugin;
    }

    /**
     * Register a command executor.
     * 
     * @param executor the executor to register
     */
    private void registerCommand(@NotNull EconomyCommandExecutor executor) {
        Economy.getPlugin().getCommand(executor.getCommandName()).setExecutor(executor);
    }

    /**
     * Register a sub-command to a command.
     * 
     * @param executor the executor of the sub-command
     * @param command  the command to register the sub-command to
     */
    private void registerSubCommand(@NotNull EconomyCommandExecutor executor, @NotNull EconomyCommand command) {
        String commandName = command.getCommandName() + "." + executor.getCommandName();
        Economy.getPlugin().getCommand(commandName).setExecutor(executor);
        command.registerSubCommand(executor);
    }

    /// The following methods are used to interact with the database.

    /**
     * Create a new account in the database.
     * 
     * @return The ID of the new account.
     */
    public Result<Integer, EconomyError> createAccount() {
        try {
            var accountId = databaseManager.createAccount();
            return Result.ok(accountId.intValue());
        } catch (EconomyDatabaseError e) {
            return Result.err(EconomyError.UNKNOWN_ERROR);
        }
    }

    /**
     * Create a new account in the database.
     * 
     * @param playerUuid The UUID of the player to create the account for.
     * @param main       Whether the account is the main account of the player.
     * @return The ID of the new account.
     */
    public Result<Integer, EconomyError> createAccount(UUID playerUuid, boolean main) {
        Result<Integer, EconomyError> result1 = this.createAccount();

        if (result1.isErr()) {
            return result1;
        }

        Integer accountId = result1.unwrapOrElseThrow();

        try {
            databaseManager.createPlayerAccountRelation(
                    playerUuid,
                    UnsignedInteger.valueOf(accountId),
                    main);
        } catch (EconomyAccountNotFoundError e) {
            return Result.err(EconomyError.ACCOUNT_NOT_FOUND);
        } catch (EconomyDatabaseError e) {
            return Result.err(EconomyError.UNKNOWN_ERROR);
        }

        return Result.ok(accountId);
    }

    /**
     * Create a new account in the database.
     * 
     * @param player The player to create the account for.
     * @param main   Whether the account is the main account of the player.
     * @return The ID of the new account.
     */
    public Result<Integer, EconomyError> createAccount(Player player, boolean main) {
        return this.createAccount(player.getUniqueId(), main);
    }

    /**
     * Delete an account from the database.
     * 
     * @param accountId The ID of the account to delete.
     */
    public Result<Void, EconomyError> deleteAccount(int accountId) {
        try {
            databaseManager.deleteAccount(UnsignedInteger.valueOf(accountId));
        } catch (EconomyAccountNotFoundError e) {
            return Result.err(EconomyError.ACCOUNT_NOT_FOUND);
        } catch (EconomyDatabaseError e) {
            return Result.err(EconomyError.UNKNOWN_ERROR);
        }

        return Result.ok(null);
    }

    /**
     * Add a player to an account.
     * 
     * @param playerUuid The UUID of the player to add to the account.
     * @param accountId  The ID of the account to add the player to.
     */
    public Result<Void, EconomyError> addPlayerToAccount(UUID playerUuid, int accountId) {
        try {
            databaseManager.createPlayerAccountRelation(
                    playerUuid,
                    UnsignedInteger.valueOf(accountId),
                    false);
        } catch (EconomyAccountNotFoundError e) {
            return Result.err(EconomyError.ACCOUNT_NOT_FOUND);
        } catch (EconomyDatabaseError e) {
            return Result.err(EconomyError.UNKNOWN_ERROR);
        }

        return Result.ok(null);
    }

    /**
     * Add a player to an account.
     * 
     * @param player    The player to add to the account.
     * @param accountId The ID of the account to add the player to.
     */
    public Result<Void, EconomyError> addPlayerToAccount(Player player, int accountId) {
        return this.addPlayerToAccount(player.getUniqueId(), accountId);
    }

    /**
     * Remove a player from an account.
     * 
     * @param playerUuid The UUID of the player to remove from the account.
     * @param accountId  The ID of the account to remove the player from.
     */
    public Result<Void, EconomyError> removePlayerFromAccount(UUID playerUuid, int accountId) {
        try {
            databaseManager.deletePlayerAccountRelation(playerUuid, UnsignedInteger.valueOf(accountId));
        } catch (EconomyAccountNotFoundError e) {
            return Result.err(EconomyError.ACCOUNT_NOT_FOUND);
        } catch (EconomyDatabaseError e) {
            return Result.err(EconomyError.UNKNOWN_ERROR);
        }

        return Result.ok(null);
    }

    /**
     * Remove a player from an account.
     * 
     * @param player    The player to remove from the account.
     * @param accountId The ID of the account to remove the player from.
     */
    public Result<Void, EconomyError> removePlayerFromAccount(Player player, int accountId) {
        return this.removePlayerFromAccount(player.getUniqueId(), accountId);
    }

    /**
     * Transfer money from one account to another.
     * 
     * @param accountId1 The ID of the account to remove money from.
     * @param accountId2 The ID of the account to add money to.
     * @param amount     The amount of money to transfer.
     */
    public Result<Void, EconomyError> transferMoney(int accountId1, int accountId2, double amount) {
        try {
            databaseManager.removeBalance(UnsignedInteger.valueOf(accountId1), amount);
        } catch (EconomyInsufficientBalance e) {
            return Result.err(EconomyError.INSUFFICIENT_BALANCE);
        } catch (EconomyInvalidAmountError e) {
            return Result.err(EconomyError.INVALID_AMOUNT);
        } catch (EconomyDatabaseError e) {
            return Result.err(EconomyError.UNKNOWN_ERROR);
        }

        try {
            databaseManager.addBalance(UnsignedInteger.valueOf(accountId2), amount);
        } catch (EconomyAccountNotFoundError e) {
            return Result.err(EconomyError.ACCOUNT_NOT_FOUND);
        } catch (EconomyInvalidAmountError e) {
            return Result.err(EconomyError.INVALID_AMOUNT);
        } catch (EconomyDatabaseError e) {
            return Result.err(EconomyError.UNKNOWN_ERROR);
        }

        return Result.ok(null);
    }

    /**
     * Transfer money from one player to another.
     * 
     * @param playerUuid1 The UUID of the player to remove money from.
     * @param playerUuid2 The UUID of the player to add money to.
     * @param amount      The amount of money to transfer.
     */
    public Result<Void, EconomyError> transferMoney(UUID playerUuid1, UUID playerUuid2, double amount) {
        Result<Integer, EconomyError> result1 = this.getMainAccount(playerUuid1);
        Result<Integer, EconomyError> result2 = this.getMainAccount(playerUuid2);

        if (result1.isErr()) {
            return result1.mapOk(success -> (Void) null); // Cast to Void to match return type
        }

        if (result2.isErr()) {
            return result2.mapOk(success -> (Void) null); // Cast to Void to match return type
        }

        Integer accountId1 = result1.unwrapOrElseThrow();
        Integer accountId2 = result2.unwrapOrElseThrow();

        return this.transferMoney(accountId1, accountId2, amount);
    }

    /**
     * Transfer money from one player to another.
     * 
     * @param player1 The player to remove money from.
     * @param player2 The player to add money to.
     * @param amount  The amount of money to transfer.
     */
    public Result<Void, EconomyError> transferMoney(Player player1, Player player2, double amount) {
        return this.transferMoney(player1.getUniqueId(), player2.getUniqueId(), amount);
    }

    /**
     * Get the balance of an account.
     * 
     * @param accountId The ID of the account
     * @return The balance of the account.
     */
    public Result<Double, EconomyError> getBalance(int accountId) {
        try {
            return Result.ok(databaseManager.getBalance(UnsignedInteger.valueOf(accountId)));
        } catch (EconomyAccountNotFoundError e) {
            return Result.err(EconomyError.ACCOUNT_NOT_FOUND);
        } catch (EconomyDatabaseError e) {
            return Result.err(EconomyError.UNKNOWN_ERROR);
        }
    }

    /**
     * Get the balance of a player.
     * 
     * @param playerUuid The UUID of the player to get the balance of.
     * @return The balance of the player.
     */
    public Result<Double, EconomyError> getBalance(UUID playerUuid) {
        return this.getMainAccount(playerUuid).match(
                error -> Result.err(error),
                accountId -> this.getBalance(accountId));
    }

    /**
     * Get the balance of a player.
     * 
     * @param player The player to get the balance of.
     * @return The balance of the player.
     */
    public Result<Double, EconomyError> getBalance(Player player) {
        return this.getBalance(player.getUniqueId());
    }

    /**
     * Set the balance of an account.
     * 
     * @param accountId The ID of the account.
     * @param amount    The new balance of the account.
     */
    public Result<Void, EconomyError> setBalance(int accountId, double amount) {
        try {
            databaseManager.setBalance(UnsignedInteger.valueOf(accountId), amount);
        } catch (EconomyInvalidAmountError e) {
            return Result.err(EconomyError.INVALID_AMOUNT);
        } catch (EconomyAccountNotFoundError e) {
            return Result.err(EconomyError.ACCOUNT_NOT_FOUND);
        } catch (EconomyDatabaseError e) {
            return Result.err(EconomyError.UNKNOWN_ERROR);
        }

        return Result.ok(null);
    }

    /**
     * Set the balance of a player.
     * 
     * @param playerUuid The UUID of the player to set the balance of.
     * @param amount     The new balance of the player.
     */
    public Result<Void, EconomyError> setBalance(UUID playerUuid, double amount) {
        return this.getMainAccount(playerUuid).match(
                error -> Result.err(error),
                accountId -> this.setBalance(accountId, amount));
    }

    /**
     * Set the balance of a player.
     * 
     * @param player The player to set the balance of.
     * @param amount The new balance of the player.
     */
    public Result<Void, EconomyError> setBalance(Player player, double amount) {
        return this.setBalance(player.getUniqueId(), amount);
    }

    /**
     * Add money to an account.
     * 
     * @param accountId The ID of the account to add money to.
     * @param amount    The amount of money to add.
     */
    public Result<Void, EconomyError> addBalance(int accountId, double amount) {
        try {
            databaseManager.addBalance(UnsignedInteger.valueOf(accountId), amount);
        } catch (EconomyInvalidAmountError e) {
            return Result.err(EconomyError.INVALID_AMOUNT);
        } catch (EconomyAccountNotFoundError e) {
            return Result.err(EconomyError.ACCOUNT_NOT_FOUND);
        } catch (EconomyDatabaseError e) {
            return Result.err(EconomyError.UNKNOWN_ERROR);
        }

        return Result.ok(null);
    }

    /**
     * Add money to a player.
     * 
     * @param playerUuid The UUID of the player to add money to.
     * @param amount     The amount of money to add.
     */
    public Result<Void, EconomyError> addBalance(UUID playerUuid, double amount) {
        return this.getMainAccount(playerUuid).match(
                error -> Result.err(error),
                accountId -> this.addBalance(accountId, amount));
    }

    /**
     * Add money to a player.
     * 
     * @param player The player to add money to.
     * @param amount The amount of money to add.
     */
    public Result<Void, EconomyError> addBalance(Player player, double amount) {
        return this.addBalance(player.getUniqueId(), amount);
    }

    /**
     * Remove money from an account.
     * 
     * @param accountId The ID of the account to remove money from.
     * @param amount    The amount of money to remove.
     */
    public Result<Void, EconomyError> removeBalance(int accountId, double amount) {
        try {
            databaseManager.removeBalance(UnsignedInteger.valueOf(accountId), amount);
        } catch (EconomyInsufficientBalance e) {
            return Result.err(EconomyError.INSUFFICIENT_BALANCE);
        } catch (EconomyInvalidAmountError e) {
            return Result.err(EconomyError.INVALID_AMOUNT);
        } catch (EconomyDatabaseError e) {
            return Result.err(EconomyError.UNKNOWN_ERROR);
        }

        return Result.ok(null);
    }

    /**
     * Remove money from a player.
     * 
     * @param playerUuid The UUID of the player to remove money from.
     * @param amount     The amount of money to remove.
     */
    public Result<Void, EconomyError> removeBalance(UUID playerUuid, double amount) {
        return this.getMainAccount(playerUuid).match(
                error -> Result.err(error),
                accountId -> this.removeBalance(accountId, amount));
    }

    /**
     * Remove money from a player.
     * 
     * @param player The player to remove money from.
     * @param amount The amount of money to remove.
     */
    public Result<Void, EconomyError> removeBalance(Player player, double amount) {
        return this.removeBalance(player.getUniqueId(), amount);
    }

    /**
     * Get every player that is in an account.
     * 
     * @param accountId The ID of the account.
     * @return A list of UUIDs of the players in the account.
     */
    public Result<List<UUID>, EconomyError> getPlayers(int accountId) {
        try {
            return Result.ok(databaseManager.getPlayers(UnsignedInteger.valueOf(accountId)));
        } catch (EconomyDatabaseError e) {
            return Result.err(EconomyError.UNKNOWN_ERROR);
        }
    }

    /**
     * Get every account of a player.
     * 
     * @param player The player to get the accounts of.
     * @return A list of account IDs of the player.
     */
    public Result<List<Integer>, EconomyError> getAccounts(Player player) {
        try {
            return Result.ok(databaseManager.getAccounts(player.getUniqueId()));
        } catch (EconomyDatabaseError e) {
            return Result.err(EconomyError.UNKNOWN_ERROR);
        }
    }

    /**
     * Get the top player accounts.
     * 
     * @param limit  the maximum number of accounts to return
     * @param offset the number of accounts to skip
     * @return
     */
    public Result<List<ImmutablePair<String, Double>>, EconomyError> getTopPlayerAccounts(
            int limit,
            int offset) {
        try {
            return Result.ok(databaseManager.getTopPlayerAccounts(limit, offset));
        } catch (EconomyDatabaseError e) {
            return Result.err(EconomyError.UNKNOWN_ERROR);
        }
    }

    /**
     * Get the main account of a player.
     * 
     * @param playerUuid The UUID of the player to get the main account of.
     * @return The ID of the main account of the player.
     */
    public Result<Integer, EconomyError> getMainAccount(UUID playerUuid) {
        try {
            return Result.ok(databaseManager.getMainAccount(playerUuid).intValue());
        } catch (EconomyAccountNotFoundError e) {
            return Result.err(EconomyError.ACCOUNT_NOT_FOUND);
        } catch (EconomyDatabaseError e) {
            return Result.err(EconomyError.UNKNOWN_ERROR);
        }
    }

    /**
     * Get the main account of a player.
     * 
     * @param player The player to get the main account of.
     * @return The ID of the main account of the player.
     */
    public Result<Integer, EconomyError> getMainAccount(Player player) {
        return this.getMainAccount(player.getUniqueId());
    }

}
