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

public class DatabaseManager {

    private Connection connection;

    public DatabaseManager() {
        connect();
    }

    /**
     * Connect to the database
     */
    private void connect() {
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
            e.printStackTrace();
        }

        // Connect to the database
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getPath());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Initialize the database
        try {
            initialization();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Disconnect from the database
     */
    public void disconnect() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize the database
     */
    private void initialization() throws SQLException {
        Statement statement = connection.createStatement();

        // Create the accounts table
        statement.execute("CREATE TABLE IF NOT EXISTS accounts ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "balance DOUBLE NOT NULL"
                + ");");

        // Create the players - accounts relation table
        statement.execute("CREATE TABLE IF NOT EXISTS players_accounts ("
                + "player_uuid VARCHAR(32),"
                + "account_id INTEGER,"
                + "PRIMARY KEY (player_uuid, account_id),"
                + "FOREIGN KEY (account_id) REFERENCES accounts(id)"
                + ");");

        statement.close();
    }

    /**
     * Create a new account
     *
     * @return the ID of the new account
     */
    public int createAccount() throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO accounts (balance) VALUES (0.0);",
                Statement.RETURN_GENERATED_KEYS);

        int affectedRows = statement.executeUpdate();

        if (affectedRows == 0) {
            throw new SQLException("Creating account failed, no rows affected.");
        }

        ResultSet generatedKeys = statement.getGeneratedKeys();

        if (generatedKeys.next()) {
            return generatedKeys.getInt(1);
        }

        throw new SQLException("Creating account failed, no ID obtained.");
    }

    /**
     * Create a new player account relation
     *
     * @param playerUuid the UUID of the player
     * @param accountId  the ID of the account
     */
    public void createPlayerAccountRelation(UUID playerUuid, int accountId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO players_accounts (player_uuid, account_id) VALUES (?, ?);");

        statement.setString(1, playerUuid.toString());
        statement.setInt(2, accountId);

        int affectedRows = statement.executeUpdate();

        if (affectedRows == 0) {
            throw new SQLException("Creating player account relation failed, no rows affected.");
        }
    }

    /**
     * Get the balance of an account
     *
     * @param accountId the ID of the account
     * @return the balance of the account
     */
    public double getBalance(int accountId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT balance FROM accounts WHERE id = ?;");

        statement.setInt(1, accountId);

        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            return resultSet.getDouble("balance");
        }

        throw new SQLException("Account not found.");
    }

    /**
     * Set the balance of an account
     *
     * @param accountId the ID of the account
     * @param balance   the new balance of the account (must be positive)
     */
    public void setBalance(int accountId, double balance) throws SQLException {
        if (balance < 0) {
            throw new SQLException("Setting balance failed, balance cannot be negative.");
        }

        PreparedStatement statement = connection.prepareStatement(
                "UPDATE accounts SET balance = ? WHERE id = ?;");

        statement.setDouble(1, balance);
        statement.setInt(2, accountId);

        int affectedRows = statement.executeUpdate();

        if (affectedRows == 0) {
            throw new SQLException("Setting balance failed, no rows affected.");
        }
    }

    /**
     * Add an amount to the balance of an account
     *
     * @param accountId the ID of the account
     * @param amount    the amount to add (must be positive)
     */
    public void addBalance(int accountId, double amount) throws SQLException {
        if (amount < 0) {
            throw new SQLException("Adding balance failed, amount cannot be negative.");
        }

        PreparedStatement statement = connection.prepareStatement(
                "UPDATE accounts SET balance = balance + ? WHERE id = ?;");

        statement.setDouble(1, amount);
        statement.setInt(2, accountId);

        int affectedRows = statement.executeUpdate();

        if (affectedRows == 0) {
            throw new SQLException("Adding balance failed, no rows affected.");
        }
    }

    /**
     * Remove an amount from the balance of an account
     *
     * @param accountId the ID of the account
     * @param amount    the amount to remove (must be positive)
     */
    public void removeBalance(int accountId, double amount) throws SQLException {
        if (amount < 0) {
            throw new SQLException("Removing balance failed, amount cannot be negative.");
        }

        PreparedStatement statement = connection.prepareStatement(
                "UPDATE accounts SET balance = balance - ? WHERE id = ? AND balance >= ?;");
        statement.setDouble(1, amount);
        statement.setInt(2, accountId);
        statement.setDouble(3, amount);

        int affectedRows = statement.executeUpdate();

        if (affectedRows == 0) {
            // Either the account doesn't exist or the balance is too low
            throw new SQLException("Removing balance failed, no rows affected.");
        }
    }

    /**
     * Get the UUIDs of the players associated with an account
     *
     * @param accountId the ID of the account
     * @return the UUIDs of the players associated with the account
     */
    public List<UUID> getPlayers(int accountId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT player_uuid FROM players_accounts WHERE account_id = ?;");

        statement.setInt(1, accountId);

        ResultSet resultSet = statement.executeQuery();

        List<UUID> players = new ArrayList<>();

        while (resultSet.next()) {
            players.add(UUID.fromString(resultSet.getString("player_uuid")));
        }

        return players;
    }

    /**
     * Get the accounts associated with a player
     *
     * @param playerUuid the UUID of the player
     * @return the IDs of the accounts associated with the player
     */
    public List<Integer> getAccounts(UUID playerUuid) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT account_id FROM players_accounts WHERE player_uuid = ?;");

        statement.setString(1, playerUuid.toString());

        ResultSet resultSet = statement.executeQuery();

        List<Integer> accounts = new ArrayList<>();

        while (resultSet.next()) {
            accounts.add(resultSet.getInt("account_id"));
        }

        return accounts;
    }
}
