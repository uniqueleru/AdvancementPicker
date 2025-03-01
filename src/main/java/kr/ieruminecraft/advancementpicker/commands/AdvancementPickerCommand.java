package kr.ieruminecraft.advancementpicker.commands;

import kr.ieruminecraft.advancementpicker.AdvancementPicker;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AdvancementPickerCommand implements CommandExecutor, TabCompleter {

    private final AdvancementPicker plugin;

    public AdvancementPickerCommand(AdvancementPicker plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            // No subcommand provided, show help
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "pick" -> handlePickCommand(sender);
            case "giveup" -> handleGiveUpCommand(sender);
            case "reload" -> handleReloadCommand(sender);
            case "help" -> showHelp(sender);
            default -> {
                sender.sendMessage(plugin.getConfigManager().getMessageComponent("error.unknown-command"));
                return false;
            }
        }

        return true;
    }

    private void handlePickCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessageComponent("error.player-only"));
            return;
        }
        
        if (plugin.getAdvancementManager().isAdvancementEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessageComponent("error.no-advancements"));
            return;
        }
        
        if (plugin.getAdvancementManager().hasActiveAdvancement(player)) {
            player.sendMessage(plugin.getConfigManager().getMessageComponent("error.already-has-advancement"));
            return;
        }
        
        // Pick a random advancement
        String advancementKey = plugin.getAdvancementManager().pickRandomAdvancement();
        plugin.getAdvancementManager().assignAdvancement(player, advancementKey);
        
        // Get advancement display name using official translation
        Component advancementName = plugin.getConfigManager().getAdvancementDisplay(advancementKey);
        
        // Get message template and replace placeholder
        Component messageTemplate = plugin.getConfigManager().getMessageComponent("advancement.picked");
        Component message = messageTemplate.replaceText(builder -> 
            builder.matchLiteral("%advancement%").replacement(advancementName));
        
        player.sendMessage(message);
    }

    private void handleGiveUpCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessageComponent("error.player-only"));
            return;
        }
        
        if (!plugin.getAdvancementManager().hasActiveAdvancement(player)) {
            player.sendMessage(plugin.getConfigManager().getMessageComponent("error.no-active-advancement"));
            return;
        }
        
        String advancementKey = plugin.getAdvancementManager().removeAdvancement(player);
        
        // Get advancement display name using official translation
        Component advancementName = plugin.getConfigManager().getAdvancementDisplay(advancementKey);
        
        // Get message template and replace placeholder
        Component messageTemplate = plugin.getConfigManager().getMessageComponent("advancement.giveup");
        Component message = messageTemplate.replaceText(builder -> 
            builder.matchLiteral("%advancement%").replacement(advancementName));
        
        player.sendMessage(message);
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("advancementpicker.reload")) {
            sender.sendMessage(plugin.getConfigManager().getMessageComponent("error.no-permission"));
            return;
        }
        
        plugin.reload();
        sender.sendMessage(plugin.getConfigManager().getMessageComponent("config.reloaded"));
    }

    private void showHelp(CommandSender sender) {
        Component helpMessage = plugin.getConfigManager().getMessageComponent("command.help");
        sender.sendMessage(helpMessage);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("pick");
            subCommands.add("giveup");
            
            if (sender.hasPermission("advancementpicker.reload")) {
                subCommands.add("reload");
            }
            
            subCommands.add("help");
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        }
        
        return completions;
    }
}