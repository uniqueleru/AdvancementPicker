package kr.ieruminecraft.advancementpicker.gui;

import kr.ieruminecraft.advancementpicker.AdvancementPicker;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * GUI 시스템을 관리하고 이벤트를 처리하는 클래스입니다.
 */
public class GUIManager implements Listener {

    private final AdvancementPicker plugin;
    private final MainGUI mainGUI;
    
    // 선택된 도전과제를 추적하는 맵
    private static final Map<UUID, Set<String>> playerSelections = new HashMap<>();
    
    /**
     * 생성자
     */
    public GUIManager(AdvancementPicker plugin) {
        this.plugin = plugin;
        this.mainGUI = new MainGUI(plugin);
        
        // 이벤트 리스너 등록
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getLogger().info("GUIManager: Event listener registered");
    }
    
    /**
     * 메인 GUI를 열어줍니다.
     */
    public void openMainGUI(Player player) {
        // 플레이어 선택 초기화 (필요한 경우)
        initializePlayerSelections(player);
        
        // 메인 GUI 열기
        mainGUI.open(player);
    }
    
    /**
     * 카테고리 GUI를 열어줍니다.
     */
    public void openCategoryGUI(Player player, String category, int page) {
        // 플레이어 선택 초기화 (필요한 경우)
        initializePlayerSelections(player);
        
        // 카테고리 GUI 열기
        CategoryGUI categoryGUI = new CategoryGUI(plugin, category, page);
        categoryGUI.open(player);
    }
    
    /**
     * 플레이어 선택 목록을 초기화합니다.
     */
    private void initializePlayerSelections(Player player) {
        if (!playerSelections.containsKey(player.getUniqueId())) {
            Set<String> initialSelections = new HashSet<>(plugin.getConfigManager().getConfig().getStringList("advancements"));
            playerSelections.put(player.getUniqueId(), initialSelections);
        }
    }
    
    /**
     * 플레이어의 선택 목록을 가져옵니다.
     */
    public static Set<String> getPlayerSelections(UUID playerUUID) {
        return playerSelections.computeIfAbsent(playerUUID, k -> new HashSet<>());
    }
    
    /**
     * 선택된 도전과제 목록을 업데이트합니다.
     */
    private void updatePlayerSelection(Player player, String advKey, boolean isSelected) {
        Set<String> selections = getPlayerSelections(player.getUniqueId());
        
        if (isSelected) {
            selections.add(advKey);
        } else {
            selections.remove(advKey);
        }
        
        plugin.getLogger().info("[GUIManager] Updated selection for " + player.getName() + 
                              ": " + advKey + " is now " + (isSelected ? "selected" : "unselected"));
    }
    
    /**
     * 설정을 저장합니다.
     */
    private void saveSettings(Player player) {
        // 플레이어 선택 가져오기
        Set<String> selectedAdvancements = getPlayerSelections(player.getUniqueId());
        
        // 기존 선택된 목록 가져오기
        List<String> currentAdvancements = plugin.getConfigManager().getConfig().getStringList("advancements");
        
        // 선택되지 않은 카테고리의 도전과제는 유지
        List<String> finalAdvancements = new ArrayList<>();
        for (String advKey : currentAdvancements) {
            // 이 도전과제가 GUI에서 관리하는 카테고리에 속하지 않는 경우 유지
            if (!GUIUtils.isAdvancementInAnyCategory(advKey, GUIUtils.CATEGORIES)) {
                finalAdvancements.add(advKey);
            }
        }
        
        // 선택된 도전과제 추가 (중복 없음)
        for (String advKey : selectedAdvancements) {
            if (!finalAdvancements.contains(advKey)) {
                finalAdvancements.add(advKey);
            }
        }
        
        // 설정 파일 업데이트
        plugin.getConfigManager().getConfig().set("advancements", finalAdvancements);
        plugin.saveConfig();
        
        // 도전과제 관리자 리로드
        plugin.getAdvancementManager().loadAdvancements();
        
        // 플레이어 선택 맵에서 제거
        playerSelections.remove(player.getUniqueId());
        
        player.sendMessage(plugin.getConfigManager().getMessageComponent("config.advancements-updated"));
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // 인벤토리의 제목을 확인
        Component title = event.getView().title();
        String titleText = title.toString();
        
        // 도전과제 선택기 인벤토리인지 확인 (제목에 포함된 문자열로 판단)
        boolean isMainGUI = titleText.contains(GUIUtils.MAIN_TITLE);
        boolean isCategoryGUI = false;
        
        // 카테고리 GUI 확인
        for (String category : GUIUtils.CATEGORIES) {
            String formattedCategory = GUIUtils.formatCategoryName(category);
            // 제목에 카테고리명과 "도전과제"가 모두 포함되어 있는지 확인
            if (titleText.contains(formattedCategory) && titleText.contains("도전과제")) {
                isCategoryGUI = true;
                break;
            }
        }
        
        // 플러그인의 GUI가 아니면 처리하지 않음
        if (!isMainGUI && !isCategoryGUI) {
            return;
        }
        
        // 클릭 이벤트 취소 (인벤토리 아이템 이동 방지)
        event.setCancelled(true);
        
        // 클릭된 아이템이 없으면 처리하지 않음
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }
        
