package org.simpmc.simppay.data.card.thesieuviet;

import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.data.card.CardPrice;

public class TSVCardAdapter {

    public static PaymentStatus parseStatus(int status) {
        switch (status) {
            case 1:
                return PaymentStatus.SUCCESS;
            case 2:
                return PaymentStatus.SUCCESS_WRONG_AMOUNT;
            case 3:
            case 4:
            case 100:
                return PaymentStatus.FAILED;
            case 99:
                return PaymentStatus.PENDING;
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