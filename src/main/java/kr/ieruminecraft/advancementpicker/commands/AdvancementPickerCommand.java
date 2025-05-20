package kr.ieruminecraft.advancementpicker.commands;

import kr.ieruminecraft.advancementpicker.AdvancementPicker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
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
            case "info" -> handleInfoCommand(sender);
            case "reload" -> handleReloadCommand(sender);
            case "config", "gui", "edit" -> handleConfigCommand(sender);
            case "help" -> showHelp(sender);
            default -> {
                sender.sendMessage(plugin.getConfigManager().getMessageComponent("error.unknown-command"));
                return false;
            }
        }

        return true;
    }
    
    private void handleConfigCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessageComponent("error.player-only"));
            return;
        }
        
        if (!player.hasPermission("advancementpicker.config")) {
            player.sendMessage(plugin.getConfigManager().getMessageComponent("error.no-permission"));
            return;
        }
        
        plugin.getAdvancementPickerGUI().openMainGUI(player);
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
        
        // Get advancement info (title and description)
        Component[] advancementInfo = plugin.getConfigManager().getAdvancementInfo(advancementKey);
        Component advancementName = advancementInfo[0];
        Component advancementDescription = advancementInfo[1];
        
        // Add hover event to show description when hovering over the advancement name
        final Component hoverable = !advancementDescription.equals(Component.empty()) && !advancementDescription.equals(Component.text(""))
            ? advancementName.hoverEvent(HoverEvent.showText(advancementDescription))
            : advancementName;
        
        // Get message template and replace placeholder with hoverable component
        Component messageTemplate = plugin.getConfigManager().getMessageComponent("advancement.picked");
        Component message = messageTemplate.replaceText(builder -> 
            builder.matchLiteral("%advancement%").replacement(hoverable));
        
        // Send the main message with hover effect
        player.sendMessage(message);
        
        // Show title and subtitle
        Component titleComponent = plugin.getConfigManager().getMessageComponent("advancement.title");
        Component subtitleTemplate = plugin.getConfigManager().getMessageComponent("advancement.subtitle");
        Component subtitleComponent = subtitleTemplate.replaceText(builder ->
            builder.matchLiteral("%advancement%").replacement(advancementName));
        
        // Display title and subtitle to the player (using times of 10, 70, 20 for fade in, stay, fade out)
        player.showTitle(net.kyori.adventure.title.Title.title(
            titleComponent,
            subtitleComponent,
            net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(500),  // fade in
                java.time.Duration.ofMillis(3500), // stay
                java.time.Duration.ofMillis(1000)  // fade out
            )
        ));
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
        
        // Get advancement info (title and description)
        Component[] advancementInfo = plugin.getConfigManager().getAdvancementInfo(advancementKey);
        Component advancementName = advancementInfo[0];
        Component advancementDescription = advancementInfo[1];
        
        // Add hover event to show description when hovering over the advancement name
        final Component hoverable = !advancementDescription.equals(Component.empty()) && !advancementDescription.equals(Component.text(""))
            ? advancementName.hoverEvent(HoverEvent.showText(advancementDescription))
            : advancementName;
        
        // Get message template and replace placeholder
        Component messageTemplate = plugin.getConfigManager().getMessageComponent("advancement.giveup");
        Component message = messageTemplate.replaceText(builder -> 
            builder.matchLiteral("%advancement%").replacement(hoverable));
        
        player.sendMessage(message);
    }
    
    private void handleInfoCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessageComponent("error.player-only"));
            return;
        }
        
        if (!plugin.getAdvancementManager().hasActiveAdvancement(player)) {
            player.sendMessage(plugin.getConfigManager().getMessageComponent("error.no-active-advancement"));
            return;
        }
        
        // Get player's active advancement
        String advancementKey = plugin.getAdvancementManager().getPlayerAdvancement(player);
        
        // Get advancement info (title and description)
        Component[] advancementInfo = plugin.getConfigManager().getAdvancementInfo(advancementKey);
        Component advancementName = advancementInfo[0];
        Component advancementDescription = advancementInfo[1];
        
        // Add hover event to show description when hovering over the advancement name
        final Component hoverable = !advancementDescription.equals(Component.empty()) && !advancementDescription.equals(Component.text(""))
            ? advancementName.hoverEvent(HoverEvent.showText(advancementDescription))
            : advancementName;
        
        // Get message template and replace placeholder
        Component messageTemplate = plugin.getConfigManager().getMessageComponent("advancement.info");
        Component message = messageTemplate.replaceText(builder -> 
            builder.matchLiteral("%advancement%").replacement(hoverable));
        
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
            subCommands.add("info");
            
            if (sender.hasPermission("advancementpicker.reload")) {
                subCommands.add("reload");
            }
            
            if (sender.hasPermission("advancementpicker.config")) {
                subCommands.add("config");
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