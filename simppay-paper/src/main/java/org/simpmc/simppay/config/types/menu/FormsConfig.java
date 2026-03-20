package org.simpmc.simppay.config.types.menu;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import org.simpmc.simppay.config.annotations.Folder;

/**
 * Configuration for Bedrock (Floodgate/Geyser) forms.
 * Note: Bedrock forms do not support MiniMessage or ChatColor formatting.
 * Use plain text only.
 */
@Configuration
@Folder("menus")
public class FormsConfig {

    @Comment("Cau hinh form nap the Bedrock")
    public NaptheFormStrings naptheForm = new NaptheFormStrings();

    @Comment("Cau hinh form xem lich su Bedrock")
    public ViewHistoryFormStrings viewHistoryForm = new ViewHistoryFormStrings();

    @Comment("Cau hinh form streak Bedrock")
    public StreakFormStrings streakForm = new StreakFormStrings();

    @Configuration
    public static class NaptheFormStrings {
        public String title = "SimpPay - Nap the";
        public String cardTypeLabel = "Loai The";
        public String priceLabel = "Menh Gia";
        public String warning = "Luu y: Nhap sai menh gia se nhan xu tuong ung gia tri that cua the";
        public String serialLabel = "So Serial";
        public String serialPlaceholder = "Nhap so serial cua the";
        public String pinLabel = "Ma The";
        public String pinPlaceholder = "Nhap ma PIN cua the";
        public String submitLabel = "Bam Submit de nap the";
    }

    @Configuration
    public static class ViewHistoryFormStrings {
        public String title = "Lich su nap tien";
        public String amountFormat = "So tien: %s";
        public String emptyMessage = "Khong co lich su nap tien";
    }

    @Configuration
    public static class StreakFormStrings {
        public String title = "Chuỗi nạp thẻ";
        public String currentStreakFormat = "Chuỗi hiện tại: %d ngày";
        public String bestStreakFormat = "Chuỗi dài nhất: %d ngày";
        public String lastPaymentFormat = "Lần nạp cuối: %s";
        public String neverText = "Chưa có";
        public String dateFormat = "dd/MM/yyyy HH:mm";
        public String statusClaimed = "Đã nhận";
        public String statusCompleted = "Chờ nhận";
        public String statusLockedFormat = "Chưa mở (%d ngày còn lại)";
        public String detailSeparator = "===============================";
        public String daysRequiredFormat = "Yêu cầu: %d ngày";
        public String claimedText = "Đã nhận";
        public String completedText = "Chờ nhận (sẽ được trao)";
        public String lockedText = "Chưa mở khoá";
        public String remainingDaysFormat = "Còn %d ngày nữa!";
        public String rewardsLabel = "Phần thưởng:";
    }
}
