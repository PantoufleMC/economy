package org.pantouflemc.economy.exceptions;

public class EconomyDatabaseError extends Exception {
    public EconomyDatabaseError() {
        super("An error occurred while interacting with the database");
    }

    public EconomyDatabaseError(String message) {
        super(message);
    }
}
