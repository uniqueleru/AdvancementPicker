package kr.ieruminecraft.advancementpicker.config;

import kr.ieruminecraft.advancementpicker.AdvancementPicker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

public class ConfigManager {

    private final AdvancementPicker plugin;
    private FileConfiguration config;
    private FileConfiguration langConfig;
    private File langFile;

    public ConfigManager(AdvancementPicker plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    private void loadConfigs() {
        // Save default config
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        // Set up language file
        langFile = new File(plugin.getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getLangConfig() {
        return langConfig;
    }

    /**
     * Get a message as a Component from the language file
     *
     * @param path The path to the message in the language file
     * @return The message as a Component
     */
    public Component getMessageComponent(String path) {
        String message = langConfig.getString(path);
        if (message == null) {
            message = "Missing message: " + path;
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
    
    /**
     * Get a message as a Component from the language file with replacements
     *
     * @param path The path to the message in the language file
     * @param replacements A map of replacements to apply to the message
     * @return The message as a Component with replacements applied
     * @deprecated Use {@link #getMessageComponent(String)} and Component.replaceText() instead
     */
    @Deprecated
    public Component getMessageComponent(String path, Map<String, String> replacements) {
        String message = langConfig.getString(path);
        if (message == null) {
            message = "Missing message: " + path;
        }
        
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
    
    /**
     * @deprecated Use {@link #getMessageComponent(String)} instead
     */
    @Deprecated
    public String getMessage(String path) {
        String message = langConfig.getString(path);
        if (message == null) {
            return "Missing message: " + path;
        }
        return message.replace("&", "§");
    }
    
    /**
     * @deprecated Use {@link #getMessageComponent(String, Map)} instead
     */
    @Deprecated
    public String getMessage(String path, Map<String, String> replacements) {
        String message = getMessage(path);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        return message;
    }
    
    /**
     * @deprecated Use {@link #getAdvancementDisplay(String)} instead
     */
    @Deprecated
    public String formatAdvancement(String advancementKey) {
        String formatted = config.getString("advancement-names." + advancementKey);
        return formatted != null ? formatted : advancementKey;
    }
    
    /**
     * Gets the official translated display name of an advancement
     * 
     * @param advancementKey The advancement key
     * @return The translated display name
     */
    public Component getAdvancementDisplay(String advancementKey) {
        // Try to get the advancement from the server
        org.bukkit.advancement.Advancement advancement = Bukkit.getAdvancement(NamespacedKey.fromString(advancementKey));
        
        if (advancement != null && advancement.getDisplay() != null) {
            // Return the official translation of the advancement
            return advancement.getDisplay().title();
        }
        
        // Fallback to config if the advancement is not found or has no display
        String fallback = config.getString("advancement-names." + advancementKey);
        if (fallback != null) {
            return LegacyComponentSerializer.legacySection().deserialize(fallback);
        }
        
        // Last resort: return the key itself
        return Component.text(advancementKey);
    }
}