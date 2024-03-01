package org.pantouflemc.economy.commands;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

public abstract class EconomyCommandExecutor implements TabExecutor {

    protected final String commandName;
    protected Map<String, EconomyCommandExecutor> subCommands = new HashMap<>();

    EconomyCommandExecutor(String name) {
        this.commandName = name;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0) {
            return false;
        }

        CommandExecutor executor = subCommands.get(args[0]);
        if (executor == null) {
            return false;
        }

        return executor.onCommand(sender, command, label, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return List.copyOf(subCommands.keySet());
        }

        EconomyCommandExecutor executor = subCommands.get(args[0]);
        if (executor == null) {
            return List.of();
        }

        return executor.onTabComplete(sender, command, alias, Arrays.copyOfRange(args, 1, args.length));
    }

    /**
     * Get the name of the command
     *
     * @return the name of the command
     */
    public String getCommandName() {
        return this.commandName;
    }

    /**
     * Register a sub-command
     *
     * @param executor the executor of the sub-command
     */
    public void registerSubCommand(EconomyCommandExecutor executor) {
        subCommands.put(executor.getCommandName(), executor);
    }

}
