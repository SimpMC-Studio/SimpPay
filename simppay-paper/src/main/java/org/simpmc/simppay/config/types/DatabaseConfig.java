package org.simpmc.simppay.config.types;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Data;
import org.simpmc.simppay.api.DatabaseSettings;

@Data
@Configuration
public class DatabaseConfig implements DatabaseSettings {
    @Comment("Loại cơ sở dữ liệu để sử dụng. Các loại được hỗ trợ: MYSQL, H2")
    public String type = "H2";
    
    @Comment({"Dưới đây chỉ dành cho kết nối SQL", "Địa chỉ host của cơ sở dữ liệu MySQL"})
    public String host = "localhost";
    
    @Comment("Cổng của cơ sở dữ liệu MySQL")
    public int port = 3306;
    
    @Comment("Tên của cơ sở dữ liệu MySQL")
    public String database = "simppay";
    
    @Comment("Tên người dùng của cơ sở dữ liệu MySQL")
    public String username = "root";
    
    @Comment("Mật khẩu của cơ sở dữ liệu MySQL")
    public String password = "password";
    
    @Comment("Cấu hình HikariCP Pool")
    public PoolConfig pool = new PoolConfig();
    
    @Data
    @Configuration
    public static class PoolConfig {
        @Comment("Số lượng kết nối tối đa trong pool")
        public int maximumPoolSize = 10;
        
        @Comment("Số lượng kết nối tối thiểu trong pool")
        public int minimumIdle = 2;
        
        @Comment("Thời gian chờ kết nối (milliseconds)")
        public long connectionTimeout = 30000;
        
        @Comment("Thời gian timeout khi idle (milliseconds)")
        public long idleTimeout = 600000;
        
        @Comment("Thời gian sống tối đa của kết nối (milliseconds)")
        public long maxLifetime = 1800000;
        
        @Comment("Thời gian rò rỉ kết nối (milliseconds)")
        public long leakDetectionThreshold = 60000;
    }
}
