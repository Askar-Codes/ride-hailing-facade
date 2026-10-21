package ridehailing.strategy.matching;

import ridehailing.driver.CityMapPoint;
import ridehailing.driver.Driver;

import java.util.Comparator;
import java.util.List;

/** Chooses the free driver who has waited longest since his last ride. */
public class LongestIdleDriverStrategy implements DriverMatchingStrategy {

    private final DriverStatsSource driverStatsSource;

    public LongestIdleDriverStrategy(DriverStatsSource driverStatsSource) {
        this.driverStatsSource = driverStatsSource;
    }

    @Override
    public Driver chooseDriver(List<Driver> allDrivers, CityMapPoint pickupPoint) {
        return FreeDriversFilter.keepOnlyFreeDriversOrThrow(allDrivers).stream()
                .max(Comparator.comparingInt(driver -> driverStatsSource.getMinutesSinceLastRideOf(driver.getDriverId())))
                .orElseThrow();
    }
}
