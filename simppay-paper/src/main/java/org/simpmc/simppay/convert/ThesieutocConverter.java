package org.simpmc.simppay.convert;

import com.j256.ormlite.dao.Dao;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.data.card.CardType;
import org.simpmc.simppay.database.entities.CardPayment;
import org.simpmc.simppay.database.entities.SPPlayer;
import org.simpmc.simppay.handler.data.CardAPI;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.util.MessageUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ThesieutocConverter extends PluginConverter {

    private static final DateTimeFormatter FLAT_FILE_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Override
    public String getName() {
        return "Thesieutoc";
    }

    @Override
    public boolean supportsMySQL() { return true; }

    @Override
    public boolean supportsFlatfile() { return true; }

    @Override
    public boolean supportsCredentials() { return true; }

    @Override
    public ImportResult importFromMySQL(String host, int port, String database, String username, String password) {
        ImportResult result = new ImportResult();

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8";

        try {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                } catch (ClassNotFoundException ex2) {
                    Class.forName("org.mariadb.jdbc.Driver");
                    url = "jdbc:mariadb://" + host + ":" + port + "/" + database + "?useSSL=false";
                }
            }
        } catch (ClassNotFoundException e) {
            result.addFailed("Không tìm thấy MySQL/MariaDB JDBC driver. Vui lòng thêm connector vào classpath server.");
            return result;
        }

        Set<UUID> importedUUIDs = new HashSet<>();

        try (Connection conn = DriverManager.getConnection(url, username, password);
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM napthe_log WHERE note = 'thanh cong'");
             ResultSet rs = stmt.executeQuery()) {

            DatabaseService dbService = SPPlugin.getService(DatabaseService.class);
            Dao<CardPayment, UUID> cardDao = dbService.getDatabase().getCardDao();

            while (rs.next()) {
                int rowId = 0;
                try {
                    rowId = rs.getInt("id");
                    String name = rs.getString("name");
                    String uuidStr = rs.getString("uuid");
                    String serial = rs.getString("seri");
                    String pin = rs.getString("pin");
                    String cardTypeStr = rs.getString("loai");
                    long timeSec = rs.getLong("time");
                    String menhGia = rs.getString("menhgia");

                    UUID paymentUUID = UUID.nameUUIDFromBytes(("TST-MYSQL-" + rowId).getBytes(StandardCharsets.UTF_8));

                    if (cardDao.idExists(paymentUUID)) {
                        result.addSkipped();
                        continue;
                    }

                    UUID playerUUID;
                    try {
                        playerUUID = UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException e) {
                        result.addFailed("Row " + rowId + ": UUID không hợp lệ: " + uuidStr);
                        continue;
                    }

                    CardType cardType;
                    try {
                        cardType = CardType.fromString(cardTypeStr);
                    } catch (IllegalArgumentException e) {
                        result.addFailed("Row " + rowId + ": Loại thẻ không xác định: " + cardTypeStr);
                        continue;
                    }

                    double priceValue;
                    try {
                        priceValue = Double.parseDouble(menhGia.replaceAll("[^0-9.]", ""));
                    } catch (NumberFormatException e) {
                        result.addFailed("Row " + rowId + ": Mệnh giá không hợp lệ: " + menhGia);
                        continue;
                    }

                    SPPlayer spPlayer = findOrCreatePlayer(playerUUID, name);

                    CardPayment cardPayment = new CardPayment();
                    cardPayment.setPaymentID(paymentUUID);
                    cardPayment.setPlayer(spPlayer);
                    cardPayment.setPin(pin != null ? pin : "");
                    cardPayment.setSerial(serial != null ? serial : "");
                    cardPayment.setPriceValue(priceValue);
                    cardPayment.setCardType(cardType);
                    cardPayment.setRefID("TST-IMPORT-" + rowId);
                    cardPayment.setTrueAmount(priceValue);
                    cardPayment.setAmount(priceValue);
                    cardPayment.setTimestamp(timeSec * 1000L);
                    cardPayment.setApiProvider(CardAPI.THESIEUTOC);

                    cardDao.create(cardPayment);
                    importedUUIDs.add(playerUUID);
                    result.addImported();

                } catch (Exception e) {
                    result.addFailed("Row " + rowId + ": " + e.getMessage());
                    MessageUtil.debug("[Import] MySQL row error: " + e.getMessage());
                }
            }

        } catch (SQLException e) {
            result.addFailed("Lỗi kết nối MySQL: " + e.getMessage());
            return result;
        }

        rebuildCache(importedUUIDs);
        return result;
    }

    @Override
    public ImportResult importFromFlatfile(File logFile) {
        ImportResult result = new ImportResult();

        if (!logFile.exists()) {
            result.addFailed("File không tồn tại: " + logFile.getAbsolutePath());
            return result;
        }

        Set<UUID> importedUUIDs = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile, StandardCharsets.UTF_8))) {
            DatabaseService dbService = SPPlugin.getService(DatabaseService.class);
            Dao<CardPayment, UUID> cardDao = dbService.getDatabase().getCardDao();

            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    // Format: [HH:mm dd/MM/yyyy] playerName | serial | pin | cardPrice | cardType | callbackMessage
                    if (!line.startsWith("[")) {
                        result.addFailed("Dòng " + lineNum + ": Định dạng không hợp lệ");
                        continue;
                    }
                    int closeBracket = line.indexOf(']');
                    if (closeBracket < 0) {
                        result.addFailed("Dòng " + lineNum + ": Thiếu dấu ']'");
                        continue;
                    }

                    String timestampStr = line.substring(1, closeBracket).trim();
                    String rest = line.substring(closeBracket + 1).trim();
                    String[] parts = rest.split(" \\| ");

                    if (parts.length < 5) {
                        result.addFailed("Dòng " + lineNum + ": Thiếu trường dữ liệu (cần ít nhất 5)");
                        continue;
                    }

                    String playerName = parts[0].trim();
                    String serial = parts[1].trim();
                    String pin = parts[2].trim();
                    String cardPriceStr = parts[3].trim();
                    String cardTypeStr = parts[4].trim();

                    long epochMilli;
                    try {
                        LocalDateTime dt = LocalDateTime.parse(timestampStr, FLAT_FILE_FORMAT);
                        epochMilli = dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    } catch (Exception e) {
                        result.addFailed("Dòng " + lineNum + ": Timestamp không hợp lệ: " + timestampStr);
                        continue;
                    }

                    CardType cardType;
                    try {
                        cardType = CardType.fromString(cardTypeStr);
                    } catch (IllegalArgumentException e) {
                        result.addFailed("Dòng " + lineNum + ": Loại thẻ không xác định: " + cardTypeStr);
                        continue;
                    }

                    double priceValue;
                    try {
                        priceValue = Double.parseDouble(cardPriceStr.replaceAll("[^0-9.]", ""));
                    } catch (NumberFormatException e) {
                        result.addFailed("Dòng " + lineNum + ": Mệnh giá không hợp lệ: " + cardPriceStr);
                        continue;
                    }

                    UUID paymentUUID = UUID.nameUUIDFromBytes(
                            ("TST-FF-" + playerName + "-" + serial + "-" + pin).getBytes(StandardCharsets.UTF_8));

                    if (cardDao.idExists(paymentUUID)) {
                        result.addSkipped();
                        continue;
                    }

                    @SuppressWarnings("deprecation")
                    UUID playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
                    SPPlayer spPlayer = findOrCreatePlayer(playerUUID, playerName);

                    CardPayment cardPayment = new CardPayment();
                    cardPayment.setPaymentID(paymentUUID);
                    cardPayment.setPlayer(spPlayer);
                    cardPayment.setPin(pin);
                    cardPayment.setSerial(serial);
                    cardPayment.setPriceValue(priceValue);
                    cardPayment.setCardType(cardType);
                    cardPayment.setRefID("TST-FF-" + lineNum);
                    cardPayment.setTrueAmount(priceValue);
                    cardPayment.setAmount(priceValue);
                    cardPayment.setTimestamp(epochMilli);
                    cardPayment.setApiProvider(CardAPI.THESIEUTOC);

                    cardDao.create(cardPayment);
                    importedUUIDs.add(playerUUID);
                    result.addImported();

                } catch (Exception e) {
                    result.addFailed("Dòng " + lineNum + ": " + e.getMessage());
                    MessageUtil.debug("[Import] Flatfile line error: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            result.addFailed("Lỗi đọc file: " + e.getMessage());
        }

        rebuildCache(importedUUIDs);
        return result;
    }

    @Override
    public ImportResult importCredentials() {
        ImportResult result = new ImportResult();

        File pluginsFolder = SPPlugin.getInstance().getDataFolder().getParentFile();
        File thesieutocConfig = new File(pluginsFolder, "Thesieutoc/config.yml");

        if (!thesieutocConfig.exists()) {
            result.addFailed("Không tìm thấy config Thesieutoc tại: " + thesieutocConfig.getAbsolutePath());
            return result;
        }

        YamlConfiguration tstConfig = YamlConfiguration.loadConfiguration(thesieutocConfig);
        String apiKey = tstConfig.getString("api-key", "");
        String secretKey = tstConfig.getString("api-secret", "");

        if (apiKey.isEmpty()) {
            result.addFailed("Trường 'api-key' trống trong config Thesieutoc");
            return result;
        }

        File simpPayTSTConfig = new File(SPPlugin.getInstance().getDataFolder(), "card/thesieutoc/thesieutoc-config.yml");

        if (!simpPayTSTConfig.exists()) {
            ConfigManager.getInstance().reloadAll();
        }

        if (!simpPayTSTConfig.exists()) {
            result.addFailed("Không tìm thấy thesieutoc-config.yml trong thư mục SimpPay");
            return result;
        }

        try {
            YamlConfiguration spConfig = YamlConfiguration.loadConfiguration(simpPayTSTConfig);
            spConfig.set("api-key", apiKey);
            spConfig.set("secret-key", secretKey);
            spConfig.save(simpPayTSTConfig);

            ConfigManager.getInstance().reloadAll();
            result.addImported();
        } catch (IOException e) {
            result.addFailed("Lỗi lưu config: " + e.getMessage());
        }

        return result;
    }
}
