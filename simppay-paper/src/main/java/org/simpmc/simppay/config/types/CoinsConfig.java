package org.simpmc.simppay.config.types;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.Ignore;
import lombok.Data;
import org.simpmc.simppay.data.card.CardPrice;
import org.simpmc.simppay.handler.data.CoinsAPI;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Configuration
@Data
public class CoinsConfig {
    @Ignore
    public final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    @Comment("Plugin sử dụng để cộng xu: PLAYERPOINTS, COINSENGINE")
    public CoinsAPI pointsProvider = CoinsAPI.PLAYERPOINTS;
    @Comment("Tên đơn vị xu trong CoinsEngine (Chỉ dùng cho CoinsEngine)")
    public String coinsEngineCurrency = "coins";
    @Comment("Lượng xu cộng vào tương ứng với giá trị thẻ cào")
    public Map<CardPrice, Long> cardToCoins = Map.of(
            CardPrice._10K, 10L,
            CardPrice._20K, 20L,
            CardPrice._30K, 30L,
            CardPrice._50K, 50L,
            CardPrice._100K, 100L,
            CardPrice._200K, 200L,
            CardPrice._300K, 300L,
            CardPrice._500K, 500L,
            CardPrice._1000K, 1000L
    );
    @Comment({"Lệnh chạy tương ứng với giá trị thẻ cào", "Có hỗ trợ PlaceholderAPI dạng %player_name%"})
    public Map<CardPrice, List<String>> cardToCommands = Map.of(
            CardPrice._10K, List.of("tell %player_name% Bạn vừa nạp 10k"),
            CardPrice._20K, List.of("tell %player_name% Bạn vừa nạp 20k"),
            CardPrice._30K, List.of("tell %player_name% Bạn vừa nạp 30k"),
            CardPrice._50K, List.of("tell %player_name% Bạn vừa nạp 50k"),
            CardPrice._100K, List.of("tell %player_name% Bạn vừa nạp 100k"),
            CardPrice._200K, List.of("tell %player_name% Bạn vừa nạp 200k"),
            CardPrice._300K, List.of("tell %player_name% Bạn vừa nạp 300k"),
            CardPrice._500K, List.of("tell %player_name% Bạn vừa nạp 500k"),
            CardPrice._1000K, List.of("tell %player_name% Bạn vừa nạp 1000k")
    );
    @Comment({
            "Lệnh chạy khi nạp chuyển khoản thành công, theo từng mức tiền",
            "Key: Số tiền tối thiểu để chạy lệnh (VD: 50000 = chạy khi nạp >= 50,000đ)",
            "Value: Danh sách lệnh sẽ chạy",
            "",
            "Placeholder hỗ trợ:",
            "  <amount> - Số tiền nạp (VD: 50000)",
            "  <amount_formatted> - Số tiền có dấu phẩy (VD: 50,000)",
            "  <ref_id> - Mã giao dịch banking",
            "  %player_name% - Tên người chơi (PlaceholderAPI)",
            "  Tất cả placeholder của PlaceholderAPI đều được hỗ trợ",
            "",
            "VD: Nạp 100,000đ sẽ chạy tất cả lệnh có key <= 100000"
    })
    public Map<Long, List<String>> bankingToCommands = Map.of(
            0L, List.of("say %player_name% vừa nạp <amount_formatted>đ qua chuyển khoản!")
    );
    @Comment({"Công thức tính xu được nhận khi nạp chuyển khoản",
            "- Số xu nhận được = Số xu tiêu chuẩn + Số xu được nhận thêm + (Số tiền nạp chuyển khoản / 1000) × Khuyến mãi)",
            "Trong đó:",
            "- Số xu tiêu chuẩn = (Số tiền nạp chuyển khoản / 1000) x Tỷ lệ tiêu chuẩn cho chuyển khoản",
            "- Số xu được nhận thêm = (Số tiền nạp chuyển khoản / 1000) × Tỷ lệ nhận thêm cho chuyển khoản",
            " ",
            "Lượng xu tiêu chuẩn cộng vào tương ứng với mỗi 1000đ nạp chuyển khoản", "Mặc định: 1000đ = 1 xu"})
    public double baseBankRate = 1.0;
    @Comment({"Tỷ lệ nhận thêm cho chuyển khoản, tương ứng với mõi 1000đ khi nạp chuyển khoản",
            "Mặc định: 1000đ = 0.5 xu"})

    public double extraBankRate = 0.5;

    @Comment({"Giá trị khuyến mãi thêm",
            "VD: ",
            "0: Không có khuyến mãi",
            "0.5: Khuyến mãi 50%, VD: nạp 100k được 150k",
            "1: Khuyến mãi 100%, VD: nạp 100k được 200k",
            "1.5: Khuyến mãi 150%, VD: nạp 100k được 250k"})
    public double promoRate = 0.0;
    @Comment("Thời gian kết thúc khuyến mãi")
    public String promoEndTimeString = "30/04/1975 11:30";

    public double getPromoRate() {
        try {
            LocalDateTime promoEndTime = LocalDateTime.parse(promoEndTimeString, formatter);
            if (promoEndTime.isBefore(LocalDateTime.now())) {
                return 0.0;
            }
            return promoRate;
        } catch (Exception e) {
            // không parse được time => cook event
            return 0.0;
        }
    }
}
