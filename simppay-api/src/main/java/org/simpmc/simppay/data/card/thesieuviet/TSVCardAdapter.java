package org.simpmc.simppay.data.card.thesieuviet;

import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.data.card.CardPrice;

public class TSVCardAdapter {

    public static PaymentStatus parseStatus(String status) {
        switch (status.toLowerCase()) {
            case "success":
                return PaymentStatus.SUCCESS;
            case "pending":
                return PaymentStatus.PENDING;
            case "wrong_value":
                return PaymentStatus.WRONG_PRICE;
            case "invalid":
                return PaymentStatus.INVALID;
            default:
                return PaymentStatus.FAILED;
        }
    }

    public static int getCardPriceValue(CardPrice cardPrice) {
        return switch (cardPrice) {
            case _10K -> 10000;
            case _20K -> 20000;
            case _30K -> 30000;
            case _50K -> 50000;
            case _100K -> 100000;
            case _200K -> 200000;
            case _300K -> 300000;
            case _500K -> 500000;
            case _1000K -> 1000000;
        };
    }
}