        // 액션 키가 없으면 처리하지 않음
        ItemMeta meta = clickedItem.getItemMeta();
        if (!meta.getPersistentDataContainer().has(mainGUI.actionKey, PersistentDataType.STRING)) {
            return;
        }
        
        // 액션 처리
        String action = meta.getPersistentDataContainer().get(mainGUI.actionKey, PersistentDataType.STRING);
        
        switch (action) {
            case GUIUtils.ACTION_CATEGORY:
                // 메인 화면에서 카테고리 선택
                if (isMainGUI) {
                    String category = meta.getPersistentDataContainer().get(mainGUI.advancementKey, PersistentDataType.STRING);
                    if (category != null) {
                        openCategoryGUI(player, category, 0);
                    }
                }
                break;
                
            case GUIUtils.ACTION_PREV:
            case GUIUtils.ACTION_NEXT:
                // 페이지 이동
                if (meta.getPersistentDataContainer().has(mainGUI.pageKey, PersistentDataType.INTEGER)) {
                    int page = meta.getPersistentDataContainer().get(mainGUI.pageKey, PersistentDataType.INTEGER);
                    String category = meta.getPersistentDataContainer().get(mainGUI.advancementKey, PersistentDataType.STRING);
                    if (category != null) {
                        openCategoryGUI(player, category, page);
                    }
                }
                break;
                
            case GUIUtils.ACTION_TOGGLE:
                // 도전과제 토글
                if (meta.getPersistentDataContainer().has(mainGUI.advancementKey, PersistentDataType.STRING)) {
                    String advKey = meta.getPersistentDataContainer().get(mainGUI.advancementKey, PersistentDataType.STRING);
                    boolean isCurrentlySelected = clickedItem.getType() == Material.LIME_DYE;
                    
                    // 플레이어 선택 상태 업데이트
                    updatePlayerSelection(player, advKey, !isCurrentlySelected);
                    
                    // 아이템 교체
                    ItemStack newItem = GUIUtils.createAdvancementItem(
                        plugin, mainGUI.advancementKey, mainGUI.actionKey, 
                        player, advKey, !isCurrentlySelected
                    );
                    event.getClickedInventory().setItem(event.getSlot(), newItem);
                }
                break;
                
            case GUIUtils.ACTION_BACK:
                // 뒤로 가기 (카테고리 GUI에서 메인 GUI로)
                openMainGUI(player);
                break;
                
            case GUIUtils.ACTION_SAVE:
                // 설정 저장
                saveSettings(player);
                player.closeInventory();
                break;
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // 플러그인의 GUI를 완전히 닫는 경우가 아니라면 (다른 GUI로 이동하는 경우) 선택 상태 유지
        // 저장 버튼을 누르지 않고 완전히 닫는 경우에는 변경 사항이 저장되지 않음
    }
}