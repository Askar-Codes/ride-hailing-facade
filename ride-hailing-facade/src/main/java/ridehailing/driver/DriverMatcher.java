package ridehailing.driver;

import java.util.List;

/** Business logic only: decides which driver gets the ride. */
public class DriverMatcher {

    public Driver findNearestFreeDriver(List<Driver> allDrivers, CityMapPoint pickupPoint) {
        Driver nearestFreeDriverFoundSoFar = null;
        double distanceOfNearestFreeDriver = 0;

        for (Driver driver : allDrivers) {
            if (!driver.isFreeForNewRide()) {
                continue;
            }
            double distanceOfThisDriver = driver.getCurrentLocation().calculateDistanceInKilometersTo(pickupPoint);
            if (nearestFreeDriverFoundSoFar == null || distanceOfThisDriver < distanceOfNearestFreeDriver) {
                nearestFreeDriverFoundSoFar = driver;
                distanceOfNearestFreeDriver = distanceOfThisDriver;
            }
        }

        if (nearestFreeDriverFoundSoFar == null) {
            throw new NoFreeDriverException("No free driver is available right now");
        }
        return nearestFreeDriverFoundSoFar;
    }
}
