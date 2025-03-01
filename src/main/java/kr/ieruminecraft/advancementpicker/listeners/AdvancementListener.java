package kr.ieruminecraft.advancementpicker.listeners;

import kr.ieruminecraft.advancementpicker.AdvancementPicker;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.HashMap;
import java.util.Map;

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
            Map<String, String> replacements = new HashMap<>();
            replacements.put("%player%", player.getName());
            replacements.put("%advancement%", plugin.getConfigManager().formatAdvancement(advancementKey));
            
            // Get the message as a Component
            Component message = plugin.getConfigManager().getMessageComponent("advancement.completed", replacements);
            
            // Broadcast the message using the non-deprecated method
            Bukkit.broadcast(message);
            
            plugin.getAdvancementManager().removeAdvancement(player);
        }
    }
}