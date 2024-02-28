package org.pantouflemc.economy;

import org.pantouflemc.economy.database.DatabaseError;

/**
 * An error that can occur when interacting with {@link Economy}.
 */
public enum EconomyError {
    /**
     * The account was not found in the database.
     */
    ACCOUNT_NOT_FOUND,

    /**
     * The player was not found in the database.
     */
    PLAYER_NOT_FOUND,

    /**
     * The amount is invalid. For example, it is negative.
     */
    INVALID_AMOUNT,

    /**
     * The account has not enough balance to perform the operation.
     */
    INSUFFICIENT_BALANCE,

    /**
     * An unknown error occurred.
     */
    UNKNOWN_ERROR;

    /**
     * Convert a {@link DatabaseError} to an {@link EconomyError}.
     * 
     * @param error The database error to convert.
     * @return The converted economy error.
     */
    public static EconomyError valueOf(DatabaseError error) {
        switch (error) {
            case ACCOUNT_NOT_FOUND:
            case PLAYER_DOESNT_HAVE_ACCOUNT:
                return EconomyError.ACCOUNT_NOT_FOUND;
            case PLAYER_NOT_FOUND:
                return EconomyError.PLAYER_NOT_FOUND;
            case INVALID_AMOUNT:
                return EconomyError.INVALID_AMOUNT;
            case ACCOUNT_HAS_NOT_ENOUGH_BALANCE:
                return EconomyError.INSUFFICIENT_BALANCE;
            default:
                return EconomyError.UNKNOWN_ERROR;
        }
    }
}
