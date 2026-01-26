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
public class StreakMenuConfig {
    @Comment("Title supports PlaceholderAPI")
    public String title = "<red><bold>Chuỗi nạp thẻ</bold></red>";

    @Comment("Layout structure: '#' = border, 's' = streak info, 'O' = paginated rewards, 'L' = prev page, 'R' = next page")
    public List<String> layout = Arrays.asList(
            "#########",
            "####s####",
            "##OOOOO##",
            "##OOOOO##",
            "###L#R###"
    );

    @Comment("Map of characters to display items")
    public Map<Character, DisplayItem> displayItems = Map.of(
            '#', DisplayItem.builder()
                    .material(Material.GRAY_STAINED_GLASS_PANE)
                    .role(RoleType.NONE)
                    .name(" ")
                    .amount(1)
                    .build(),

            'L', DisplayItem.builder()
                    .material(Material.ARROW)
                    .role(RoleType.PREV_PAGE)
                    .name("<green>« Trang trước")
                    .amount(1)
                    .build(),

            'R', DisplayItem.builder()
                    .material(Material.ARROW)
                    .role(RoleType.NEXT_PAGE)
                    .name("<green>Trang sau »")
                    .amount(1)
                    .build()
    );

    @Comment("Streak info item (fixed position, visible on all pages)")
    public StreakInfoItem streakInfoItem = new StreakInfoItem();

    @Comment("Reward item templates for different statuses")
    public RewardItems rewardItems = new RewardItems();

    @Configuration
    public static class StreakInfoItem {
        public Material material = Material.NETHER_STAR;
        public String name = "<gold><bold>Chuỗi của bạn</bold></gold>";

        @Comment("Placeholders: {current_streak}, {best_streak}, {last_payment}")
        public List<String> lore = Arrays.asList(
                "<gray>Chuỗi nạp thẻ",
                "",
                "<yellow>Chuỗi hiện tại: <gold>{current_streak}</gold> days",
                "<yellow>Chuỗi dài nhất: <gold>{best_streak}</gold> days",
                ""
        );
    }

    @Configuration
    public static class RewardItems {
        @Comment("Item shown when reward is claimed")
        public RewardItemTemplate claimed = new RewardItemTemplate(
                Material.LIME_WOOL,
                "<gold>{reward_name}",
                Arrays.asList(
                        "<gray>Số ngày: <yellow>{days}</yellow> ngày",
                        "",
                        "<green>✓ Đã nhận</green>",
                        "",
                        "<gray>Phần thưởng:",
                        "{rewards_list}"
                )
        );

        @Comment("Item shown when reward is completed but not yet awarded")
        public RewardItemTemplate completed = new RewardItemTemplate(
                Material.YELLOW_WOOL,
                "<gold>{reward_name}",
                Arrays.asList(
                        "<gray>Số ngày: <yellow>{days}</yellow> ngày",
                        "",
                        "<yellow>✓ Chờ nhận</yellow>",
                        "",
                        "<gray>Phần thưởng:",
                        "{rewards_list}"
                )
        );

        @Comment("Item shown when reward is locked (not reached)")
        public RewardItemTemplate locked = new RewardItemTemplate(
                Material.RED_WOOL,
                "<gold>{reward_name}",
                Arrays.asList(
                        "<gray>Số ngày: <yellow>{days}</yellow> ngày",
                        "",
                        "<red>✗ Chưa mở khoá</red>",
                        "",
                        "<gray>Phần thưởng:",
                        "{rewards_list}",
                        "",
                        "<gray>{remaining_days} ngày nữa!"
                )
        );
    }

    @Configuration
    public static class RewardItemTemplate {
        public Material material;
        public String name;

        @Comment("Placeholders: {reward_name}, {days}, {remaining_days}, {rewards_list}")
        public List<String> lore;

        public RewardItemTemplate() {
            this.material = Material.PAPER;
            this.name = "<gray>Reward";
            this.lore = Arrays.asList("<gray>No description");
        }

        public RewardItemTemplate(Material material, String name, List<String> lore) {
            this.material = material;
            this.name = name;
            this.lore = lore;
        }
    }
}
