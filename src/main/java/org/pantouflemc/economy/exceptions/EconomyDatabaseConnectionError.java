package org.pantouflemc.economy.exceptions;

public class EconomyDatabaseConnectionError extends EconomyDatabaseError {
    public EconomyDatabaseConnectionError() {
        super("An error occurred while connecting to the database");
    }
}
