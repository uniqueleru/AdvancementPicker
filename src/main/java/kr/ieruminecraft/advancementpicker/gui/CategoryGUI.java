package kr.ieruminecraft.advancementpicker.gui;

import kr.ieruminecraft.advancementpicker.AdvancementPicker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 카테고리별 도전과제 GUI 클래스입니다.
 */
public class CategoryGUI extends AbstractGUI {

    private final String category;
    private final int page;

    public CategoryGUI(AdvancementPicker plugin, String category, int page) {
        super(plugin);
        this.category = category;
        this.page = Math.max(0, page); // 최소 0 페이지
    }

    @Override
    public void open(Player player) {
        Inventory inventory = createInventory();
        
        // 도전과제 아이템 배치
        addAdvancementItems(inventory, player);
        
        // 네비게이션 버튼 추가 (이전/다음 페이지)
        addNavigationButtons(inventory);
        
        // 저장 버튼
        inventory.setItem(49, GUIUtils.createSaveButton(actionKey));
        
        // 뒤로가기 버튼
        inventory.setItem(45, GUIUtils.createBackButton(actionKey));
        
        player.openInventory(inventory);
    }
    
    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, 
            Component.text(String.format(GUIUtils.CATEGORY_TITLE_FORMAT, GUIUtils.formatCategoryName(category)))
                .color(NamedTextColor.DARK_PURPLE));
    }
    
    /**
     * 도전과제 아이템을 인벤토리에 추가합니다.
     */
    private void addAdvancementItems(Inventory inventory, Player player) {
        // 카테고리 내 모든 도전과제 가져오기
        Set<String> categoryAdvancements = GUIUtils.getAllAdvancementsInCategory(category);
        
        // 현재 선택된 도전과제 가져오기
        Set<String> selectedAdvancements = GUIManager.getPlayerSelections(player.getUniqueId());
        
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
        int currentPage = Math.min(page, Math.max(0, totalPages - 1));
        
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, advancementsList.size());
        
        // 도전과제 아이템 배치
        for (int i = startIndex; i < endIndex; i++) {
            String advKey = advancementsList.get(i);
            int slot = (i - startIndex);
            
            boolean isSelected = selectedAdvancements.contains(advKey);
            
            ItemStack advItem = GUIUtils.createAdvancementItem(plugin, advancementKey, actionKey, player, advKey, isSelected);
            inventory.setItem(slot, advItem);
        }
    }
    
    /**
     * 네비게이션 버튼을 추가합니다.
     */
    private void addNavigationButtons(Inventory inventory) {
        // 카테고리 내 모든 도전과제 가져오기
        Set<String> categoryAdvancements = GUIUtils.getAllAdvancementsInCategory(category);
        
        // 페이징 계산
        int itemsPerPage = 45; // 9x5
        int totalPages = (int) Math.ceil((double) categoryAdvancements.size() / itemsPerPage);
        int currentPage = Math.min(page, Math.max(0, totalPages - 1));
        
        // 네비게이션 버튼 추가
        GUIUtils.addNavigationButtons(plugin, advancementKey, pageKey, actionKey, 
            inventory, currentPage, totalPages, category);
    }
    
    /**
     * 현재 카테고리를 반환합니다.
     */
    public String getCategory() {
        return category;
    }
    
    /**
     * 현재 페이지를 반환합니다.
     */
    public int getPage() {
        return page;
    }
}