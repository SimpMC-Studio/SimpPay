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
public class ServerPaymentHistoryMenuConfig {
    @Comment("Title có hỗ trợ PlaceholderAPI")
    public String title = "<gradient:#E34949:#D8DB5C><bold>SimpPay</bold><white> Lịch Sử Nạp Server";

    public List<String> layout = Arrays.asList(
            "#########",
            "#OOOOOOO#",
            "#OOOOOOO#",
            "#OOOOOOO#",
            "#WWWWWWW#",
            "###L@R###"
    );

    @Comment({"Map ký tự -> item hiển thị", "'O' là vị trí hiển thị giao dịch"})
    public Map<Character, DisplayItem> displayItems = Map.of(
            '#', DisplayItem.builder()
                    .material(Material.GRAY_STAINED_GLASS_PANE)
                    .name(" ")
                    .role(RoleType.NONE)
                    .amount(1)
                    .build(),

            'L', DisplayItem.builder()
                    .amount(1)
                    .material(Material.ARROW)
                    .role(RoleType.PREV_PAGE)
                    .name("<green>‹ Quay lại")
                    .lores(List.of(" "))
                    .build(),

            'R', DisplayItem.builder()
                    .material(Material.ARROW)
                    .amount(1)
                    .role(RoleType.NEXT_PAGE)
                    .name("<green>Tiếp theo ›")
                    .lores(List.of(" "))
                    .build(),
            'W', DisplayItem.builder()
                    .material(Material.WHITE_STAINED_GLASS_PANE)
                    .amount(1)
                    .role(RoleType.NONE)
                    .name(" ")
                    .build(),

            '@', DisplayItem.builder()
                    .material(Material.NETHER_STAR)
                    .amount(1)
                    .role(RoleType.NONE)
                    .name("<yellow><bold>Tổng nạp thẻ của server")
                    .lores(
                            List.of(
                                    "<color:#83E349>○<white> Tổng nạp qua ngân hàng: <green><papi:simppay_bank_total_formatted>",
                                    "<color:#83E349>○<white> Tổng nạp qua thẻ: <green><papi:simppay_card_total_formatted>",
                                    "<color:#83E349>○<white> Tổng nạp toàn server: <green><papi:simppay_server_total_formatted>"
                            )
                    )
                    .build()
    );

    public DisplayItem cardItem = DisplayItem.builder()
            .material(Material.EMERALD)
            .amount(1)
            .name("<white>Thẻ:<color:#24d65d> {card_type} {amount}")
            .lores(List.of(
                    "  <color:#83E349>○<white> Tên: <color:#54EDC4>{name}",
                    " ",
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
                    "  <color:#83E349>○<white> Tên: <color:#54EDC4>{name}",
                    " ",
                    "  <color:#83E349>○<white> Thời gian: <color:#54EDC4>{time}",
                    "  <color:#83E349>○<white> Cổng nạp: <color:#54EDC4>{api}",
                    "  <color:#83E349>○<white> Mã giao dịch: <color:#54EDC4>{transaction_id}"
            ))
            .build();
}
