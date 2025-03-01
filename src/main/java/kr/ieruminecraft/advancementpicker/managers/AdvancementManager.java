package kr.ieruminecraft.advancementpicker.managers;

import kr.ieruminecraft.advancementpicker.AdvancementPicker;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class AdvancementManager {

    private final AdvancementPicker plugin;
    private final Map<UUID, String> playerAdvancements = new HashMap<>();
    private final Random random = new Random();
    private List<String> advancementList;

    public AdvancementManager(AdvancementPicker plugin) {
        this.plugin = plugin;
        loadAdvancements();
    }

    public void loadAdvancements() {
        advancementList = plugin.getConfigManager().getConfig().getStringList("advancements");
        if (advancementList.isEmpty()) {
            plugin.getLogger().warning("No advancements found in config.yml");
        }
    }

    public boolean hasActiveAdvancement(Player player) {
        return playerAdvancements.containsKey(player.getUniqueId());
    }

    public String getPlayerAdvancement(Player player) {
        return playerAdvancements.get(player.getUniqueId());
    }

    public String pickRandomAdvancement() {
        if (advancementList.isEmpty()) {
            return null;
        }
        return advancementList.get(random.nextInt(advancementList.size()));
    }

    public void assignAdvancement(Player player, String advancementKey) {
        playerAdvancements.put(player.getUniqueId(), advancementKey);
    }

    public String removeAdvancement(Player player) {
        return playerAdvancements.remove(player.getUniqueId());
    }

    public boolean isAdvancementEmpty() {
        return advancementList.isEmpty();
    }
}