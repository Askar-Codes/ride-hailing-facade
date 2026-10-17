package ridehailing.pricing;

import java.util.UUID;

/** Business logic only: builds a new receipt. Saving it is the repository's job. */
public class ReceiptGenerator {

    public Receipt generateReceipt(String rideOrderId, String riderName, double amountToPay) {
        String receiptId = "RCT-" + UUID.randomUUID().toString().substring(0, 8);
        return new Receipt(receiptId, rideOrderId, riderName, amountToPay, ReceiptStatus.ISSUED);
    }
}
