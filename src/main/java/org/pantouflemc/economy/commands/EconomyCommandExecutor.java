package org.pantouflemc.economy.commands;

import org.bukkit.command.CommandExecutor;

public abstract class EconomyCommandExecutor implements CommandExecutor {
    protected final String commandName;

    EconomyCommandExecutor(String name) {
        this.commandName = name;
    }
}
