package org.simpmc.simppay.model.detail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class BankingDetail implements PaymentDetail {

    private double amount;
    private String refID;
    private String description;
    private String QRCode;

    @Override
    public double getAmount() {
        return amount;
    }

    @Override
    public String getRefID() {
        return refID;
    }

    @Override
    public PaymentDetail setAmount(int amount) {
        this.amount = amount;
        return this;
    }

}
