package ridehailing.strategy.matching;

import ridehailing.driver.CityMapPoint;
import ridehailing.driver.Driver;

import java.util.List;

/** One way to choose the driver for a ride. */
public interface DriverMatchingStrategy {

    /** Chooses among the free drivers of the list, throws NoFreeDriverException if there is none. */
    Driver chooseDriver(List<Driver> allDrivers, CityMapPoint pickupPoint);
}
