package org.pantouflemc.economy.exceptions;

public class EconomyInvalidAmountError extends EconomyDatabaseError {
    public EconomyInvalidAmountError() {
        super("Invalid amount");
    }
}
