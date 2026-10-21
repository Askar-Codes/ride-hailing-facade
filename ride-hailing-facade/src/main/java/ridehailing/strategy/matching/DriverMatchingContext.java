package ridehailing.strategy.matching;

import ridehailing.driver.CityMapPoint;
import ridehailing.driver.Driver;

import java.util.List;

/** Holds the current driver matching strategy and asks it for the driver. */
public class DriverMatchingContext {

    private DriverMatchingStrategy driverMatchingStrategy;

    public DriverMatchingContext(DriverMatchingStrategy driverMatchingStrategy) {
        this.driverMatchingStrategy = driverMatchingStrategy;
    }

    public void setStrategy(DriverMatchingStrategy driverMatchingStrategy) {
        this.driverMatchingStrategy = driverMatchingStrategy;
    }

    public Driver chooseDriver(List<Driver> allDrivers, CityMapPoint pickupPoint) {
        return driverMatchingStrategy.chooseDriver(allDrivers, pickupPoint);
    }
}
