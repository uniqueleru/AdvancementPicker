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

public class AdvancementPickerGUI implements Listener {

    private final AdvancementPicker plugin;
    private final Map<UUID, AdvancementGUISession> activeSessions = new HashMap<>();
    private final NamespacedKey advancementKey;
    private final NamespacedKey sessionPageKey;
    private final NamespacedKey actionKey;

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

    public AdvancementPickerGUI(AdvancementPicker plugin) {
        this.plugin = plugin;
        this.advancementKey = new NamespacedKey(plugin, "advancement_key");
        this.sessionPageKey = new NamespacedKey(plugin, "session_page");
        this.actionKey = new NamespacedKey(plugin, "action");
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
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
            Component.text("도전과제 선택기").color(NamedTextColor.DARK_PURPLE));
        
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
        
        // 세션 정보 저장
        AdvancementGUISession session = new AdvancementGUISession();
        session.setCurrentCategory(null);
        session.setCurrentPage(0);
        session.setInventory(inventory);
        
        activeSessions.put(player.getUniqueId(), session);
        
        player.openInventory(inventory);
    }
    
    /**
     * 특정 카테고리의 도전과제를 보여주는 GUI를 열어줍니다.
     */
    public void openCategoryGUI(Player player, String category, int page) {
        AdvancementGUISession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            session = new AdvancementGUISession();
            activeSessions.put(player.getUniqueId(), session);
        }
        
        session.setCurrentCategory(category);
        session.setCurrentPage(page);
        
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
            Component.text(formatCategoryName(category) + " 도전과제").color(NamedTextColor.DARK_PURPLE));
        
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
        addNavigationButtons(inventory, page, totalPages);
        
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
        
        // 세션 업데이트
        session.setInventory(inventory);
        
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
    private void addNavigationButtons(Inventory inventory, int currentPage, int totalPages) {
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
            prevMeta.getPersistentDataContainer().set(sessionPageKey, PersistentDataType.INTEGER, currentPage - 1);
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
            nextMeta.getPersistentDataContainer().set(sessionPageKey, PersistentDataType.INTEGER, currentPage + 1);
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
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
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
    
    // 선택된 도전과제를 추적하는 맵
    private final Map<UUID, Set<String>> playerSelections = new HashMap<>();
    
    /**
     * 설정을 저장합니다.
     */
    private void saveSettings(Player player) {
        // 현재 구성 가져오기
        Set<String> selectedAdvancements = playerSelections.getOrDefault(player.getUniqueId(), new HashSet<>());
        
        // 현재 열려있는 인벤토리에서 선택된 항목도 추가
        AdvancementGUISession session = activeSessions.get(player.getUniqueId());
        if (session != null && session.getInventory() != null) {
            Inventory inv = session.getInventory();
            
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType() == Material.LIME_DYE) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.getPersistentDataContainer().has(advancementKey, PersistentDataType.STRING)) {
                        String key = meta.getPersistentDataContainer().get(advancementKey, PersistentDataType.STRING);
                        selectedAdvancements.add(key);
                    }
                }
            }
        }
        
        // 기존 선택된 목록 가져오기
        List<String> currentAdvancements = plugin.getConfigManager().getConfig().getStringList("advancements");
        
        // 선택되지 않은 카테고리의 도전과제는 유지
        List<String> finalAdvancements = new ArrayList<>();
        for (String advKey : currentAdvancements) {
            boolean isCategoryAdvancement = false;
            for (String category : CATEGORIES) {
                if (advKey.startsWith("minecraft:" + category + "/")) {
                    isCategoryAdvancement = true;
                    break;
                }
            }
            
            if (!isCategoryAdvancement) {
                finalAdvancements.add(advKey);
            }
        }
        
        // 선택된 도전과제 추가
        finalAdvancements.addAll(selectedAdvancements);
        
        // 설정 파일 업데이트
        plugin.getConfigManager().getConfig().set("advancements", finalAdvancements);
        plugin.saveConfig();
        
        // 도전과제 관리자 리로드
        plugin.getAdvancementManager().loadAdvancements();
        
        // 플레이어 선택 맵에서 제거 (세션 종료)
        playerSelections.remove(player.getUniqueId());
        
        player.sendMessage(plugin.getConfigManager().getMessageComponent("config.advancements-updated"));
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        if (!activeSessions.containsKey(player.getUniqueId())) return;
        
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        
        ItemMeta meta = clickedItem.getItemMeta();
        if (!meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) return;
        
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        
        switch (action) {
            case ACTION_CATEGORY:
                String category = meta.getPersistentDataContainer().get(advancementKey, PersistentDataType.STRING);
                openCategoryGUI(player, category, 0);
                break;
                
            case ACTION_PREV:
            case ACTION_NEXT:
                if (meta.getPersistentDataContainer().has(sessionPageKey, PersistentDataType.INTEGER)) {
                    int page = meta.getPersistentDataContainer().get(sessionPageKey, PersistentDataType.INTEGER);
                    AdvancementGUISession session = activeSessions.get(player.getUniqueId());
                    if (session != null && session.getCurrentCategory() != null) {
                        openCategoryGUI(player, session.getCurrentCategory(), page);
                    }
                }
                break;
                
            case ACTION_TOGGLE:
                if (meta.getPersistentDataContainer().has(advancementKey, PersistentDataType.STRING)) {
                    String advKey = meta.getPersistentDataContainer().get(advancementKey, PersistentDataType.STRING);
                    boolean isCurrentlySelected = clickedItem.getType() == Material.LIME_DYE;
                    
                    // 플레이어 선택 상태 업데이트
                    Set<String> selections = playerSelections.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
                    
                    if (isCurrentlySelected) {
                        selections.remove(advKey);
                    } else {
                        selections.add(advKey);
                    }
                    
                    // 아이템 교체
                    event.getClickedInventory().setItem(
                        event.getSlot(), 
                        createAdvancementItem(player, advKey, !isCurrentlySelected)
                    );
                }
                break;
                
            case ACTION_BACK:
                openMainGUI(player);
                break;
                
            case ACTION_SAVE:
                saveSettings(player);
                player.closeInventory();
                break;
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            // 세션 정리 (메모리 관리)
            activeSessions.remove(player.getUniqueId());
            
            // 플레이어가 인벤토리를 닫을 때 선택 상태 저장하지 않음
            // 저장 버튼을 누르지 않고 닫으면 변경 사항 저장 안 됨
        }
    }
    
    /**
     * GUI 세션 정보를 관리하는 내부 클래스
     */
    private static class AdvancementGUISession {
        private String currentCategory;
        private int currentPage;
        private Inventory inventory;
        
        public String getCurrentCategory() {
            return currentCategory;
        }
        
        public void setCurrentCategory(String currentCategory) {
            this.currentCategory = currentCategory;
        }
        
        public int getCurrentPage() {
            return currentPage;
        }
        
        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }
        
        public Inventory getInventory() {
            return inventory;
        }
        
        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }
}