package org.simpmc.simppay.data.bank.payos;

import org.simpmc.simppay.data.PaymentStatus;

public class PayosAdapter {
    public static PaymentStatus getStatus(String statusCode) {
        return switch (statusCode) {
            case "PAID" -> PaymentStatus.SUCCESS;
            case "PENDING", "PROCESSING" -> PaymentStatus.PENDING;
            case "CANCELLED" -> PaymentStatus.CANCELLED;
            case "EXPIRED" -> PaymentStatus.EXPIRED;
            case null, default -> PaymentStatus.FAILED;
        };
    }
}
