package kr.ieruminecraft.advancementpicker.listeners;

import kr.ieruminecraft.advancementpicker.AdvancementPicker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class AdvancementListener implements Listener {

    private final AdvancementPicker plugin;

    public AdvancementListener(AdvancementPicker plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        String advancementKey = event.getAdvancement().getKey().toString();
        
        if (plugin.getAdvancementManager().hasActiveAdvancement(player) &&
            plugin.getAdvancementManager().getPlayerAdvancement(player).equals(advancementKey)) {
            
            // Player completed their picked advancement
            Component playerName = Component.text(player.getName());
            Component[] advancementInfo = plugin.getConfigManager().getAdvancementInfo(advancementKey);
            Component advancementName = advancementInfo[0];
            Component advancementDescription = advancementInfo[1];
            
            // Add hover event to show description when hovering over the advancement name
            Component hoverable = advancementName;
            if (advancementDescription != Component.empty()) {
                hoverable = advancementName.hoverEvent(HoverEvent.showText(advancementDescription));
            }
            
            // Get the message template
            Component messageTemplate = plugin.getConfigManager().getMessageComponent("advancement.completed");
            
            // Replace placeholders with actual values
            Component message = messageTemplate
                .replaceText(builder -> builder.matchLiteral("%player%").replacement(playerName))
                .replaceText(builder -> builder.matchLiteral("%advancement%").replacement(hoverable));
            
            // Broadcast the message using the non-deprecated method
            Bukkit.broadcast(message);
            
            plugin.getAdvancementManager().removeAdvancement(player);
        }
    }
}