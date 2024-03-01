package org.pantouflemc.economy.commands;

import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.pantouflemc.economy.Economy;
import org.pantouflemc.economy.exceptions.EconomyAccountNotFoundError;
import org.pantouflemc.economy.exceptions.EconomyInsufficientBalance;
import org.pantouflemc.economy.exceptions.EconomyInvalidAmountError;

public class EconomyPayCommand extends EconomyCommandExecutor {

    public EconomyPayCommand() {
        super("pay");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        try {
            // Check if the sender is a player
            if (!(sender instanceof Player)) {
                return false;
            }

            // Get the player
            Player player = (Player) sender;

            // Parse the arguments
            if (args.length != 2)
                return false;
            String targetName = args[0];
            double amount = Double.parseDouble(args[1]);

            // Get the target player
            @Nullable
            OfflinePlayer targetPlayer = sender.getServer().getOfflinePlayerIfCached(targetName);
            if (targetPlayer == null) {
                throw new EconomyAccountNotFoundError();
            }

            // Check if the target player is the same as the sender
            if (player.getUniqueId().equals(targetPlayer.getUniqueId())) {
                sender.sendMessage("You cannot transfer money to yourself");
                return false;
            }

            // Transfer the balance
            Economy.getPlugin().transferMoney(player.getUniqueId(), targetPlayer.getUniqueId(), amount);

            sender.sendMessage("$" + amount + " transferred to " + targetPlayer.getName());

            return true;
        } catch (EconomyAccountNotFoundError e) {
            sender.sendMessage("Target not found");
            return false;
        } catch (EconomyInsufficientBalance e) {
            sender.sendMessage("You do not have enough balance");
            return false;
        } catch (EconomyInvalidAmountError e) {
            sender.sendMessage("Amount must be positive");
            return false;
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid amount");
            return false;
        } catch (Exception e) {
            sender.sendMessage("An error occurred");
            return false;
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return Economy.getPlugin().getServer().getOnlinePlayers().stream()
                    .map(player -> player.getName())
                    .toList();
        }

        if (args.length == 2) {
            return List.of("100", "1000", "10000");
        }

        return List.of();
    }

}
