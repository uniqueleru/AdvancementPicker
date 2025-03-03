package kr.ieruminecraft.advancementpicker.gui;

import kr.ieruminecraft.advancementpicker.AdvancementPicker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 메인 카테고리 선택 GUI 클래스입니다.
 */
public class MainGUI extends AbstractGUI {

    public MainGUI(AdvancementPicker plugin) {
        super(plugin);
    }

    @Override
    public void open(Player player) {
        Inventory inventory = createInventory();
        createCategoryButtons(inventory, player);
        
        // 저장 버튼 추가
        inventory.setItem(22, GUIUtils.createSaveButton(actionKey));
        
        player.openInventory(inventory);
    }
    
    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, 
            Component.text(GUIUtils.MAIN_TITLE).color(NamedTextColor.DARK_PURPLE));
    }
    
    /**
     * 각 카테고리 버튼을 생성합니다.
     */
    private void createCategoryButtons(Inventory inventory, Player player) {
        Set<String> selectedAdvancements = GUIManager.getPlayerSelections(player.getUniqueId());
        
        // 카테고리 아이템 생성
        for (int i = 0; i < GUIUtils.CATEGORIES.length; i++) {
            String categoryName = GUIUtils.CATEGORIES[i];
            ItemStack categoryItem = createCategoryButton(categoryName, player, selectedAdvancements);
            inventory.setItem(10 + i, categoryItem);
        }
    }
    
    /**
     * 단일 카테고리 버튼을 생성합니다.
     */
    private ItemStack createCategoryButton(String categoryName, Player player, Set<String> selectedAdvancements) {
        ItemStack categoryItem = new ItemStack(GUIUtils.CATEGORY_ICONS[getCategoryIndex(categoryName)]);
        ItemMeta meta = categoryItem.getItemMeta();
        
        // 카테고리 이름 설정
        meta.displayName(Component.text(GUIUtils.formatCategoryName(categoryName))
            .color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        
        // 카테고리에 속한 도전과제 수 계산
        Set<String> categoryAdvancements = GUIUtils.getAllAdvancementsInCategory(categoryName);
        
        int totalCount = categoryAdvancements.size();
        int selectedCount = 0;
        
        for (String adv : categoryAdvancements) {
            if (selectedAdvancements.contains(adv)) {
                selectedCount++;
            }
        }
        
        // 로어(설명) 설정
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("클릭하여 " + GUIUtils.formatCategoryName(categoryName) + " 도전과제 보기")
            .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("총 도전과제: " + totalCount + "개")
            .color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("선택된 도전과제: " + selectedCount + "개")
            .color(selectedCount > 0 ? NamedTextColor.GREEN : NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        
        // 액션 저장
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, GUIUtils.ACTION_CATEGORY);
        meta.getPersistentDataContainer().set(advancementKey, PersistentDataType.STRING, categoryName);
        
        categoryItem.setItemMeta(meta);
        return categoryItem;
    }
    
    /**
     * 카테고리 인덱스를 가져옵니다.
     */
    private int getCategoryIndex(String category) {
        for (int i = 0; i < GUIUtils.CATEGORIES.length; i++) {
            if (GUIUtils.CATEGORIES[i].equals(category)) {
                return i;
            }
        }
        return 0; // 기본값 (story)
    }
}