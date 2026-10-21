package ridehailing.strategy.matching;

import ridehailing.driver.CityMapPoint;
import ridehailing.driver.Driver;

import java.util.Comparator;
import java.util.List;

/** Chooses the free driver with the best average rating, wherever he is. */
public class HighestRatedDriverStrategy implements DriverMatchingStrategy {

    private final DriverStatsSource driverStatsSource;

    public HighestRatedDriverStrategy(DriverStatsSource driverStatsSource) {
        this.driverStatsSource = driverStatsSource;
    }

    @Override
    public Driver chooseDriver(List<Driver> allDrivers, CityMapPoint pickupPoint) {
        return FreeDriversFilter.keepOnlyFreeDriversOrThrow(allDrivers).stream()
                .max(Comparator.comparingDouble(driver -> driverStatsSource.getAverageRatingOf(driver.getDriverId())))
                .orElseThrow();
    }
}
