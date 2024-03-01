package org.pantouflemc.economy;

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
}
