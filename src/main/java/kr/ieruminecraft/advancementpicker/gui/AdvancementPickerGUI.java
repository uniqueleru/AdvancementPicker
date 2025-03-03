package kr.ieruminecraft.advancementpicker.gui;

import kr.ieruminecraft.advancementpicker.AdvancementPicker;
import org.bukkit.entity.Player;

/**
 * 도전과제 선택 GUI의 진입점 클래스입니다.
 * 기존 코드의 호환성을 위해 유지합니다.
 */
public class AdvancementPickerGUI {

    private final GUIManager guiManager;

    public AdvancementPickerGUI(AdvancementPicker plugin) {
        // GUI 관리자 생성
        this.guiManager = new GUIManager(plugin);
    }
    
    /**
     * 메인 카테고리 선택 GUI를 열어줍니다.
     * 호환성을 위해 내부적으로 새로운 구현을 호출합니다.
     */
    public void openMainGUI(Player player) {
        guiManager.openMainGUI(player);
    }
    
    /**
     * 특정 카테고리의 도전과제를 보여주는 GUI를 열어줍니다.
     * 호환성을 위해 내부적으로 새로운 구현을 호출합니다.
     */
    public void openCategoryGUI(Player player, String category, int page) {
        guiManager.openCategoryGUI(player, category, page);
    }
}