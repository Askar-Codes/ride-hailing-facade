package ridehailing.facade;

import java.util.UUID;

/**
 * The saved progress of one ride. It stores only the ids of the records that
 * subsystems A, B and C created, so it does not depend on their classes.
 * An empty receiptId or paymentId means "that step has not happened".
 */
public class RideOrder {

    private final String rideOrderId;
    private final String riderName;
    private final String driverId;
    private String receiptId;
    private String paymentId;
    private RideOrderStatus status;

    /** A brand new ride: step A is about to happen. */
    public RideOrder(String riderName, String driverId) {
        this("ORD-" + UUID.randomUUID().toString().substring(0, 8),
                riderName, driverId, "", "", RideOrderStatus.PENDING_B);
    }

    public RideOrder(String rideOrderId, String riderName, String driverId,
                     String receiptId, String paymentId, RideOrderStatus status) {
        this.rideOrderId = rideOrderId;
        this.riderName = riderName;
        this.driverId = driverId;
        this.receiptId = receiptId;
        this.paymentId = paymentId;
        this.status = status;
    }

    public String getRideOrderId() {
        return rideOrderId;
    }

    public String getRiderName() {
        return riderName;
    }

    public String getDriverId() {
        return driverId;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public RideOrderStatus getStatus() {
        return status;
    }

    public boolean hasReceiptAttached() {
        return !receiptId.isEmpty();
    }

    public boolean hasPaymentAttached() {
        return !paymentId.isEmpty();
    }

    /** Step B is done: PENDING_B -> PENDING_C. */
    public void attachReceiptAndMoveToPendingC(String receiptId) {
        changeStatusTo(RideOrderStatus.PENDING_C);
        this.receiptId = receiptId;
    }

    /** Step C was tried (the payment can be charged or declined). The status stays PENDING_C. */
    public void attachPayment(String paymentId) {
        if (status != RideOrderStatus.PENDING_C) {
            throw new IllegalStateException(
                    "Ride " + rideOrderId + " cannot get a payment while it is " + status);
        }
        this.paymentId = paymentId;
    }

    public void markAsCompleted() {
        changeStatusTo(RideOrderStatus.COMPLETED);
    }

    public void markAsCompensated() {
        changeStatusTo(RideOrderStatus.COMPENSATED);
    }

    private void changeStatusTo(RideOrderStatus newStatus) {
        if (!status.isAllowedToChangeTo(newStatus)) {
            throw new IllegalStateException(
                    "Ride " + rideOrderId + " cannot change from " + status + " to " + newStatus);
        }
        status = newStatus;
    }
}
