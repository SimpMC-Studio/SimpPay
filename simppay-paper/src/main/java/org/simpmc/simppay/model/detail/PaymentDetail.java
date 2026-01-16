package org.simpmc.simppay.model.detail;

public interface PaymentDetail {
    double getAmount();

    PaymentDetail setAmount(int amount);

    String getRefID();

    void setRefID(String refID);

    String getDescription();

    String getQRCode();
}
