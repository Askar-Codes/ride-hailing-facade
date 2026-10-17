package ridehailing.pricing;

/** The entity of subsystem B. It guards its own status changes. */
public class Receipt {

    private final String receiptId;
    private final String rideOrderId;
    private final String riderName;
    private final double amountToPay;
    private ReceiptStatus status;

    public Receipt(String receiptId, String rideOrderId, String riderName, double amountToPay, ReceiptStatus status) {
        this.receiptId = receiptId;
        this.rideOrderId = rideOrderId;
        this.riderName = riderName;
        this.amountToPay = amountToPay;
        this.status = status;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public String getRideOrderId() {
        return rideOrderId;
    }

    public String getRiderName() {
        return riderName;
    }

    public double getAmountToPay() {
        return amountToPay;
    }

    public ReceiptStatus getStatus() {
        return status;
    }

    public void markAsPaid() {
        changeStatusTo(ReceiptStatus.PAID);
    }

    public void markAsVoided() {
        changeStatusTo(ReceiptStatus.VOIDED);
    }

    private void changeStatusTo(ReceiptStatus newStatus) {
        if (!status.isAllowedToChangeTo(newStatus)) {
            throw new IllegalStateException(
                    "Receipt " + receiptId + " cannot change from " + status + " to " + newStatus);
        }
        status = newStatus;
    }
}
