package org.pantouflemc.economy.exceptions;

public class EconomyDriverNotFoundException extends Exception {
    public EconomyDriverNotFoundException() {
        super("The database driver was not found");
    }
}
