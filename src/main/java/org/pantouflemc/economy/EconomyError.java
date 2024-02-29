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
                return EconomyError.ACCOUNT_NOT_FOUND;
            case PLAYER_NOT_FOUND:
                return EconomyError.PLAYER_NOT_FOUND;
            case ACCOUNT_HAS_NOT_ENOUGH_BALANCE:
            case INVALID_AMOUNT:
                return EconomyError.INVALID_AMOUNT;
            default:
                return EconomyError.UNKNOWN_ERROR;
        }
    }
}
