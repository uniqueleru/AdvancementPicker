package kr.ieruminecraft.advancementpicker.gui;

import kr.ieruminecraft.advancementpicker.AdvancementPicker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.Iterator;

public class AdvancementPickerGUI implements Listener {

    private final AdvancementPicker plugin;
    private final NamespacedKey advancementKey;
    private final NamespacedKey pageKey;
    private final NamespacedKey actionKey;
    
    // 선택된 도전과제를 추적하는 맵
    private final Map<UUID, Set<String>> playerSelections = new HashMap<>();

    // 카테고리 정의
    private final String[] CATEGORIES = {
        "story", "nether", "end", "adventure", "husbandry"
    };
    
    // 카테고리 아이콘
    private final Material[] CATEGORY_ICONS = {
        Material.CRAFTING_TABLE, // story
        Material.NETHERRACK,     // nether
        Material.END_STONE,      // end
        Material.MAP,            // adventure
        Material.HAY_BLOCK       // husbandry
    };

    // 액션 정의
    private static final String ACTION_PREV = "prev";
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_CATEGORY = "category";
    private static final String ACTION_TOGGLE = "toggle";
    private static final String ACTION_BACK = "back";
    private static final String ACTION_SAVE = "save";
    
    // 인벤토리 제목 정의 (식별용)
    private static final String MAIN_TITLE = "도전과제 선택기";
    private static final String CATEGORY_TITLE_FORMAT = "%s 도전과제";
    
    // 디버깅 로그 활성화 여부
    private static final boolean DEBUG_ENABLED = true;

    public AdvancementPickerGUI(AdvancementPicker plugin) {
        this.plugin = plugin;
        this.advancementKey = new NamespacedKey(plugin, "advancement_key");
        this.pageKey = new NamespacedKey(plugin, "page");
        this.actionKey = new NamespacedKey(plugin, "action");
        
        // 이벤트 리스너 등록
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getLogger().info("AdvancementPickerGUI: Event listener registered");
    }
    
    /**
     * 디버그 로그를 출력합니다.
     */
    private void logDebug(String message) {
        if (DEBUG_ENABLED) {
            Bukkit.getLogger().info("[AdvancementPickerGUI] " + message);
        }
    }

