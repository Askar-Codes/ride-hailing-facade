package ridehailing.payment;

public enum PaymentStatus {
    NEW,
    CHARGED,
    DECLINED,
    REFUNDED;

    /** Allowed changes: NEW -> CHARGED, NEW -> DECLINED, CHARGED -> REFUNDED. */
    public boolean isAllowedToChangeTo(PaymentStatus newStatus) {
        if (this == NEW) {
            return newStatus == CHARGED || newStatus == DECLINED;
        }
        if (this == CHARGED) {
            return newStatus == REFUNDED;
        }
        return false;   // DECLINED and REFUNDED never change again
    }
}
