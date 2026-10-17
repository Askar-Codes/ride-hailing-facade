package ridehailing.payment;

/**
 * The entity of subsystem C. It guards its own status changes.
 * A driver rating of 0 means "not rated yet".
 */
public class Payment {

    private final String paymentId;
    private final String rideOrderId;
    private final String cardLastFourDigits;
    private final double amount;
    private PaymentStatus status;
    private int driverRatingInStars;

    public Payment(String paymentId, String rideOrderId, String cardLastFourDigits, double amount,
                   PaymentStatus status, int driverRatingInStars) {
        this.paymentId = paymentId;
        this.rideOrderId = rideOrderId;
        this.cardLastFourDigits = cardLastFourDigits;
        this.amount = amount;
        this.status = status;
        this.driverRatingInStars = driverRatingInStars;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getRideOrderId() {
        return rideOrderId;
    }

    public String getCardLastFourDigits() {
        return cardLastFourDigits;
    }

    public double getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public int getDriverRatingInStars() {
        return driverRatingInStars;
    }

    public void setDriverRatingInStars(int driverRatingInStars) {
        this.driverRatingInStars = driverRatingInStars;
    }

    public void markAsCharged() {
        changeStatusTo(PaymentStatus.CHARGED);
    }

    public void markAsDeclined() {
        changeStatusTo(PaymentStatus.DECLINED);
    }

    public void markAsRefunded() {
        changeStatusTo(PaymentStatus.REFUNDED);
    }

    private void changeStatusTo(PaymentStatus newStatus) {
        if (!status.isAllowedToChangeTo(newStatus)) {
            throw new IllegalStateException(
                    "Payment " + paymentId + " cannot change from " + status + " to " + newStatus);
        }
        status = newStatus;
    }
}
