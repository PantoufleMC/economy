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

import com.google.common.primitives.UnsignedInteger;
import com.hubspot.algebra.Result;

public class DatabaseManager {

    private Connection connection;

    public DatabaseManager() {
        connect().ifErr(error -> {
            throw new RuntimeException("Failed to connect to the database: " + error);
        });
    }

    /**
     * Connect to the database
     */
    private Result<Void, DatabaseError> connect() {
        // TODO: Change SQLite to PostgreSQL
        File databaseDirectory = new File("plugins/Economy");
        File databaseFile = new File(databaseDirectory, "database.db");

        // Create the path to the database if it doesn't exist
        if (!databaseDirectory.exists()) {
            databaseDirectory.mkdirs();
        }

        // Register Driver Class, this should never fail
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            // Driver class not found
            e.printStackTrace();
            return Result.err(DatabaseError.UNKNOWN_ERROR);
        }

        // Connect to the database
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getPath());
        } catch (SQLException e) {
            e.printStackTrace();
            return Result.err(DatabaseError.CONNECTION_ERROR);
        }

        // Initialize the database
        return initialization();
    }

    /**
     * Disconnect from the database
     */
    public Result<Void, DatabaseError> disconnect() {
        try {
            if (connection != null) {
                connection.close();
            }

            return Result.ok(null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.err(DatabaseError.CONNECTION_ERROR);
        }
    }

    /**
     * Initialize the database
     */
    private Result<Void, DatabaseError> initialization() {
        try {
            Statement statement = this.connection.createStatement();

            // Create the accounts table
            statement.execute("CREATE TABLE IF NOT EXISTS accounts ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "balance DOUBLE NOT NULL"
                    + ");");

            // Create the players name - uuid relation table
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS players (
                        player_uuid VARCHAR(32) PRIMARY KEY,
                        player_name VARCHAR(16) NOT NULL
                    );
                    """);

            // Create the players - accounts relation table
            statement.execute("CREATE TABLE IF NOT EXISTS players_accounts ("
                    + "player_uuid VARCHAR(32),"
                    + "account_id INTEGER,"
                    + "main BOOLEAN DEFAULT FALSE,"
                    + "PRIMARY KEY (player_uuid, account_id),"
                    + "FOREIGN KEY (player_uuid) REFERENCES players(player_uuid),"
                    + "FOREIGN KEY (account_id) REFERENCES accounts(id),"
                    + "UNIQUE (player_uuid, account_id)"
                    + ");"
                    + "CREATE UNIQUE INDEX IF NOT EXISTS player_uuid_index"
                    + "ON players_accounts (player_uuid, account_id)"
                    + "WHERE main = TRUE;");

            statement.close();

            return Result.ok(null);
        } catch (SQLException e) {
            e.printStackTrace();
            return Result.err(DatabaseError.CONNECTION_ERROR);
        }
    }

    /**
     * Create a new account
     *
     * @return the ID of the new account
     */
    public Result<UnsignedInteger, DatabaseError> createAccount() {
        try {
            String query = "INSERT INTO accounts (balance) VALUES (0.0);";
            PreparedStatement statement = this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                // Should never happen
                return Result.err(DatabaseError.UNKNOWN_ERROR);
            }

            ResultSet generatedKeys = statement.getGeneratedKeys();

            if (generatedKeys.next()) {
                return Result.ok(UnsignedInteger.valueOf(generatedKeys.getInt(1)));
            }

            return Result.err(DatabaseError.UNKNOWN_ERROR);
        } catch (SQLException e) {
            return Result.err(DatabaseError.UNKNOWN_ERROR);
        }
    }

    /**
     * Delete an account
     *
     * @param id the ID of the account
     */
    public Result<Void, DatabaseError> deleteAccount(UnsignedInteger accountId) {
        try {
            String query = "DELETE FROM accounts WHERE id = ?;";
            PreparedStatement statement = this.connection.prepareStatement(query);
            statement.setInt(1, accountId.intValue());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return Result.err(DatabaseError.ACCOUNT_NOT_FOUND);
            }

            return Result.ok(null);
        } catch (SQLException e) {
            return Result.err(DatabaseError.UNKNOWN_ERROR);
        }
    }

    /**
     * Add a player to the database
     *
     * @param playerUuid the UUID of the player
     * @param playerName the name of the player
     */
    public Result<Void, DatabaseError> addPlayer(UUID playerUuid, String playerName) {
        try {
            String query = "INSERT INTO players (player_uuid, player_name) VALUES (?, ?);";
            PreparedStatement statement = this.connection.prepareStatement(query);
            statement.setString(1, playerUuid.toString());
            statement.setString(2, playerName);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return Result.err(DatabaseError.UNKNOWN_ERROR);
            }

            return Result.ok(null);
        } catch (SQLException e) {
            return Result.err(DatabaseError.UNKNOWN_ERROR);
        }
    }

    /**
     * Create a new player account relation
     *
     * @param playerUuid the UUID of the player
     * @param accountId  the ID of the account
     * @param main       whether the account is the main account of the player
     */
    public Result<Void, DatabaseError> createPlayerAccountRelation(
            UUID playerUuid,
            UnsignedInteger accountId,
            boolean main) {
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
                return Result.err(DatabaseError.ACCOUNT_NOT_FOUND);
            }

            return Result.ok(null);
        } catch (SQLException e) {
            return Result.err(DatabaseError.UNKNOWN_ERROR);
        }
    }

    /**
     * Delete a player account relation
     *
     * @param playerUuid the UUID of the player
     * @param accountId  the ID of the account
     */
    public Result<Void, DatabaseError> deletePlayerAccountRelation(UUID playerUuid, UnsignedInteger accountId) {
        try {
            String query = "DELETE FROM players_accounts WHERE player_uuid = ? AND account_id = ?;";
            PreparedStatement statement = this.connection.prepareStatement(query);
            statement.setString(1, playerUuid.toString());
            statement.setInt(2, accountId.intValue());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return Result.err(DatabaseError.ACCOUNT_NOT_FOUND);
            }

            // Check if the account has no more players associated with it and delete it if
            // it's the case
            String query2 = "SELECT count(*) FROM players_accounts WHERE account_id = ?;";
            PreparedStatement statement2 = this.connection.prepareStatement(query2);
            statement2.setInt(1, accountId.intValue());

            ResultSet resultSet = statement2.executeQuery();

            if (resultSet.getInt(1) == 0) {
                deleteAccount(accountId);
            }

            return Result.ok(null);
        } catch (SQLException e) {
            return Result.err(DatabaseError.UNKNOWN_ERROR);
        }
    }

    /**
     * Get the balance of an account
     *
     * @param accountId the ID of the account
     * @return the balance of the account
     */
    public Result<Double, DatabaseError> getBalance(UnsignedInteger accountId) {
        try {
            String query = "SELECT balance FROM accounts WHERE id = ?;";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, accountId.intValue());

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return Result.ok(resultSet.getDouble("balance"));
            }

            return Result.err(DatabaseError.ACCOUNT_NOT_FOUND);
        } catch (SQLException e) {
            return Result.err(DatabaseError.UNKNOWN_ERROR);
        }
    }

    /**
     * Set the balance of an account
     *
     * @param accountId the ID of the account
     * @param balance   the new balance of the account (must be positive)
     */
    public Result<Void, DatabaseError> setBalance(UnsignedInteger accountId, double balance) {
        if (balance < 0) {
            return Result.err(DatabaseError.INVALID_AMOUNT);
        }

        try {
            String query = "UPDATE accounts SET balance = ? WHERE id = ?;";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setDouble(1, balance);
            statement.setInt(2, accountId.intValue());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return Result.err(DatabaseError.ACCOUNT_NOT_FOUND);
            }

            return Result.ok(null);
        } catch (SQLException e) {
            return Result.err(DatabaseError.UNKNOWN_ERROR);
        }
    }

    /**
     * Add an amount to the balance of an account
     *
     * @param accountId the ID of the account
     * @param amount    the amount to add (must be positive)
     */
    public Result<Void, DatabaseError> addBalance(UnsignedInteger accountId, double amount) {
        if (amount < 0) {
            return Result.err(DatabaseError.INVALID_AMOUNT);
        }

        try {
            String query = "UPDATE accounts SET balance = balance + ? WHERE id = ?;";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setDouble(1, amount);
            statement.setInt(2, accountId.intValue());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return Result.err(DatabaseError.ACCOUNT_NOT_FOUND);
            }

            return Result.ok(null);
        } catch (SQLException e) {
            return Result.err(DatabaseError.UNKNOWN_ERROR);
        }
    }

    /**
     * Remove an amount from the balance of an account
     *
     * @param accountId the ID of the account
     * @param amount    the amount to remove (must be positive)
     */
    public Result<Void, DatabaseError> removeBalance(UnsignedInteger accountId, double amount) {
        if (amount < 0) {
            return Result.err(DatabaseError.INVALID_AMOUNT);
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

                // return Result.err(DatabaseError.ACCOUNT_NOT_FOUND);
                return Result.err(DatabaseError.ACCOUNT_HAS_NOT_ENOUGH_BALANCE);
            }

            return Result.ok(null);
        } catch (SQLException e) {
            return Result.err(DatabaseError.UNKNOWN_ERROR);
        }
    }

    /**
     * Get the UUIDs of the players associated with an account
     *
     * @param accountId the ID of the account
     * @return the UUIDs of the players associated with the account
     */
    public Result<List<UUID>, DatabaseError> getPlayers(UnsignedInteger accountId) {
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
            return Result.ok(players);
        } catch (SQLException e) {
            return Result.err(DatabaseError.UNKNOWN_ERROR);
        }
    }

    /**
     * Get the accounts associated with a player
     *
     * @param playerUuid the UUID of the player
     * @return the IDs of the accounts associated with the player
     */
    public Result<List<Integer>, DatabaseError> getAccounts(UUID playerUuid) {
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
            return Result.ok(accounts);
        } catch (SQLException e) {
            return Result.err(DatabaseError.UNKNOWN_ERROR);
        }
    }

    /**
     * Get the top player accounts
     * 
     * @param limit  the maximum number of accounts to return
     * @param offset the number of accounts to skip
     * @return
     */
    public Result<List<ImmutablePair<String, Double>>, DatabaseError> getTopPlayerAccounts(
            int limit,
            int offset) {
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

            return Result.ok(accounts);
        } catch (SQLException e) {
            return Result.err(DatabaseError.UNKNOWN_ERROR);
        }
    }

    /**
     * Get the main account of a player
     *
     * @param playerUuid the UUID of the player
     * @return the ID of the main account of the player
     */
    public Result<Integer, DatabaseError> getMainAccount(UUID playerUuid) {
        try {
            String query = "SELECT account_id FROM players_accounts WHERE player_uuid = ? AND main = TRUE;";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, playerUuid.toString());

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return Result.ok(resultSet.getInt("account_id"));
            }

            return Result.err(DatabaseError.PLAYER_DOESNT_HAVE_ACCOUNT);
        } catch (SQLException e) {
            return Result.err(DatabaseError.UNKNOWN_ERROR);
        }
    }

    /**
     * Check if a player has a certain account
     *
     * @param playerUuid the UUID of the player
     * @param accountId  the ID of the account
     * @return true if the player has the account, false otherwise
     */
    public Result<Boolean, DatabaseError> hasAccount(UUID playerUuid, UnsignedInteger accountId) {
        try {
            String query = "SELECT count(*) FROM players_accounts WHERE player_uuid = ? AND account_id = ?;";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, playerUuid.toString());
            statement.setInt(2, accountId.intValue());

            ResultSet resultSet = statement.executeQuery();

            return Result.ok(resultSet.getInt(1) > 0);
        } catch (SQLException e) {
            return Result.err(DatabaseError.UNKNOWN_ERROR);
        }
    }
}
