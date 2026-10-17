package ridehailing.pricing;

public enum ReceiptStatus {
    ISSUED,
    PAID,
    VOIDED;

    /** Allowed changes: ISSUED -> PAID, ISSUED -> VOIDED, PAID -> VOIDED (money was refunded). */
    public boolean isAllowedToChangeTo(ReceiptStatus newStatus) {
        if (this == ISSUED) {
            return newStatus == PAID || newStatus == VOIDED;
        }
        if (this == PAID) {
            return newStatus == VOIDED;
        }
        return false;   // a VOIDED receipt never changes again
    }
}
