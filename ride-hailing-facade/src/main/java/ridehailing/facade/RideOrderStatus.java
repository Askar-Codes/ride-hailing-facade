package ridehailing.facade;

/** How far the A -> B -> C chain got. This is what the facade reads back from disk after a restart. */
public enum RideOrderStatus {
    PENDING_B,    // step A is done (driver reserved), step B is next
    PENDING_C,    // steps A and B are done (receipt issued), step C is next
    COMPLETED,    // all three steps are done
    COMPENSATED;  // the ride was rolled back

    public boolean isAllowedToChangeTo(RideOrderStatus newStatus) {
        if (this == PENDING_B) {
            return newStatus == PENDING_C || newStatus == COMPENSATED;
        }
        if (this == PENDING_C) {
            return newStatus == COMPLETED || newStatus == COMPENSATED;
        }
        if (this == COMPLETED) {
            return newStatus == COMPENSATED;
        }
        return false;   // a COMPENSATED ride never changes again
    }
}
