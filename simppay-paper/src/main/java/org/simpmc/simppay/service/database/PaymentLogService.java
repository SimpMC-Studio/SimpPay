package org.simpmc.simppay.service.database;

import com.j256.ormlite.dao.Dao;
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
import java.util.List;
import java.util.UUID;

public class PaymentLogService {
    private final Dao<BankingPayment, UUID> bankDao;
    private final Dao<CardPayment, UUID> cardDao;

    public PaymentLogService(Database database) {
        this.bankDao = database.getBankDao();
        this.cardDao = database.getCardDao();
    }

    // TODO: Please, someone make a PR to optimize this code, i know it is trash, better when using proper SQL Query with JOIN and SUM :D
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
        try {
            List<BankingPayment> bankingPayments = bankDao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerId)
                    .query();
            List<CardPayment> cardPayments = cardDao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerId)
                    .query();
            double bankingTotal = bankingPayments.stream()
                    .mapToDouble(BankingPayment::getAmount)
                    .sum();
            double cardTotal = cardPayments.stream()
                    .mapToDouble(CardPayment::getAmount)
                    .sum();
            return bankingTotal + cardTotal;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    public long getEntireServerAmount() {
        try {
            List<BankingPayment> bankingPayments = bankDao.queryForAll();
            List<CardPayment> cardPayments = cardDao.queryForAll();
            double bankingTotal = bankingPayments.stream()
                    .mapToDouble(BankingPayment::getAmount)
                    .sum();
            double cardTotal = cardPayments.stream()
                    .mapToDouble(CardPayment::getAmount)
                    .sum();
            return (long) (bankingTotal + cardTotal);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
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

            List<BankingPayment> bankingPayments = bankDao.queryBuilder()
                    .where().between("timestamp", startOfDay, endOfDay).query();
            List<CardPayment> cardPayments = cardDao.queryBuilder().where().between("timestamp", startOfDay, endOfDay).query();

            double bankingTotal = bankingPayments.stream()
                    .mapToDouble(BankingPayment::getAmount)
                    .sum();
            double cardTotal = cardPayments.stream()
                    .mapToDouble(CardPayment::getAmount)
                    .sum();
            return (long) (bankingTotal + cardTotal);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public long getEntireServerWeeklyAmount() {
        try {
            long epoch = System.currentTimeMillis();
            long startOfWeek = CalendarUtil.getFirstDayOfWeek(epoch);
            long endOfWeek = CalendarUtil.getLastDayOfWeek(epoch);

            List<BankingPayment> bankingPayments = bankDao.queryBuilder()
                    .where().between("timestamp", startOfWeek, endOfWeek).query();
            List<CardPayment> cardPayments = cardDao.queryBuilder().where().between("timestamp", startOfWeek, endOfWeek).query();

            double bankingTotal = bankingPayments.stream()
                    .mapToDouble(BankingPayment::getAmount)
                    .sum();
            double cardTotal = cardPayments.stream()
                    .mapToDouble(CardPayment::getAmount)
                    .sum();
            return (long) (bankingTotal + cardTotal);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public long getEntireServerMonthlyAmount() {
        try {
            long epoch = System.currentTimeMillis();
            long startOfMonth = CalendarUtil.getFirstDayOfMonth(epoch);
            long endOfMonth = CalendarUtil.getLastDayOfMonth(epoch);

            List<BankingPayment> bankingPayments = bankDao.queryBuilder()
                    .where().between("timestamp", startOfMonth, endOfMonth).query();
            List<CardPayment> cardPayments = cardDao.queryBuilder().where().between("timestamp", startOfMonth, endOfMonth).query();

            double bankingTotal = bankingPayments.stream()
                    .mapToDouble(BankingPayment::getAmount)
                    .sum();
            double cardTotal = cardPayments.stream()
                    .mapToDouble(CardPayment::getAmount)
                    .sum();
            return (long) (bankingTotal + cardTotal);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public long getEntireServerYearlyAmount() {
        try {
            long epoch = System.currentTimeMillis();
            long startOfYear = CalendarUtil.getFirstDayOfYear(epoch);
            long endOfYear = CalendarUtil.getLastDayOfYear(epoch);

            List<BankingPayment> bankingPayments = bankDao.queryBuilder()
                    .where().between("timestamp", startOfYear, endOfYear).query();
            List<CardPayment> cardPayments = cardDao.queryBuilder().where().between("timestamp", startOfYear, endOfYear).query();

            double bankingTotal = bankingPayments.stream()
                    .mapToDouble(BankingPayment::getAmount)
                    .sum();
            double cardTotal = cardPayments.stream()
                    .mapToDouble(CardPayment::getAmount)
                    .sum();
            return (long) (bankingTotal + cardTotal);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public long getPlayerDailyAmount(SPPlayer playerId) {
        try {
            long epoch = System.currentTimeMillis();
            long startOfDay = CalendarUtil.getFirstHourOfDay(epoch);
            long endOfDay = CalendarUtil.getLastHourOfDay(epoch);

            return queryForPlayerAmount(playerId, startOfDay, endOfDay);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public long getPlayerWeeklyAmount(SPPlayer playerId) {
        try {
            long epoch = System.currentTimeMillis();
            long startOfWeek = CalendarUtil.getFirstDayOfWeek(epoch);
            long endOfWeek = CalendarUtil.getLastDayOfWeek(epoch);

            return queryForPlayerAmount(playerId, startOfWeek, endOfWeek);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public long getPlayerMonthlyAmount(SPPlayer playerId) {
        try {
            long epoch = System.currentTimeMillis();
            long startOfMonth = CalendarUtil.getFirstDayOfMonth(epoch);
            long endOfMonth = CalendarUtil.getLastDayOfMonth(epoch);

            return queryForPlayerAmount(playerId, startOfMonth, endOfMonth);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public long getPlayerYearlyAmount(SPPlayer playerId) {
        try {
            long epoch = System.currentTimeMillis();
            long startOfYear = CalendarUtil.getFirstDayOfYear(epoch);
            long endOfYear = CalendarUtil.getLastDayOfYear(epoch);

            return queryForPlayerAmount(playerId, startOfYear, endOfYear);
        } catch (Exception e) {
            e.printStackTrace();
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

    /**
     * Batch query optimization - retrieves all time period amounts in optimized queries
     * Reduces 5 separate database calls to more efficient batched processing
     *
     * @param playerId Player to query
     * @return Map with keys: "total", "daily", "weekly", "monthly", "yearly"
     */
    public java.util.Map<String, Long> getPlayerAmountsBatch(SPPlayer playerId) {
        java.util.Map<String, Long> amounts = new java.util.HashMap<>();

        try {
            long epoch = System.currentTimeMillis();

            // Calculate all time boundaries
            long startOfDay = CalendarUtil.getFirstHourOfDay(epoch);
            long endOfDay = CalendarUtil.getLastHourOfDay(epoch);
            long startOfWeek = CalendarUtil.getFirstDayOfWeek(epoch);
            long endOfWeek = CalendarUtil.getLastDayOfWeek(epoch);
            long startOfMonth = CalendarUtil.getFirstDayOfMonth(epoch);
            long endOfMonth = CalendarUtil.getLastDayOfMonth(epoch);
            long startOfYear = CalendarUtil.getFirstDayOfYear(epoch);
            long endOfYear = CalendarUtil.getLastDayOfYear(epoch);

            // Fetch ALL payments for this player once
            List<BankingPayment> allBankingPayments = bankDao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerId)
                    .query();
            List<CardPayment> allCardPayments = cardDao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerId)
                    .query();

            // Calculate totals by filtering in-memory (much faster than 5 separate queries)
            long total = 0;
            long daily = 0;
            long weekly = 0;
            long monthly = 0;
            long yearly = 0;

            for (BankingPayment payment : allBankingPayments) {
                long amount = (long) payment.getAmount();
                long timestamp = payment.getTimestamp();
                total += amount;

                if (timestamp >= startOfDay && timestamp <= endOfDay) {
                    daily += amount;
                }
                if (timestamp >= startOfWeek && timestamp <= endOfWeek) {
                    weekly += amount;
                }
                if (timestamp >= startOfMonth && timestamp <= endOfMonth) {
                    monthly += amount;
                }
                if (timestamp >= startOfYear && timestamp <= endOfYear) {
                    yearly += amount;
                }
            }

            for (CardPayment payment : allCardPayments) {
                long amount = (long) payment.getAmount();
                long timestamp = payment.getTimestamp();
                total += amount;

                if (timestamp >= startOfDay && timestamp <= endOfDay) {
                    daily += amount;
                }
                if (timestamp >= startOfWeek && timestamp <= endOfWeek) {
                    weekly += amount;
                }
                if (timestamp >= startOfMonth && timestamp <= endOfMonth) {
                    monthly += amount;
                }
                if (timestamp >= startOfYear && timestamp <= endOfYear) {
                    yearly += amount;
                }
            }

            amounts.put("total", total);
            amounts.put("daily", daily);
            amounts.put("weekly", weekly);
            amounts.put("monthly", monthly);
            amounts.put("yearly", yearly);

        } catch (Exception e) {
            e.printStackTrace();
            // Return zeros on error
            amounts.put("total", 0L);
            amounts.put("daily", 0L);
            amounts.put("weekly", 0L);
            amounts.put("monthly", 0L);
            amounts.put("yearly", 0L);
        }

        return amounts;
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
