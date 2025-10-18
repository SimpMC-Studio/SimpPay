package org.simpmc.simppay.service.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.simpmc.simppay.data.PaymentType;
import org.simpmc.simppay.database.Database;
import org.simpmc.simppay.database.dto.PaymentRecord;
import org.simpmc.simppay.database.entities.BankingPayment;
import org.simpmc.simppay.database.entities.CardPayment;
import org.simpmc.simppay.database.entities.SPPlayer;
import org.simpmc.simppay.model.Payment;
import org.simpmc.simppay.util.CalendarUtil;
import org.simpmc.simppay.util.MessageUtil;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized payment logging service with caching and efficient SQL queries.
 * Previously fetched all records and summed in Java - now uses SQL SUM() aggregations.
 */
@Slf4j
public class PaymentLogService {
    private final Dao<BankingPayment, UUID> bankDao;
    private final Dao<CardPayment, UUID> cardDao;

    // Cache for amount queries with TTL (expires after 10 seconds)
    private static final long CACHE_TTL_MS = 10_000;
    private static final class CacheEntry {
        final long amount;
        final long timestamp;

        CacheEntry(long amount) {
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    private final Map<String, CacheEntry> queryCache = new ConcurrentHashMap<>();

    public PaymentLogService(Database database) {
        this.bankDao = database.getBankDao();
        this.cardDao = database.getCardDao();
    }

    /**
     * Get cached value if available and not expired, otherwise calculate
     */
    private long getOrComputeAmount(String cacheKey, AmountQuery query) {
        CacheEntry cached = queryCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.amount;
        }

        try {
            long amount = query.query();
            queryCache.put(cacheKey, new CacheEntry(amount));
            return amount;
        } catch (Exception e) {
            log.error("Error computing amount for cache key: {}", cacheKey, e);
            return 0L;
        }
    }

    @FunctionalInterface
    private interface AmountQuery {
        long query() throws SQLException;
    }

    /**
     * Optimized query for total amount by summing banking and card payments
     */
    private long sumAmountForPlayer(SPPlayer player, Long startTime, Long endTime) throws SQLException {
        double bankTotal = 0;
        double cardTotal = 0;

        QueryBuilder<BankingPayment, UUID> bankBuilder = bankDao.queryBuilder();
        bankBuilder.selectRaw("COALESCE(SUM(amount), 0) as total");
        bankBuilder.where().eq("player_uuid", player);

        if (startTime != null && endTime != null) {
            bankBuilder.where().between("timestamp", startTime, endTime);
        }

        List<BankingPayment> bankResults = bankBuilder.query();
        if (!bankResults.isEmpty() && bankResults.get(0).getAmount() > 0) {
            bankTotal = bankResults.get(0).getAmount();
        }

        QueryBuilder<CardPayment, UUID> cardBuilder = cardDao.queryBuilder();
        cardBuilder.selectRaw("COALESCE(SUM(amount), 0) as total");
        cardBuilder.where().eq("player_uuid", player);

        if (startTime != null && endTime != null) {
            cardBuilder.where().between("timestamp", startTime, endTime);
        }

        List<CardPayment> cardResults = cardBuilder.query();
        if (!cardResults.isEmpty() && cardResults.get(0).getAmount() > 0) {
            cardTotal = cardResults.get(0).getAmount();
        }

        return (long) (bankTotal + cardTotal);
    }

    /**
     * Optimized query for server-wide amount
     */
    private long sumAmountForServer(Long startTime, Long endTime) throws SQLException {
        double bankTotal = 0;
        double cardTotal = 0;

        QueryBuilder<BankingPayment, UUID> bankBuilder = bankDao.queryBuilder();
        bankBuilder.selectRaw("COALESCE(SUM(amount), 0) as total");

        if (startTime != null && endTime != null) {
            bankBuilder.where().between("timestamp", startTime, endTime);
        }

        List<BankingPayment> bankResults = bankBuilder.query();
        if (!bankResults.isEmpty() && bankResults.get(0).getAmount() > 0) {
            bankTotal = bankResults.get(0).getAmount();
        }

        QueryBuilder<CardPayment, UUID> cardBuilder = cardDao.queryBuilder();
        cardBuilder.selectRaw("COALESCE(SUM(amount), 0) as total");

        if (startTime != null && endTime != null) {
            cardBuilder.where().between("timestamp", startTime, endTime);
        }

        List<CardPayment> cardResults = cardBuilder.query();
        if (!cardResults.isEmpty() && cardResults.get(0).getAmount() > 0) {
            cardTotal = cardResults.get(0).getAmount();
        }

        return (long) (bankTotal + cardTotal);
    }

