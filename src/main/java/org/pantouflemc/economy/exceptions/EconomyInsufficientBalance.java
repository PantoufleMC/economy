package org.pantouflemc.economy.exceptions;

public class EconomyInsufficientBalance extends EconomyDatabaseError {
    public EconomyInsufficientBalance() {
        super("Insufficient balance to perform the operation");
    }
}
