package org.simpmc.simppay.config.types.menu;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import org.bukkit.Material;
import org.simpmc.simppay.config.annotations.Folder;
import org.simpmc.simppay.config.types.data.menu.DisplayItem;
import org.simpmc.simppay.config.types.data.menu.RoleType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@Folder("menus")
public class PaymentHistoryMenuConfig {
    @Comment("Title có hỗ trợ PlaceholderAPI")
    public String title = "<gold>Lịch sử nạp";

    public List<String> layout = Arrays.asList(
            "#########",
            "#OOOOOOO#",
            "#OOOOOOO#",
            "#OOOOOOO#",
            "#WWWWWWW#",
            "###L#R###"
    );

    @Comment({"Map ký tự -> item hiển thị", "'O' là vị trí hiển thị giao dịch"})
    public Map<Character, DisplayItem> displayItems = Map.of(
            '#', DisplayItem.builder()
                    .material(Material.GRAY_STAINED_GLASS_PANE)
                    .role(RoleType.NONE)
                    .name(" ")
                    .amount(1)
                    .build(),

            'L', DisplayItem.builder()
                    .amount(1)
                    .material(Material.ARROW)
                    .role(RoleType.PREV_PAGE)
                    .name("<green>‹ Quay lại")
                    .lores(List.of(" "))
                    .build(),
            'W', DisplayItem.builder()
                    .material(Material.WHITE_STAINED_GLASS_PANE)
                    .amount(1)
                    .role(RoleType.NONE)
                    .name(" ")
                    .build(),
            'R', DisplayItem.builder()
                    .material(Material.ARROW)
                    .amount(1)
                    .role(RoleType.NEXT_PAGE)
                    .name("<green>Tiếp theo ›")
                    .lores(List.of(" "))
                    .build()
    );

    public DisplayItem cardItem = DisplayItem.builder()
            .material(Material.EMERALD)
            .amount(1)
            .name("<white>Thẻ:<color:#24d65d> {card_type} {amount}")
            .lores(List.of(
                    "  <color:#83E349>○<white> Thời gian: <color:#54EDC4>{time}",
                    "  <color:#83E349>○<white> Số serial: <color:#54EDC4>{serial}",
                    "  <color:#83E349>○<white> Mã thẻ: <color:#54EDC4>{pin}",
                    "  <color:#83E349>○<white> Cổng nạp: <color:#54EDC4>{api}",
                    "  <color:#83E349>○<white> Mã giao dịch: <color:#54EDC4>{transaction_id}"
            ))
            .build();

    public DisplayItem bankItem = DisplayItem.builder()
            .material(Material.DIAMOND)
            .amount(1)
            .name("<white>Banking:<color:#24d65d> {amount}")
            .lores(List.of(
                    "  <color:#83E349>○<white> Thời gian: <color:#54EDC4>{time}",
                    "  <color:#83E349>○<white> Cổng nạp: <color:#54EDC4>{api}",
                    "  <color:#83E349>○<white> Mã giao dịch: <color:#54EDC4>{transaction_id}"
            ))
            .build();
}