    public boolean todaysPaymentExists(UUID playerId) {
        try {
            List<BankingPayment> bankingPayments = bankDao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerId)
                    .query();
            List<CardPayment> cardPayments = cardDao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerId)
                    .query();
            return !bankingPayments.isEmpty() || !cardPayments.isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<PaymentRecord> getPaymentsByPlayer(SPPlayer playerId) {
        try {
            List<PaymentRecord> payments = new java.util.ArrayList<>(bankDao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerId)
                    .query()
                    .stream()
                    .map(PaymentRecord::fromBank)
                    .toList());

            payments.addAll(cardDao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerId)
                    .query()
                    .stream()
                    .map(PaymentRecord::fromCard)
                    .toList());

            return payments;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<PaymentRecord> getEntireServerPayments() {
        try {
            List<PaymentRecord> payments = new java.util.ArrayList<>(bankDao
                    .queryForAll()
                    .stream()
                    .map(PaymentRecord::fromBank)
                    .toList());

            payments.addAll(cardDao
                    .queryForAll()
                    .stream()
                    .map(PaymentRecord::fromCard)
                    .toList());

            return payments;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public void resetPlayerPaymentLog(SPPlayer playerId) {
        try {
            List<BankingPayment> bankingPayments = bankDao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerId)
                    .query();
            List<CardPayment> cardPayments = cardDao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerId)
                    .query();

            for (BankingPayment payment : bankingPayments) {
                bankDao.delete(payment);
                MessageUtil.debug(String.format("Removed %s payment: %s", playerId.getName(), payment.toString()));
            }
            for (CardPayment payment : cardPayments) {
                cardDao.delete(payment);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Double getPlayerTotalAmount(SPPlayer playerId) {
        String cacheKey = "player-total-" + playerId.getUuid();
        return (double) getOrComputeAmount(cacheKey, () -> sumAmountForPlayer(playerId, null, null));
    }

    public long getEntireServerAmount() {
        String cacheKey = "server-total-all";
        return getOrComputeAmount(cacheKey, () -> sumAmountForServer(null, null));
    }

    public long getEntireServerBankAmount() {
        try {
            List<BankingPayment> bankingPayments = bankDao.queryForAll();
            double bankingTotal = bankingPayments.stream()
                    .mapToDouble(BankingPayment::getAmount)
                    .sum();
            return (long) (bankingTotal);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public long getEntireServerCardAmount() {
        try {
            List<CardPayment> cardPayments = cardDao.queryForAll();
            double cardTotal = cardPayments.stream()
                    .mapToDouble(CardPayment::getAmount)
                    .sum();
            return (long) (cardTotal);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public long getEntireServerDailyAmount() {
        try {
            long epoch = System.currentTimeMillis();
            long startOfDay = CalendarUtil.getFirstHourOfDay(epoch);
            long endOfDay = CalendarUtil.getLastHourOfDay(epoch);

            String cacheKey = "server-daily-" + startOfDay;
            return getOrComputeAmount(cacheKey, () -> sumAmountForServer(startOfDay, endOfDay));
        } catch (Exception e) {
            log.error("Error getting server daily amount", e);
            return 0L;
        }
    }

    public long getEntireServerWeeklyAmount() {
        try {
            long epoch = System.currentTimeMillis();
            long startOfWeek = CalendarUtil.getFirstDayOfWeek(epoch);
            long endOfWeek = CalendarUtil.getLastDayOfWeek(epoch);

            String cacheKey = "server-weekly-" + startOfWeek;
            return getOrComputeAmount(cacheKey, () -> sumAmountForServer(startOfWeek, endOfWeek));
        } catch (Exception e) {
            log.error("Error getting server weekly amount", e);
            return 0L;
        }
    }

    public long getEntireServerMonthlyAmount() {
        try {
            long epoch = System.currentTimeMillis();
            long startOfMonth = CalendarUtil.getFirstDayOfMonth(epoch);
            long endOfMonth = CalendarUtil.getLastDayOfMonth(epoch);

            String cacheKey = "server-monthly-" + startOfMonth;
            return getOrComputeAmount(cacheKey, () -> sumAmountForServer(startOfMonth, endOfMonth));
        } catch (Exception e) {
            log.error("Error getting server monthly amount", e);
            return 0L;
        }
    }

    public long getEntireServerYearlyAmount() {
        try {
            long epoch = System.currentTimeMillis();
            long startOfYear = CalendarUtil.getFirstDayOfYear(epoch);
            long endOfYear = CalendarUtil.getLastDayOfYear(epoch);

            String cacheKey = "server-yearly-" + startOfYear;
            return getOrComputeAmount(cacheKey, () -> sumAmountForServer(startOfYear, endOfYear));
        } catch (Exception e) {
            log.error("Error getting server yearly amount", e);
            return 0L;
        }
    }

    public long getPlayerDailyAmount(SPPlayer playerId) {
        try {
            long epoch = System.currentTimeMillis();
            long startOfDay = CalendarUtil.getFirstHourOfDay(epoch);
            long endOfDay = CalendarUtil.getLastHourOfDay(epoch);

            String cacheKey = "player-daily-" + playerId.getUuid() + "-" + startOfDay;
            return getOrComputeAmount(cacheKey, () -> sumAmountForPlayer(playerId, startOfDay, endOfDay));
        } catch (Exception e) {
            log.error("Error getting player daily amount", e);
            return 0L;
        }
    }

    public long getPlayerWeeklyAmount(SPPlayer playerId) {
        try {
            long epoch = System.currentTimeMillis();
            long startOfWeek = CalendarUtil.getFirstDayOfWeek(epoch);
            long endOfWeek = CalendarUtil.getLastDayOfWeek(epoch);

            String cacheKey = "player-weekly-" + playerId.getUuid() + "-" + startOfWeek;
            return getOrComputeAmount(cacheKey, () -> sumAmountForPlayer(playerId, startOfWeek, endOfWeek));
        } catch (Exception e) {
            log.error("Error getting player weekly amount", e);
            return 0L;
        }
    }

    public long getPlayerMonthlyAmount(SPPlayer playerId) {
        try {
            long epoch = System.currentTimeMillis();
            long startOfMonth = CalendarUtil.getFirstDayOfMonth(epoch);
            long endOfMonth = CalendarUtil.getLastDayOfMonth(epoch);

            String cacheKey = "player-monthly-" + playerId.getUuid() + "-" + startOfMonth;
            return getOrComputeAmount(cacheKey, () -> sumAmountForPlayer(playerId, startOfMonth, endOfMonth));
        } catch (Exception e) {
            log.error("Error getting player monthly amount", e);
            return 0L;
        }
    }

    public long getPlayerYearlyAmount(SPPlayer playerId) {
        try {
            long epoch = System.currentTimeMillis();
            long startOfYear = CalendarUtil.getFirstDayOfYear(epoch);
            long endOfYear = CalendarUtil.getLastDayOfYear(epoch);

            String cacheKey = "player-yearly-" + playerId.getUuid() + "-" + startOfYear;
            return getOrComputeAmount(cacheKey, () -> sumAmountForPlayer(playerId, startOfYear, endOfYear));
        } catch (Exception e) {
            log.error("Error getting player yearly amount", e);
            return 0L;
        }
    }

    private long queryForPlayerAmount(SPPlayer playerId, long start, long end) throws SQLException {
        List<BankingPayment> bankingPayments = bankDao.queryBuilder()
                .where().between("timestamp", start, end)
                .and()
                .eq("player_uuid", playerId)
                .query();
        List<CardPayment> cardPayments = cardDao.queryBuilder()
                .where().between("timestamp", start, end)
                .and()
                .eq("player_uuid", playerId)
                .query();

        double bankingTotal = bankingPayments.stream()
                .mapToDouble(BankingPayment::getAmount)
                .sum();
        double cardTotal = cardPayments.stream()
                .mapToDouble(CardPayment::getAmount)
                .sum();
        return (long) (bankingTotal + cardTotal);
    }

    public void removePayment(Payment payment) {
        if (payment.getPaymentType() == PaymentType.BANKING) {
            removeBankingPayment(payment.getPaymentID());
        }
        if (payment.getPaymentType() == PaymentType.CARD) {
            removeCardPayment(payment.getPaymentID());
        }
        throw new IllegalArgumentException("Invalid payment type: " + payment.getPaymentType());
    }

    public void addPayment(Payment payment) {
        if (payment.getPaymentType() == PaymentType.BANKING) {
            addBankingPayment(new BankingPayment(payment));
            return;
        }
        if (payment.getPaymentType() == PaymentType.CARD) {
            addCardPayment(new CardPayment(payment));
            return;
        }
        throw new IllegalArgumentException("Invalid payment type: " + payment.getPaymentType());
    }

    private void addBankingPayment(BankingPayment payment) {
        try {
            bankDao.createOrUpdate(payment);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addCardPayment(CardPayment payment) {
        try {
            cardDao.createOrUpdate(payment);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeBankingPayment(UUID paymentID) {
        try {

            BankingPayment payment = bankDao.queryForId(paymentID);
            bankDao.delete(payment);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeCardPayment(UUID paymentID) {
        try {
            CardPayment payment = cardDao.queryForId(paymentID);
            cardDao.delete(payment);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
