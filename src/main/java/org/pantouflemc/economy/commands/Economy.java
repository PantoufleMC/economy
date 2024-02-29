package org.pantouflemc.economy.commands;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class Economy implements CommandExecutor {

    protected Map<String, CommandExecutor> subCommands = new HashMap<>();
    protected final String commandName = "economy";

    public Economy(JavaPlugin plugin) {
        plugin.getCommand(this.commandName).setExecutor(this);
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

    /**
     * Register a sub-command, can be chained
     * 
     * @param name     the name of the sub-command
     * @param executor the executor of the sub-command
     * @param plugin   the plugin to register the command to
     * @return this
     */
    public Economy registerSubCommand(String name, CommandExecutor executor, JavaPlugin plugin) {
        plugin.getCommand(this.commandName + "." + name).setExecutor(executor);
        subCommands.put(name, executor);
        return this;
    }

}
