package ridehailing.strategy.matching;

import ridehailing.driver.CityMapPoint;
import ridehailing.driver.Driver;

import java.util.Comparator;
import java.util.List;

/** Chooses the free driver with the smallest distance to the pickup point. */
public class NearestDriverStrategy implements DriverMatchingStrategy {

    @Override
    public Driver chooseDriver(List<Driver> allDrivers, CityMapPoint pickupPoint) {
        return FreeDriversFilter.keepOnlyFreeDriversOrThrow(allDrivers).stream()
                .min(Comparator.comparingDouble(driver -> driver.getCurrentLocation().calculateDistanceInKilometersTo(pickupPoint)))
                .orElseThrow();
    }
}
