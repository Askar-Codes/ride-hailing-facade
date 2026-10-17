package ridehailing.driver;

/** The entity of subsystem A. It guards its own status changes. */
public class Driver {

    private final String driverId;
    private final String driverName;
    private final CityMapPoint currentLocation;
    private DriverStatus currentStatus;

    public Driver(String driverId, String driverName, CityMapPoint currentLocation) {
        this(driverId, driverName, currentLocation, DriverStatus.FREE);
    }

    public Driver(String driverId, String driverName, CityMapPoint currentLocation, DriverStatus currentStatus) {
        this.driverId = driverId;
        this.driverName = driverName;
        this.currentLocation = currentLocation;
        this.currentStatus = currentStatus;
    }

    public String getDriverId() {
        return driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public CityMapPoint getCurrentLocation() {
        return currentLocation;
    }

    public DriverStatus getCurrentStatus() {
        return currentStatus;
    }

    public boolean isFreeForNewRide() {
        return currentStatus == DriverStatus.FREE;
    }

    public void reserveForRide() {
        changeStatusTo(DriverStatus.RESERVED);
    }

    public void startDrivingRide() {
        changeStatusTo(DriverStatus.BUSY);
    }

    public void becomeFreeAgain() {
        changeStatusTo(DriverStatus.FREE);
    }

    private void changeStatusTo(DriverStatus newStatus) {
        if (!currentStatus.isAllowedToChangeTo(newStatus)) {
            throw new IllegalStateException(
                    "Driver " + driverName + " cannot change from " + currentStatus + " to " + newStatus);
        }
        currentStatus = newStatus;
    }
}
