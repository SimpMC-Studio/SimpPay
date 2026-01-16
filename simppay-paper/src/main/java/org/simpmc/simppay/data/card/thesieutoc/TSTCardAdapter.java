package org.simpmc.simppay.data.card.thesieutoc;

import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.data.card.CardPrice;

public class TSTCardAdapter {
    public static PaymentStatus getCardStatus(int statusCode) {
        return switch (statusCode) {
            case 00 -> PaymentStatus.SUCCESS;
            case 10 -> PaymentStatus.WRONG_PRICE; // should contain amount field along with actual price
            case -10 -> PaymentStatus.INVALID;
            case -9 -> PaymentStatus.PENDING;
            case 2 -> PaymentStatus.FAILED;
            default -> PaymentStatus.FAILED;
        };
    }

    public static int getCardPriceID(CardPrice cardPrice) {
        return switch (cardPrice) {
            case _10K -> 1;
            case _20K -> 2;
            case _30K -> 3;
            case _50K -> 4;
            case _100K -> 5;
            case _200K -> 6;
            case _300K -> 7;
            case _500K -> 8;
            case _1000K -> 9;
        };
    }

}
