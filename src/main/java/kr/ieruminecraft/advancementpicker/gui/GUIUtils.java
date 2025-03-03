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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * GUI 관련 유틸리티 메서드를 제공하는 클래스입니다.
 */
public class GUIUtils {

    // 액션 정의
    public static final String ACTION_PREV = "prev";
    public static final String ACTION_NEXT = "next";
    public static final String ACTION_CATEGORY = "category";
    public static final String ACTION_TOGGLE = "toggle";
    public static final String ACTION_BACK = "back";
    public static final String ACTION_SAVE = "save";
    
    // 인벤토리 제목 정의 (식별용)
    public static final String MAIN_TITLE = "도전과제 선택기";
    public static final String CATEGORY_TITLE_FORMAT = "%s 도전과제";
    
    // 카테고리 정의
    public static final String[] CATEGORIES = {
        "story", "nether", "end", "adventure", "husbandry"
    };
    
    // 카테고리 아이콘
    public static final Material[] CATEGORY_ICONS = {
        Material.CRAFTING_TABLE, // story
        Material.NETHERRACK,     // nether
        Material.END_STONE,      // end
        Material.MAP,            // adventure
        Material.HAY_BLOCK       // husbandry
    };

    /**
     * 카테고리명을 포맷팅합니다.
     */
    public static String formatCategoryName(String category) {
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
    public static Set<String> getAllAdvancementsInCategory(String category) {
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
    public static boolean isAdvancementInCategory(String advKey, String category) {
        return advKey.startsWith("minecraft:" + category + "/");
    }
    
    /**
     * 주어진 도전과제가 어떤 카테고리에 속하는지 확인합니다.
     */
    public static boolean isAdvancementInAnyCategory(String advKey, String[] categories) {
        for (String category : categories) {
            if (isAdvancementInCategory(advKey, category)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 도전과제 아이템을 생성합니다.
     */
    public static ItemStack createAdvancementItem(AdvancementPicker plugin, NamespacedKey advancementKey, 
                                                 NamespacedKey actionKey, Player player, 
                                                 String advKey, boolean isSelected) {
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
    public static void addNavigationButtons(AdvancementPicker plugin, NamespacedKey advancementKey, 
                                           NamespacedKey pageKey, NamespacedKey actionKey,
                                           Inventory inventory, int currentPage, 
                                           int totalPages, String category) {
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
     * 저장 버튼을 생성합니다.
     */
    public static ItemStack createSaveButton(NamespacedKey actionKey) {
        ItemStack saveItem = new ItemStack(Material.EMERALD);
        ItemMeta saveMeta = saveItem.getItemMeta();
        saveMeta.displayName(Component.text("저장하기").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        List<Component> saveLore = new ArrayList<>();
        saveLore.add(Component.text("현재 선택된 도전과제를 저장합니다").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        saveMeta.lore(saveLore);
        saveMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_SAVE);
        saveItem.setItemMeta(saveMeta);
        return saveItem;
    }
    
    /**
     * 뒤로가기 버튼을 생성합니다.
     */
    public static ItemStack createBackButton(NamespacedKey actionKey) {
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(Component.text("뒤로 가기").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        backMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_BACK);
        backItem.setItemMeta(backMeta);
        return backItem;
    }
}