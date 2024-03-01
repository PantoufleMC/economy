package org.pantouflemc.economy.exceptions;

public class EconomyAccountNotFoundError extends EconomyDatabaseError {
    public EconomyAccountNotFoundError() {
        super("Account not found");
    }
}
