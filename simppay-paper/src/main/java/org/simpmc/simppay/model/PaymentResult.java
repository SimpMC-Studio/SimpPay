package org.simpmc.simppay.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.simpmc.simppay.data.PaymentStatus;

@Data
@AllArgsConstructor
public class PaymentResult {
    public PaymentStatus status;
    public int amount;
    public String message;
}
