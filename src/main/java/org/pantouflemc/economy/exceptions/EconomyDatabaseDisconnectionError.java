package org.pantouflemc.economy.exceptions;

public class EconomyDatabaseDisconnectionError extends EconomyDatabaseError {
    public EconomyDatabaseDisconnectionError() {
        super("An error occurred while disconnecting from the database");
    }
}
