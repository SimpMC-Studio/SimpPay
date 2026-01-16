package org.simpmc.simppay.handler;

import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.model.Payment;
import org.simpmc.simppay.model.PaymentResult;
import org.simpmc.simppay.model.detail.PaymentDetail;

public interface PaymentHandler {

    PaymentStatus processPayment(Payment payment); // should only return pending or exist

    PaymentResult getTransactionResult(PaymentDetail detail);

    PaymentStatus cancel(Payment payment);
}
