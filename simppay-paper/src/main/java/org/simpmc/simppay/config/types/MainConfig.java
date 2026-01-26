package org.simpmc.simppay.config.types;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;

@Configuration
public class MainConfig {
    public boolean debug = false;
    @Comment("Thời gian gọi API kiểm tra thẻ và giao dịch ngân hàng, tính theo giây")
    public int intervalApiCall = 5;

    @Comment("Cấu hình BossBar milestone")
    public BossBarSettings bossbar = new BossBarSettings();

    @Configuration
    public static class BossBarSettings {
        @Comment("Tần suất cập nhật BossBar, tính theo game tick (20 tick = 1 giây). Giá trị nhỏ hơn = mượt hơn nhưng tốn hiệu năng hơn")
        public int updateFrequencyTicks = 5;

        @Comment("Thời gian hiển thị mỗi milestone trước khi chuyển sang milestone tiếp theo, tính theo giây")
        public int cycleDurationSeconds = 15;
    }
}
