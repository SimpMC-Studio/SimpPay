package org.simpmc.simppay.config.types;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import net.kyori.adventure.key.Key;
import org.simpmc.simppay.config.types.data.SoundConfig;
import org.simpmc.simppay.data.PaymentStatus;

import java.util.Map;

@Configuration
public class MessageConfig {

    // <gradient:#f9535c:#FCD05C>SimpPay</gradient>
    @Comment({"Tất cả message đều hỗ trợ PlaceholderAPI dưới dạng [papi:<placeholder>]",
            "Không điền % %, ví dụ <papi:player_name>"})
    public String prefix = "<dark_gray>[<gradient:#E34949:#D8DB5C><bold>SimpPay</bold><dark_gray>] <reset>";

    public String configReloaded = "<color:#B3E664>Tải lại config thành công!";

    public String pendingCard = "<color:#E7EE88>Thẻ của bạn đang được xử lý, vui lòng chờ trong giây lát...";

    public String failedCard = "<color:#ff0000>Nạp thẻ thất bại!";

    public String wrongPriceCard = "<color:#ff0000>Thẻ cào của bạn nhập sai mệnh giá, bạn đã được cộng thẻ trị giá <white><amount><color:#ff0000> vào tài khoản!";

    public String successPayment = "<color:#00ff00>Nạp thành công với mệnh giá <white><amount>đ<color:#00ff00>!";

    public String cancelBanking = "<color:#ff0000>Đã hủy yêu cầu thanh toán ngân hàng!";

    public String existBankingSession = "<color:#ff0000>Bạn đã tạo lệnh nạp trước đó rồi! Nếu muốn tạo lệnh nạp mới, hãy gõ /bank cancel";

    public String noExistBankingSession = "<color:#ff0000>Bạn chưa tạo lệnh nạp nào cả! Hãy gõ <white>/bank <số tiền><color:#ff0000> để tạo lệnh nạp mới!";

    public String pendingBank = "<color:#00ff00>Hãy quét mã QR ở bên tay của bạn để thanh toán!";

    public String promptPaymentLink = "<color:#00ff00>Bạn có thể thanh toán qua đường dẫn sau nếu QR trên tay bị lỗi: <dark_green><click:open_url:'<link>'>Click vào đây</click>";

    public String mustDivisibleBy1000 = "<color:#ff0000>Số tiền phải chia hết cho <white>1000<color:#ff0000>!";

    public String unknownErrror = "<color:#ff0000>Lỗi không xác định! Hãy báo cho<white> Admin Server<color:#ff0000> để kiểm tra lỗi!";

    public String invalidAmount = "<color:#ff0000>Số tiền nạp tối thiểu là <white>{amount}<color:#ff0000>!";

    public String invalidParam = "<color:#ff0000>Serial hoặc mã thẻ nhập vào chưa chính xác!";

    public String playerNotFound = "<color:#ff0000>Không tìm thấy người chơi nào với tên <white>{name}<color:#ff0000>!";

    public String noPromo = "<color:#ff0000>Không có khuyến mại theo mặc định!";

    // TODO: store sound directly or have a toSound method
    public Map<PaymentStatus, SoundConfig> soundEffect = Map.of(
            PaymentStatus.SUCCESS, new SoundConfig(Key.key(Key.MINECRAFT_NAMESPACE, "entity.player.levelup"), 1, 1), // /playsound minecraft:entity.player.levelup ambient @a ~ ~ ~ 1 1
            PaymentStatus.PENDING, new SoundConfig(Key.key(Key.MINECRAFT_NAMESPACE, "block.amethyst_block.resonate"), 1, 1), // /playsound minecraft:block.amethyst_block.resonate master ThatCorona ~ ~ ~ 1 1
            PaymentStatus.FAILED, new SoundConfig(Key.key(Key.MINECRAFT_NAMESPACE, "block.amethyst_block.resonate"), 1, 0.5F) // /playsound minecraft:block.amethyst_block.resonate master ThatCorona ~ ~ ~ 1 0.5
    );

}
