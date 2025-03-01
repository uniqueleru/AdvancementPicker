package kr.ieruminecraft.advancementpicker;

import kr.ieruminecraft.advancementpicker.commands.AdvancementPickerCommand;
import kr.ieruminecraft.advancementpicker.config.ConfigManager;
import kr.ieruminecraft.advancementpicker.listeners.AdvancementListener;
import kr.ieruminecraft.advancementpicker.managers.AdvancementManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdvancementPicker extends JavaPlugin {

    private static AdvancementPicker instance;
    private ConfigManager configManager;
    private AdvancementManager advancementManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        configManager = new ConfigManager(this);
        advancementManager = new AdvancementManager(this);
        
        // Register events
        getServer().getPluginManager().registerEvents(new AdvancementListener(this), this);
        
        // Register the main command
        AdvancementPickerCommand mainCommand = new AdvancementPickerCommand(this);
        getCommand("advancementpicker").setExecutor(mainCommand);
        getCommand("advancementpicker").setTabCompleter(mainCommand);
        
        getLogger().info(LegacyComponentSerializer.legacySection()
            .serialize(configManager.getMessageComponent("plugin.enabled")));
    }

    @Override
    public void onDisable() {
        getLogger().info(LegacyComponentSerializer.legacySection()
            .serialize(configManager.getMessageComponent("plugin.disabled")));
        instance = null;
    }
    
    public static AdvancementPicker getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public AdvancementManager getAdvancementManager() {
        return advancementManager;
    }
    
    public void reload() {
        configManager.reloadConfigs();
        advancementManager.loadAdvancements();
    }
}