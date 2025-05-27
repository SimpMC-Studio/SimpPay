package org.simpmc.simppay.database.entities;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.data.card.CardType;
import org.simpmc.simppay.handler.data.CardAPI;
import org.simpmc.simppay.model.Payment;
import org.simpmc.simppay.model.detail.CardDetail;
import org.simpmc.simppay.service.PaymentService;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@DatabaseTable(tableName = "card_payments")
public class CardPayment {

    @DatabaseField(columnName = "payment_id", id = true, dataType = DataType.UUID)
    private UUID paymentID;

    @DatabaseField(columnName = "player_uuid", foreign = true, foreignAutoRefresh = true, canBeNull = false)
    private SPPlayer player;

    @DatabaseField(columnName = "pin", canBeNull = false)
    private String pin;

    @DatabaseField(columnName = "serial", canBeNull = false)
    private String serial;

    @DatabaseField(columnName = "price_value", canBeNull = false)
    private double priceValue;

    @DatabaseField(columnName = "card_type", canBeNull = false, dataType = DataType.ENUM_NAME)
    private CardType cardType;

    @DatabaseField(columnName = "ref_id", canBeNull = false)
    private String refID;

    @DatabaseField(columnName = "true_amount", canBeNull = false)
    private double trueAmount; // the amount that the server receive after the service provider cut the fee

    @DatabaseField(columnName = "amount", canBeNull = false)
    private double amount;

    @DatabaseField(columnName = "timestamp", canBeNull = false, dataType = DataType.LONG)
    private long timestamp;

    @DatabaseField(columnName = "api_provider", canBeNull = false)
    private CardAPI apiProvider;

    public CardPayment(Payment payment) {
        this.paymentID = payment.getPaymentID();
        // may cause trouble if null, but player should already be created on join
        this.player = SPPlugin.getInstance().getDatabaseService().getPlayerService().findByUuid(payment.getPlayerUUID());
        this.pin = ((CardDetail) payment.getDetail()).getPin();
        this.serial = ((CardDetail) payment.getDetail()).getSerial();
        this.priceValue = ((CardDetail) payment.getDetail()).getPrice().getValue();
        this.cardType = ((CardDetail) payment.getDetail()).getType();
        this.refID = payment.getDetail().getRefID();
        this.trueAmount = ((CardDetail) payment.getDetail()).getTrueAmount();
        this.amount = payment.getDetail().getAmount();
        this.apiProvider = PaymentService.getCardAPI();
        this.timestamp = Instant.now().toEpochMilli();
    }
}
