package org.simpmc.simppay.config.types;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;

@Configuration
public class MainConfig {
    @Comment("Bật chế độ debug để xem thêm thông tin log")
    public boolean debug = false;
    
    @Comment("Thời gian gọi API kiểm tra thẻ và giao dịch ngân hàng, tính theo giây (tối thiểu 3 giây)")
    public int intervalApiCall = 5;
    
    @Comment("Thời gian làm mới cache, được sử dụng cho placeholder và menu, tính theo giây (tối thiểu 3 giây)")
    public int intervalRefreshCache = 5;
    
    @Comment("Cấu hình hiệu suất")
    public PerformanceConfig performance = new PerformanceConfig();
    
    @Comment("Cấu hình bảo mật")
    public SecurityConfig security = new SecurityConfig();
    
    @Configuration
    public static class PerformanceConfig {
        @Comment("Kích thước cache tối đa cho player data")
        public int maxPlayerCacheSize = 1000;
        
        @Comment("Thời gian sống cache player (giây)")
        public int playerCacheTtl = 300;
        
        @Comment("Bật tối ưu hóa database batch operations")
        public boolean enableBatchOperations = true;
        
        @Comment("Kích thước batch cho database operations")
        public int batchSize = 100;
    }
    
    @Configuration
    public static class SecurityConfig {
        @Comment("Bật rate limiting cho API calls")
        public boolean enableRateLimit = true;
        
        @Comment("Số lượng request tối đa per second")
        public int maxRequestsPerSecond = 10;
        
        @Comment("Bật validation cho webhook signatures")
        public boolean validateWebhookSignatures = true;
    }
}
