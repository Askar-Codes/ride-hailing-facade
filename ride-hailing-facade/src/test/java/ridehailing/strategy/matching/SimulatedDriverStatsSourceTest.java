package ridehailing.strategy.matching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulatedDriverStatsSourceTest {

    private final SimulatedDriverStatsSource source = new SimulatedDriverStatsSource();

    @Test
    void sameDriverAlwaysGetsTheSameNumbers() {
        assertEquals(source.getAverageRatingOf("D1"), source.getAverageRatingOf("D1"), 0.0001);
        assertEquals(source.getMinutesSinceLastRideOf("D1"), source.getMinutesSinceLastRideOf("D1"));
    }

    @Test
    void ratingIsBetweenOneAndFiveAndIdleTimeIsNotNegative() {
        for (String driverId : new String[]{"D1", "D2", "D3", "some-long-id"}) {
            assertTrue(source.getAverageRatingOf(driverId) >= 1.0 && source.getAverageRatingOf(driverId) <= 5.0);
            assertTrue(source.getMinutesSinceLastRideOf(driverId) >= 0);
        }
    }
}
