package org.pantouflemc.economy.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public abstract class EconomyCommandExecutor implements TabExecutor {

    protected final String commandName;
    protected Map<String, EconomyCommandExecutor> subCommands = new HashMap<>();

    protected final Component messageNoPermission = Component
            .text("You don't have permission to use this command")
            .color(NamedTextColor.RED);
    protected final Component messageErrorOccurred = Component
            .text("An error occurred")
            .color(NamedTextColor.RED);
    protected final Component messagePlayerOnly = Component
            .text("This command can only be used by players")
            .color(NamedTextColor.RED);
    protected final Component messageUsage = Component
            .text("Usage: /balance")
            .color(NamedTextColor.RED);

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
            List<String> commandNames = new ArrayList<>();
            for (String commandName : subCommands.keySet()) {
                // Check if the sender has the permission to execute the command
                if (!(sender.hasPermission("economy." + commandName) || sender.hasPermission("economy.*")))
                    continue;
                // Check if the command name starts with the argument
                if (!(args.length == 0 || commandName.startsWith(args[0])))
                    continue;

                commandNames.add(commandName);
            }

            return commandNames;
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

    /**
     * Format a number to a currency string. The number is formatted with the
     * German locale. (e.g. 1.000,00)
     * 
     * @param number the number to format
     */
    protected static @NotNull String formatCurrency(final @NotNull double number) {
        return String.format(Locale.GERMAN, "%,.2f", number);
    }

}