    /**
     * 메인 카테고리 선택 GUI를 열어줍니다.
     */
    public void openMainGUI(Player player) {
        // 플레이어 선택 초기화
        if (!playerSelections.containsKey(player.getUniqueId())) {
            Set<String> initialSelections = new HashSet<>(plugin.getConfigManager().getConfig().getStringList("advancements"));
            playerSelections.put(player.getUniqueId(), initialSelections);
        }
        
        Inventory inventory = Bukkit.createInventory(null, 27, 
            Component.text(MAIN_TITLE).color(NamedTextColor.DARK_PURPLE));
        
        // 카테고리 아이템 생성
        for (int i = 0; i < CATEGORIES.length; i++) {
            ItemStack categoryItem = new ItemStack(CATEGORY_ICONS[i]);
            ItemMeta meta = categoryItem.getItemMeta();
            
            // 카테고리 이름 설정
            String categoryName = CATEGORIES[i];
            meta.displayName(Component.text(formatCategoryName(categoryName))
                .color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            
            // 카테고리에 속한 도전과제 수 계산
            Set<String> categoryAdvancements = getAllAdvancementsInCategory(categoryName);
            Set<String> selectedAdvancements = playerSelections.get(player.getUniqueId());
            
            int totalCount = categoryAdvancements.size();
            int selectedCount = 0;
            
            for (String adv : categoryAdvancements) {
                if (selectedAdvancements.contains(adv)) {
                    selectedCount++;
                }
            }
            
            // 로어(설명) 설정
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("클릭하여 " + formatCategoryName(categoryName) + " 도전과제 보기")
                .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(""));
            lore.add(Component.text("총 도전과제: " + totalCount + "개")
                .color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("선택된 도전과제: " + selectedCount + "개")
                .color(selectedCount > 0 ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
            
            meta.lore(lore);
            
            // 액션 저장
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_CATEGORY);
            meta.getPersistentDataContainer().set(advancementKey, PersistentDataType.STRING, categoryName);
            
            categoryItem.setItemMeta(meta);
            inventory.setItem(10 + i, categoryItem);
        }
        
        // 저장 버튼
        ItemStack saveItem = new ItemStack(Material.EMERALD);
        ItemMeta saveMeta = saveItem.getItemMeta();
        saveMeta.displayName(Component.text("저장하기").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        List<Component> saveLore = new ArrayList<>();
        saveLore.add(Component.text("현재 선택된 도전과제를 저장합니다").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        saveMeta.lore(saveLore);
        saveMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_SAVE);
        saveItem.setItemMeta(saveMeta);
        inventory.setItem(22, saveItem);
        
        player.openInventory(inventory);
    }
    
    /**
     * 특정 카테고리의 도전과제를 보여주는 GUI를 열어줍니다.
     */
    public void openCategoryGUI(Player player, String category, int page) {
        // 플레이어 선택 확인
        if (!playerSelections.containsKey(player.getUniqueId())) {
            Set<String> initialSelections = new HashSet<>(plugin.getConfigManager().getConfig().getStringList("advancements"));
            playerSelections.put(player.getUniqueId(), initialSelections);
        }
        
        // 카테고리 내 모든 도전과제 가져오기
        Set<String> categoryAdvancements = getAllAdvancementsInCategory(category);
        
        // 현재 선택된 도전과제 가져오기
        Set<String> selectedAdvancements = playerSelections.get(player.getUniqueId());
        
        // GUI 생성
        Inventory inventory = Bukkit.createInventory(null, 54, 
            Component.text(String.format(CATEGORY_TITLE_FORMAT, formatCategoryName(category))).color(NamedTextColor.DARK_PURPLE));
        
        // 페이징 계산
        int itemsPerPage = 45; // 9x5
        List<String> advancementsList = new ArrayList<>(categoryAdvancements);
        
        // 정렬 (가나다순)
        advancementsList.sort((a, b) -> {
            Component nameA = plugin.getConfigManager().getAdvancementDisplay(a);
            Component nameB = plugin.getConfigManager().getAdvancementDisplay(b);
            return nameA.toString().compareTo(nameB.toString());
        });
        
        int totalPages = (int) Math.ceil((double) advancementsList.size() / itemsPerPage);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, advancementsList.size());
        
        // 도전과제 아이템 배치
        for (int i = startIndex; i < endIndex; i++) {
            String advKey = advancementsList.get(i);
            int slot = (i - startIndex);
            
            boolean isSelected = selectedAdvancements.contains(advKey);
            
            ItemStack advItem = createAdvancementItem(player, advKey, isSelected);
            inventory.setItem(slot, advItem);
        }
        
        // 네비게이션 버튼 추가
        addNavigationButtons(inventory, page, totalPages, category);
        
        // 저장 버튼
        ItemStack saveItem = new ItemStack(Material.EMERALD);
        ItemMeta saveMeta = saveItem.getItemMeta();
        saveMeta.displayName(Component.text("저장하기").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        List<Component> saveLore = new ArrayList<>();
        saveLore.add(Component.text("현재 선택된 도전과제를 저장합니다").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        saveMeta.lore(saveLore);
        saveMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_SAVE);
        saveItem.setItemMeta(saveMeta);
        inventory.setItem(49, saveItem);
        
        // 뒤로가기 버튼
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(Component.text("뒤로 가기").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        backMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_BACK);
        backItem.setItemMeta(backMeta);
        inventory.setItem(45, backItem);
        
        player.openInventory(inventory);
    }
    
    /**
     * 도전과제 아이템을 생성합니다.
     */
    private ItemStack createAdvancementItem(Player player, String advKey, boolean isSelected) {
        Material material = isSelected ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // 도전과제 정보 가져오기
        Component[] advInfo = plugin.getConfigManager().getAdvancementInfo(advKey);
        Component title = advInfo[0];
        Component description = advInfo[1];
        
        meta.displayName(title.color(isSelected ? NamedTextColor.GREEN : NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false));
        
        // 로어(설명) 설정
        List<Component> lore = new ArrayList<>();
        
        // 도전과제 상태
        Advancement advancement = Bukkit.getAdvancement(NamespacedKey.fromString(advKey));
        boolean isCompleted = false;
        
        if (advancement != null) {
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            isCompleted = progress.isDone();
            
            if (!description.equals(Component.empty())) {
                lore.add(description.color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());
            }
            
            lore.add(Component.text("클릭하여 " + (isSelected ? "비활성화" : "활성화"))
                .color(isSelected ? NamedTextColor.RED : NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
                
            lore.add(Component.text("도전과제 상태: " + (isCompleted ? "완료" : "미완료"))
                .color(isCompleted ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("알 수 없는 도전과제")
                .color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        }
        
        meta.lore(lore);
        
        // 액션 및 데이터 저장
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_TOGGLE);
        meta.getPersistentDataContainer().set(advancementKey, PersistentDataType.STRING, advKey);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * 네비게이션 버튼을 추가합니다.
     */
    private void addNavigationButtons(Inventory inventory, int currentPage, int totalPages, String category) {
        // 이전 페이지 버튼
        if (currentPage > 0) {
            ItemStack prevItem = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevItem.getItemMeta();
            prevMeta.displayName(Component.text("이전 페이지").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            List<Component> prevLore = new ArrayList<>();
            prevLore.add(Component.text("페이지 " + currentPage + " / " + (totalPages == 0 ? 1 : totalPages))
                .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            prevMeta.lore(prevLore);
            prevMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_PREV);
            prevMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, currentPage - 1);
            prevMeta.getPersistentDataContainer().set(advancementKey, PersistentDataType.STRING, category);
            prevItem.setItemMeta(prevMeta);
            inventory.setItem(48, prevItem);
        }
        
        // 다음 페이지 버튼
        if (currentPage < totalPages - 1) {
            ItemStack nextItem = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextItem.getItemMeta();
            nextMeta.displayName(Component.text("다음 페이지").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            List<Component> nextLore = new ArrayList<>();
            nextLore.add(Component.text("페이지 " + (currentPage + 2) + " / " + (totalPages == 0 ? 1 : totalPages))
                .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            nextMeta.lore(nextLore);
            nextMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_NEXT);
            nextMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, currentPage + 1);
            nextMeta.getPersistentDataContainer().set(advancementKey, PersistentDataType.STRING, category);
            nextItem.setItemMeta(nextMeta);
            inventory.setItem(50, nextItem);
        }
    }
    
    /**
     * 카테고리명을 포맷팅합니다.
     */
    private String formatCategoryName(String category) {
        switch (category) {
            case "story": return "여정";
            case "nether": return "네더";
            case "end": return "엔드";
            case "adventure": return "모험";
            case "husbandry": return "동물 교배";
            default: return category;
        }
    }
    
    /**
     * 지정된 카테고리에 속한 모든 도전과제를 가져옵니다.
     */
    private Set<String> getAllAdvancementsInCategory(String category) {
        Set<String> result = new HashSet<>();
        
        // 서버에 등록된 모든 도전과제 순회
        Iterator<Advancement> iterator = Bukkit.getServer().advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            String key = advancement.getKey().toString();
            
            // 해당 카테고리에 속하는지 확인
            if (key.startsWith("minecraft:" + category + "/")) {
                result.add(key);
            }
        }
        
        return result;
    }
    
    /**
     * 주어진 도전과제가 어떤 카테고리에 속하는지 확인합니다.
     */
    private boolean isAdvancementInCategory(String advKey, String[] categories) {
        for (String category : categories) {
            if (advKey.startsWith("minecraft:" + category + "/")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 선택된 도전과제 목록을 업데이트합니다.
     */
    private void updatePlayerSelection(Player player, String advKey, boolean isSelected) {
        Set<String> selections = playerSelections.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        
        if (isSelected) {
            selections.add(advKey);
        } else {
            selections.remove(advKey);
        }
        
        logDebug("Updated selection for " + player.getName() + ": " + advKey + " is now " + (isSelected ? "selected" : "unselected"));
    }
    
    /**
     * 인벤토리 제목을 통해 카테고리를 추출합니다.
     * 예: "엔드 도전과제" -> "end"
     */
    private String getCategoryFromTitle(String title) {
        for (String category : CATEGORIES) {
            String formattedName = formatCategoryName(category);
            if (title.startsWith(formattedName)) {
                return category;
            }
        }
        return null;
    }
    
    /**
     * 설정을 저장합니다.
     */
    private void saveSettings(Player player) {
        // 플레이어 선택 가져오기
        Set<String> selectedAdvancements = playerSelections.getOrDefault(player.getUniqueId(), new HashSet<>());
        
        // 기존 선택된 목록 가져오기
        List<String> currentAdvancements = plugin.getConfigManager().getConfig().getStringList("advancements");
        
        // 선택되지 않은 카테고리의 도전과제는 유지
        List<String> finalAdvancements = new ArrayList<>();
        for (String advKey : currentAdvancements) {
            // 이 도전과제가 GUI에서 관리하는 카테고리에 속하지 않는 경우 유지
            if (!isAdvancementInCategory(advKey, CATEGORIES)) {
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
        logDebug("Inventory title: " + titleText);
        
        // 도전과제 선택기 인벤토리인지 확인 (제목에 포함된 문자열로 판단)
        boolean isMainGUI = titleText.contains(MAIN_TITLE);
        boolean isCategoryGUI = false;
        
        // 카테고리 GUI 확인
        String currentCategory = null;
        for (String category : CATEGORIES) {
            String formattedCategory = formatCategoryName(category);
            // 제목에 카테고리명과 "도전과제"가 모두 포함되어 있는지 확인
            if (titleText.contains(formattedCategory) && titleText.contains("도전과제")) {
                isCategoryGUI = true;
                currentCategory = category;
                logDebug("Detected category: " + category);
                break;
            }
        }
        
        // 플러그인의 GUI가 아니면 처리하지 않음
        if (!isMainGUI && !isCategoryGUI) {
            logDebug("Not a plugin GUI: " + titleText);
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
        if (!meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            return;
        }
        
        // 액션 처리
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        logDebug("Player " + player.getName() + " clicked item with action: " + action);
        
        switch (action) {
            case ACTION_CATEGORY:
                // 메인 화면에서 카테고리 선택
                if (isMainGUI) {
                    String category = meta.getPersistentDataContainer().get(advancementKey, PersistentDataType.STRING);
                    if (category != null) {
                        logDebug("Opening category: " + category);
                        openCategoryGUI(player, category, 0);
                    }
                }
                break;
                
            case ACTION_PREV:
            case ACTION_NEXT:
                // 페이지 이동
                if (meta.getPersistentDataContainer().has(pageKey, PersistentDataType.INTEGER)) {
                    int page = meta.getPersistentDataContainer().get(pageKey, PersistentDataType.INTEGER);
                    String category = meta.getPersistentDataContainer().get(advancementKey, PersistentDataType.STRING);
                    if (category != null) {
                        logDebug("Navigating to page: " + page + " in category: " + category);
                        openCategoryGUI(player, category, page);
                    }
                }
                break;
                
            case ACTION_TOGGLE:
                // 도전과제 토글
                if (meta.getPersistentDataContainer().has(advancementKey, PersistentDataType.STRING)) {
                    String advKey = meta.getPersistentDataContainer().get(advancementKey, PersistentDataType.STRING);
                    boolean isCurrentlySelected = clickedItem.getType() == Material.LIME_DYE;
                    
                    // 플레이어 선택 상태 업데이트
                    updatePlayerSelection(player, advKey, !isCurrentlySelected);
                    
                    // 아이템 교체
                    event.getClickedInventory().setItem(
                        event.getSlot(), 
                        createAdvancementItem(player, advKey, !isCurrentlySelected)
                    );
                    
                    logDebug("Selection toggled: " + advKey + " is now " + (!isCurrentlySelected ? "selected" : "unselected"));
                }
                break;
                
            case ACTION_BACK:
                // 뒤로 가기 (카테고리 GUI에서 메인 GUI로)
                logDebug("Going back to main menu");
                openMainGUI(player);
                break;
                
            case ACTION_SAVE:
                // 설정 저장
                logDebug("Saving settings");
                saveSettings(player);
                player.closeInventory();
                break;
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        // 인벤토리 제목 확인
        Component title = event.getView().title();
        String titleText = title.toString();
        
        // 플러그인의 GUI를 완전히 닫는 경우가 아니라면 (다른 GUI로 이동하는 경우) 선택 상태 유지
        // 저장 버튼을 누르지 않고 완전히 닫는 경우에는 변경 사항이 저장되지 않음
        
        // 타이틀 기반으로 GUI 종류 판별  
        boolean isPluginGUI = titleText.contains(MAIN_TITLE);
        if (!isPluginGUI) {
            for (String category : CATEGORIES) {
                String formattedCategory = formatCategoryName(category);
                if (titleText.contains(formattedCategory) && titleText.contains("도전과제")) {
                    isPluginGUI = true;
                    break;
                }
            }
        }
        
        logDebug("Inventory closed by " + player.getName() + ": " + titleText + 
                 (isPluginGUI ? " (plugin GUI)" : ""));
    }
}