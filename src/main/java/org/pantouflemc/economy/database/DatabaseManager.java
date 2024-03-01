package org.pantouflemc.economy.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;
import org.pantouflemc.economy.exceptions.EconomyDriverNotFoundException;
import org.pantouflemc.economy.exceptions.EconomyInsufficientBalance;
import org.pantouflemc.economy.exceptions.EconomyInvalidAmountError;
import org.pantouflemc.economy.exceptions.EconomyDatabaseError;
import org.pantouflemc.economy.exceptions.EconomyAccountNotFoundError;
import org.pantouflemc.economy.exceptions.EconomyDatabaseConnectionError;
import org.pantouflemc.economy.exceptions.EconomyDatabaseDisconnectionError;

import com.google.common.primitives.UnsignedInteger;

public class DatabaseManager {

    private Connection connection;

    /**
     * Create a new DatabaseManager
     */
    public DatabaseManager() throws EconomyDriverNotFoundException, EconomyDatabaseError,
            EconomyDatabaseConnectionError {
        connect();
    }

    /**
     * Connect to the database
     */
    private void connect() throws EconomyDriverNotFoundException, EconomyDatabaseError, EconomyDatabaseConnectionError {
        // TODO: Change SQLite to PostgreSQL
        File databaseDirectory = new File("plugins/Economy");
        File databaseFile = new File(databaseDirectory, "database.db");

        // Create the path to the database if it doesn't exist
        if (!databaseDirectory.exists()) {
            databaseDirectory.mkdirs();
        }

        try {
            // Register SQLite Driver
            Class.forName("org.sqlite.JDBC");

            // Connect to the database
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getPath());
        } catch (ClassNotFoundException e) {
            throw new EconomyDriverNotFoundException();
        } catch (SQLException e) {
            throw new EconomyDatabaseConnectionError();
        }

