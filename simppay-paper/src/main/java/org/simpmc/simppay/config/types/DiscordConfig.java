package org.simpmc.simppay.config.types;

import de.exlll.configlib.Configuration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Configuration
public class DiscordConfig {
    public boolean enabled = false;
    public EventConfig cardPayment = new EventConfig(
            false, "", "SimpPay", "",
            new EmbedConfig(
                    "Nạp thẻ thành công",
                    "**{player}** vừa nạp thẻ thành công!\nCổng: **{gateway}** | Mệnh giá: **{amount}đ** | Thực nhận: **{trueAmount}đ**",
                    "#57F287", "", "SimpPay",
                    List.of(
                            new FieldConfig("Người chơi", "{player}", true),
                            new FieldConfig("Cổng nạp", "{gateway}", true),
                            new FieldConfig("Mệnh giá", "{amount}đ", true),
                            new FieldConfig("Thực nhận", "{trueAmount}đ", true)
                    )
            )
    );
    public EventConfig bankPayment = new EventConfig(
            false, "", "SimpPay", "",
            new EmbedConfig(
                    "Chuyển khoản thành công",
                    "**{player}** vừa chuyển khoản thành công!\nCổng: **{gateway}** | Số tiền: **{amount}đ**",
                    "#3498DB", "", "SimpPay",
                    List.of(
                            new FieldConfig("Người chơi", "{player}", true),
                            new FieldConfig("Cổng nạp", "{gateway}", true),
                            new FieldConfig("Số tiền", "{amount}đ", true)
                    )
            )
    );
    public EventConfig playerMilestone = new EventConfig(
            false, "", "SimpPay", "",
            new EmbedConfig(
                    "Mốc nạp cá nhân",
                    "**{player}** đã đạt mốc nạp **{milestoneAmount}đ** ({milestoneType})!\nTổng nạp hiện tại: **{currentAmount}đ**",
                    "#F1C40F", "", "SimpPay",
                    List.of(
                            new FieldConfig("Người chơi", "{player}", true),
                            new FieldConfig("Loại mốc", "{milestoneType}", true),
                            new FieldConfig("Mốc đạt được", "{milestoneAmount}đ", true),
                            new FieldConfig("Tổng nạp", "{currentAmount}đ", true)
                    )
            )
    );
    public EventConfig serverMilestone = new EventConfig(
            false, "", "SimpPay", "",
            new EmbedConfig(
                    "Mốc nạp server",
                    "Server đã đạt mốc nạp **{milestoneAmount}đ** ({milestoneType})!\nTổng nạp server: **{currentAmount}đ**",
                    "#E74C3C", "", "SimpPay",
                    List.of(
                            new FieldConfig("Loại mốc", "{milestoneType}", true),
                            new FieldConfig("Mốc đạt được", "{milestoneAmount}đ", true),
                            new FieldConfig("Tổng nạp server", "{currentAmount}đ", true)
                    )
            )
    );

    @Configuration
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventConfig {
        public boolean enabled = false;
        public String webhookUrl = "";
        public String botUsername = "SimpPay";
        public String avatarUrl = "";
        public EmbedConfig embed = new EmbedConfig();
    }

    @Configuration
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbedConfig {
        public String title = "";
        public String description = "";
        public String color = "#57F287";
        public String thumbnailUrl = "";
        public String footerText = "SimpPay";
        public List<FieldConfig> fields = List.of();
    }

    @Configuration
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldConfig {
        public String name = "";
        public String value = "";
        public boolean inline = true;
    }
}
