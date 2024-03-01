package org.pantouflemc.economy.exceptions;

public class EconomyIllegalDatabaseEngine extends Exception {
    public EconomyIllegalDatabaseEngine() {
        super("The database engine is not supported by the plugin.");
    }
}
