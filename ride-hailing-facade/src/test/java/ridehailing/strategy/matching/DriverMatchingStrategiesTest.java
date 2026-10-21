package ridehailing.strategy.matching;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ridehailing.driver.CityMapPoint;
import ridehailing.driver.Driver;
import ridehailing.driver.DriverStatus;
import ridehailing.driver.NoFreeDriverException;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriverMatchingStrategiesTest {

    private static final CityMapPoint PICKUP_POINT = new CityMapPoint(0, 0);

    // near: close, bad rating, just worked | star: far, best rating | rested: medium, waited longest
    private static final Driver NEAR = new Driver("near", "Near", new CityMapPoint(1, 0));
    private static final Driver STAR = new Driver("star", "Star", new CityMapPoint(9, 0));
    private static final Driver RESTED = new Driver("rested", "Rested", new CityMapPoint(5, 0));
    private static final Driver BUSY_BUT_NEAREST = new Driver("busy", "Busy", new CityMapPoint(0, 1), DriverStatus.BUSY);
    private static final List<Driver> ALL_DRIVERS = List.of(BUSY_BUT_NEAREST, NEAR, STAR, RESTED);

    private static final DriverStatsSource FAKE_STATS = new FakeDriverStatsSource(
            Map.of("near", 3.0, "star", 4.9, "rested", 4.0, "busy", 5.0),
            Map.of("near", 2, "star", 10, "rested", 90, "busy", 500));

    static Stream<DriverMatchingStrategy> allStrategies() {
        return Stream.of(
                new NearestDriverStrategy(),
                new HighestRatedDriverStrategy(FAKE_STATS),
                new LongestIdleDriverStrategy(FAKE_STATS));
    }

    @Test
    void nearestStrategyChoosesTheClosestFreeDriver() {
        assertEquals(NEAR, new NearestDriverStrategy().chooseDriver(ALL_DRIVERS, PICKUP_POINT));
    }

    @Test
    void highestRatedStrategyChoosesTheBestRatedFreeDriver() {
        assertEquals(STAR, new HighestRatedDriverStrategy(FAKE_STATS).chooseDriver(ALL_DRIVERS, PICKUP_POINT));
    }

    @Test
    void longestIdleStrategyChoosesTheFreeDriverWhoWaitedLongest() {
        assertEquals(RESTED, new LongestIdleDriverStrategy(FAKE_STATS).chooseDriver(ALL_DRIVERS, PICKUP_POINT));
    }

    @ParameterizedTest
    @MethodSource("allStrategies")
    void everyStrategyChoosesOnlyAFreeDriver(DriverMatchingStrategy strategy) {
        assertTrue(strategy.chooseDriver(ALL_DRIVERS, PICKUP_POINT).isFreeForNewRide());
    }

    @ParameterizedTest
    @MethodSource("allStrategies")
    void everyStrategyFailsWhenNobodyIsFree(DriverMatchingStrategy strategy) {
        assertThrows(NoFreeDriverException.class, () -> strategy.chooseDriver(List.of(BUSY_BUT_NEAREST), PICKUP_POINT));
    }
}
