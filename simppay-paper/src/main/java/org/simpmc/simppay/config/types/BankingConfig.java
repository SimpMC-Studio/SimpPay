package org.simpmc.simppay.config.types;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import org.simpmc.simppay.config.annotations.Folder;
import org.simpmc.simppay.handler.data.BankAPI;

@Configuration
@Folder("banking")
public class BankingConfig {
    @Comment("Dịch vụ cổng banking: PAYOS, WEB2M")
    public BankAPI bankApi = BankAPI.PAYOS;

    @Comment("Thời gian chờ thanh toán ngân hàng (giây)")
    public int bankingTimeout = 60 * 5; // 5 minutes

    @Comment("Số tiền nạp chuyển khoản tối thiểu")
    public int minBanking = 10000;

    @Comment("Hiện QR code trên tay trái của người chơi thay vì tay phải")
    public boolean showQrOnLeftHand = false;

    @Comment("Hiện QR code trên item frame ảo trước mặt người chơi thay vì trên tay")
    public boolean showQrAsItemFrame = false;
}
