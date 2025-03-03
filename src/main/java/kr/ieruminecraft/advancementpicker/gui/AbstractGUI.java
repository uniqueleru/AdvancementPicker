package kr.ieruminecraft.advancementpicker.gui;

import kr.ieruminecraft.advancementpicker.AdvancementPicker;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * 모든 GUI 클래스의 기본이 되는 추상 클래스입니다.
 */
public abstract class AbstractGUI {
    protected final AdvancementPicker plugin;
    protected final NamespacedKey advancementKey;
    protected final NamespacedKey pageKey;
    protected final NamespacedKey actionKey;
    
    // 디버깅 로그 활성화 여부
    protected static final boolean DEBUG_ENABLED = true;

    public AbstractGUI(AdvancementPicker plugin) {
        this.plugin = plugin;
        this.advancementKey = new NamespacedKey(plugin, "advancement_key");
        this.pageKey = new NamespacedKey(plugin, "page");
        this.actionKey = new NamespacedKey(plugin, "action");
    }
    
    /**
     * 디버그 로그를 출력합니다.
     */
    protected void logDebug(String message) {
        if (DEBUG_ENABLED) {
            plugin.getLogger().info("[GUI] " + message);
        }
    }
    
    /**
     * 플레이어에게 GUI를 엽니다.
     */
    public abstract void open(Player player);
    
    /**
     * 인벤토리를 생성합니다.
     */
    protected abstract Inventory createInventory();
    
    /**
     * 액션 키를 반환합니다.
     */
    public NamespacedKey getActionKey() {
        return actionKey;
    }
    
    /**
     * 도전과제 키를 반환합니다.
     */
    public NamespacedKey getAdvancementKey() {
        return advancementKey;
    }
    
    /**
     * 페이지 키를 반환합니다.
     */
    public NamespacedKey getPageKey() {
        return pageKey;
    }
}