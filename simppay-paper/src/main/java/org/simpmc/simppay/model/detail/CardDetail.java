package org.simpmc.simppay.model.detail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.simpmc.simppay.data.card.CardPrice;
import org.simpmc.simppay.data.card.CardType;

@Data
@AllArgsConstructor
@Builder
public class CardDetail implements PaymentDetail {
    public String pin;
    public String serial;
    public CardPrice price;
    public CardType type;

    public String refID;
    public double trueAmount;

    @Override
    public double getAmount() {
        return price.getValue();
    }

    @Override
    public PaymentDetail setAmount(int amount) {
        price = CardPrice.fromValue(amount);
        return this;
    }

    @Override
    public String getRefID() {
        return refID;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getQRCode() {
        return null;
    }
}
