package ridehailing.strategy.matching;

import org.junit.jupiter.api.Test;
import ridehailing.driver.CityMapPoint;
import ridehailing.driver.Driver;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriverMatchingContextTest {

    private final Driver first = new Driver("first", "First", new CityMapPoint(1, 1));
    private final Driver second = new Driver("second", "Second", new CityMapPoint(2, 2));
    private final List<Driver> drivers = List.of(first, second);
    private final CityMapPoint pickupPoint = new CityMapPoint(0, 0);

    @Test
    void contextReturnsWhatTheStrategyChooses() {
        DriverMatchingContext context = new DriverMatchingContext((allDrivers, point) -> first);
        assertEquals(first, context.chooseDriver(drivers, pickupPoint));
    }

    @Test
    void contextUsesTheNewStrategyAfterSetStrategy() {
        DriverMatchingContext context = new DriverMatchingContext((allDrivers, point) -> first);
        context.setStrategy((allDrivers, point) -> second);
        assertEquals(second, context.chooseDriver(drivers, pickupPoint));
    }
}