        // Initialize the database
        initialization();
    }

    /**
     * Disconnect from the database
     */
    public void disconnect() throws EconomyDatabaseDisconnectionError {
        try {
            if (connection != null)
                connection.close();
        } catch (SQLException e) {
            throw new EconomyDatabaseDisconnectionError();
        }
    }

    /**
     * Initialize the database
     */
    private void initialization() throws EconomyDatabaseError {
        try {
            Statement statement = this.connection.createStatement();

            // Create the accounts table
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        balance DOUBLE NOT NULL
                    );
                    """);

            // Create the players name - uuid relation table
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS players (
                        player_uuid VARCHAR(32) PRIMARY KEY,
                        player_name VARCHAR(16) NOT NULL
                    );
                    """);

            // Create the players - accounts relation table
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS players_accounts (
                        player_uuid VARCHAR(32),
                        account_id INTEGER,
                        main BOOLEAN DEFAULT FALSE,
                        PRIMARY KEY (player_uuid, account_id),
                        FOREIGN KEY (player_uuid) REFERENCES players(player_uuid),
                        FOREIGN KEY (account_id) REFERENCES accounts(id),
                        UNIQUE (player_uuid, account_id)
                    );
                    CREATE UNIQUE INDEX IF NOT EXISTS player_uuid_index
                    ON players_accounts (player_uuid, account_id) WHERE main = TRUE;
                    """);

            statement.close();
        } catch (SQLException e) {
            throw new EconomyDatabaseError();
        }
    }

    /**
     * Create a new account
     *
     * @return the ID of the new account
     */
    public @NotNull UnsignedInteger createAccount() throws EconomyDatabaseError {
        try {
            String query = "INSERT INTO accounts (balance) VALUES (0.0);";
            PreparedStatement statement = this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                // Should never happen
                throw new EconomyDatabaseError();
            }

            ResultSet generatedKeys = statement.getGeneratedKeys();

            if (generatedKeys.next()) {
                return UnsignedInteger.valueOf(generatedKeys.getInt(1));
            }

            throw new EconomyDatabaseError();
        } catch (SQLException e) {
            throw new EconomyDatabaseError();
        }
    }

    /**
     * Delete an account
     *
     * @param id the ID of the account
     */
    public void deleteAccount(UnsignedInteger accountId) throws EconomyAccountNotFoundError, EconomyDatabaseError {
        try {
            String query = "DELETE FROM accounts WHERE id = ?;";
            PreparedStatement statement = this.connection.prepareStatement(query);
            statement.setInt(1, accountId.intValue());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new EconomyAccountNotFoundError();
            }
        } catch (SQLException e) {
            throw new EconomyDatabaseError();
        }
    }

    /**
     * Add a player to the database
     *
     * @param playerUuid the UUID of the player
     * @param playerName the name of the player
     */
    public void addPlayer(UUID playerUuid, String playerName) throws EconomyDatabaseError {
        try {
            String query = "INSERT INTO players (player_uuid, player_name) VALUES (?, ?);";
            PreparedStatement statement = this.connection.prepareStatement(query);
            statement.setString(1, playerUuid.toString());
            statement.setString(2, playerName);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new EconomyDatabaseError();
            }
        } catch (SQLException e) {
            throw new EconomyDatabaseError();
        }
    }

    /**
     * Create a new player account relation
     *
     * @param playerUuid the UUID of the player
     * @param accountId  the ID of the account
     * @param main       whether the account is the main account of the player
     */
    public void createPlayerAccountRelation(UUID playerUuid, UnsignedInteger accountId, boolean main)
            throws EconomyAccountNotFoundError, EconomyDatabaseError {
        try {
            String query = main
                    ? "INSERT INTO players_accounts (player_uuid, account_id, main) VALUES (?, ?, ?);"
                    : "INSERT INTO players_accounts (player_uuid, account_id) VALUES (?, ?);";
            PreparedStatement statement = this.connection.prepareStatement(query);
            statement.setString(1, playerUuid.toString());
            statement.setInt(2, accountId.intValue());

            if (main) {
                statement.setBoolean(3, true);
            }

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new EconomyAccountNotFoundError();
            }
        } catch (SQLException e) {
            throw new EconomyDatabaseError();
        }
    }

    /**
     * Delete a player account relation and the account if it has no more players
     * associated
     *
     * @param playerUuid the UUID of the player
     * @param accountId  the ID of the account
     */
    public void deletePlayerAccountRelation(UUID playerUuid, UnsignedInteger accountId)
            throws EconomyAccountNotFoundError, EconomyDatabaseError {
        try {
            String query = """
                    DELETE FROM players_accounts WHERE player_uuid = ? AND account_id = ?;
                    DELETE FROM accounts WHERE id = ? AND NOT EXISTS (SELECT 1 FROM players_accounts WHERE account_id = ?);
                    """;
            PreparedStatement statement = this.connection.prepareStatement(query);
            statement.setString(1, playerUuid.toString());
            statement.setInt(2, accountId.intValue());
            statement.setInt(3, accountId.intValue());
            statement.setInt(4, accountId.intValue());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new EconomyAccountNotFoundError();
            }
        } catch (SQLException e) {
            throw new EconomyDatabaseError();
        }
    }

    /**
     * Get the balance of an account
     *
     * @param accountId the ID of the account
     * @return the balance of the account
     */
    public @NotNull double getBalance(UnsignedInteger accountId) throws EconomyAccountNotFoundError,
            EconomyDatabaseError {
        try {
            String query = "SELECT balance FROM accounts WHERE id = ?;";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, accountId.intValue());

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getDouble("balance");
            }

            throw new EconomyAccountNotFoundError();
        } catch (SQLException e) {
            throw new EconomyDatabaseError();
        }
    }

    /**
     * Set the balance of an account
     *
     * @param accountId the ID of the account
     * @param balance   the new balance of the account (must be positive)
     */
    public void setBalance(UnsignedInteger accountId, double balance) throws EconomyInvalidAmountError,
            EconomyAccountNotFoundError, EconomyDatabaseError {
        if (balance < 0) {
            throw new EconomyInvalidAmountError();
        }

        try {
            String query = "UPDATE accounts SET balance = ? WHERE id = ?;";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setDouble(1, balance);
            statement.setInt(2, accountId.intValue());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new EconomyAccountNotFoundError();
            }
        } catch (SQLException e) {
            throw new EconomyDatabaseError();
        }
    }

    /**
     * Add an amount to the balance of an account
     *
     * @param accountId the ID of the account
     * @param amount    the amount to add (must be positive)
     */
    public void addBalance(UnsignedInteger accountId, double amount) throws EconomyInvalidAmountError,
            EconomyAccountNotFoundError, EconomyDatabaseError {
        if (amount < 0) {
            throw new EconomyInvalidAmountError();
        }

        try {
            String query = "UPDATE accounts SET balance = balance + ? WHERE id = ?;";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setDouble(1, amount);
            statement.setInt(2, accountId.intValue());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new EconomyAccountNotFoundError();
            }
        } catch (SQLException e) {
            throw new EconomyDatabaseError();
        }
    }

    /**
     * Remove an amount from the balance of an account
     *
     * @param accountId the ID of the account
     * @param amount    the amount to remove (must be positive)
     */
    public void removeBalance(UnsignedInteger accountId, double amount) throws EconomyInsufficientBalance,
            EconomyInvalidAmountError, EconomyDatabaseError {
        if (amount < 0) {
            throw new EconomyInvalidAmountError();
        }

        try {
            String query = "UPDATE accounts SET balance = balance - ? WHERE id = ? AND balance >= ?;";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setDouble(1, amount);
            statement.setInt(2, accountId.intValue());
            statement.setDouble(3, amount);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                // We can't distinguish between the account not existing and the account not
                // having enough balance, so we return ACCOUNT_HAS_NOT_ENOUGH_BALANCE in both
                // cases in the hope that the caller will give a correct account ID

                // throw new EconomyAccountNotFoundError();
                throw new EconomyInsufficientBalance();
            }
        } catch (SQLException e) {
            throw new EconomyDatabaseError();
        }
    }

    /**
     * Get the UUIDs of the players associated with an account
     *
     * @param accountId the ID of the account
     * @return the UUIDs of the players associated with the account
     */
    public @NotNull List<UUID> getPlayers(UnsignedInteger accountId) throws EconomyDatabaseError {
        try {
            String query = "SELECT player_uuid FROM players_accounts WHERE account_id = ?;";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, accountId.intValue());

            ResultSet resultSet = statement.executeQuery();

            List<UUID> players = new ArrayList<>();

            while (resultSet.next()) {
                players.add(UUID.fromString(resultSet.getString("player_uuid")));
            }

            // We can't distinguish between the account not existing and the account not
            // having any players, so we return an empty list in both cases
            return players;
        } catch (SQLException e) {
            throw new EconomyDatabaseError();
        }
    }

    /**
     * Get the accounts associated with a player
     *
     * @param playerUuid the UUID of the player
     * @return the IDs of the accounts associated with the player
     */
    public @NotNull List<Integer> getAccounts(UUID playerUuid) throws EconomyDatabaseError {
        try {
            String query = "SELECT account_id FROM players_accounts WHERE player_uuid = ?;";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, playerUuid.toString());

            ResultSet resultSet = statement.executeQuery();

            List<Integer> accounts = new ArrayList<>();

            while (resultSet.next()) {
                accounts.add(resultSet.getInt("account_id"));
            }

            // We can't distinguish between the player not existing and the player not
            // having any accounts, so we return an empty list in both cases
            return accounts;
        } catch (SQLException e) {
            throw new EconomyDatabaseError();
        }
    }

    /**
     * Get the top player accounts
     * 
     * @param limit  the maximum number of accounts to return
     * @param offset the number of accounts to skip
     * @return
     */
    public @NotNull List<ImmutablePair<String, Double>> getTopPlayerAccounts(int limit, int offset)
            throws EconomyDatabaseError {
        try {
            String query = """
                    SELECT player_name, balance FROM players_accounts
                    LEFT JOIN accounts ON players_accounts.account_id = accounts.id
                    LEFT JOIN players ON players_accounts.player_uuid = players.player_uuid
                    WHERE main = TRUE
                    ORDER BY balance DESC
                    LIMIT ? OFFSET ?;
                    """;
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, limit);
            statement.setInt(2, offset);

            ResultSet resultSet = statement.executeQuery();

            List<ImmutablePair<String, Double>> accounts = new ArrayList<>();

            while (resultSet.next()) {
                accounts.add(ImmutablePair.of(
                        resultSet.getString("player_name"),
                        resultSet.getDouble("balance")));
            }

            return accounts;
        } catch (SQLException e) {
            throw new EconomyDatabaseError();
        }
    }

    /**
     * Get the main account of a player
     *
     * @param playerUuid the UUID of the player
     * @return the ID of the main account of the player
     */
    public @NotNull UnsignedInteger getMainAccount(UUID playerUuid) throws EconomyAccountNotFoundError,
            EconomyDatabaseError {
        try {
            String query = "SELECT account_id FROM players_accounts WHERE player_uuid = ? AND main = TRUE;";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, playerUuid.toString());

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return UnsignedInteger.valueOf(resultSet.getInt("account_id"));
            }

            throw new EconomyAccountNotFoundError();
        } catch (SQLException e) {
            throw new EconomyDatabaseError();
        }
    }

    /**
     * Check if a player has a certain account
     *
     * @param playerUuid the UUID of the player
     * @param accountId  the ID of the account
     * @return true if the player has the account, false otherwise
     */
    public @NotNull boolean hasAccount(UUID playerUuid, UnsignedInteger accountId) throws EconomyDatabaseError {
        try {
            String query = "SELECT count(*) FROM players_accounts WHERE player_uuid = ? AND account_id = ?;";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, playerUuid.toString());
            statement.setInt(2, accountId.intValue());

            ResultSet resultSet = statement.executeQuery();

            return resultSet.getInt(1) > 0;
        } catch (SQLException e) {
            throw new EconomyDatabaseError();
        }
    }
}
