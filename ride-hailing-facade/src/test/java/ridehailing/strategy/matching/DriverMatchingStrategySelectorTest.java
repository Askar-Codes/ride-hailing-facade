package ridehailing.strategy.matching;

import org.junit.jupiter.api.Test;
import ridehailing.strategy.TaxiClass;

import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DriverMatchingStrategySelectorTest {

    private final DriverMatchingStrategySelector selector =
            new DriverMatchingStrategySelector(new FakeDriverStatsSource(Map.of(), Map.of()));

    @Test
    void premiumTaxiGetsTheHighestRatedDriverEvenAtNight() {
        assertTrue(selector.chooseStrategyFor(TaxiClass.PREMIUM, LocalTime.of(23, 0)) instanceof HighestRatedDriverStrategy);
    }

    @Test
    void regularTaxiAtNightGetsTheLongestIdleDriver() {
        assertTrue(selector.chooseStrategyFor(TaxiClass.REGULAR, LocalTime.of(23, 0)) instanceof LongestIdleDriverStrategy);
    }

    @Test
    void regularTaxiBeforeDawnGetsTheLongestIdleDriver() {
        assertTrue(selector.chooseStrategyFor(TaxiClass.REGULAR, LocalTime.of(3, 0)) instanceof LongestIdleDriverStrategy);
    }

    @Test
    void regularTaxiByDayGetsTheNearestDriver() {
        assertTrue(selector.chooseStrategyFor(TaxiClass.REGULAR, LocalTime.of(14, 0)) instanceof NearestDriverStrategy);
    }
}
