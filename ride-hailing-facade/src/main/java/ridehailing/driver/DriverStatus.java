package ridehailing.driver;

public enum DriverStatus {
    FREE,
    RESERVED,
    BUSY;

    /**
     * Allowed changes: FREE -> RESERVED, RESERVED -> BUSY,
     * RESERVED -> FREE (reservation cancelled), BUSY -> FREE (trip finished).
     */
    public boolean isAllowedToChangeTo(DriverStatus newStatus) {
        if (this == FREE) {
            return newStatus == RESERVED;
        }
        if (this == RESERVED) {
            return newStatus == BUSY || newStatus == FREE;
        }
        return newStatus == FREE;   // this is BUSY
    }
}
