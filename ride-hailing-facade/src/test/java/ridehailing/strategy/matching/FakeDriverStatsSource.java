package ridehailing.strategy.matching;

import java.util.Map;

/** Test double: ratings and idle minutes are fixed in the constructor. */
public class FakeDriverStatsSource implements DriverStatsSource {

    private final Map<String, Double> ratingsByDriverId;
    private final Map<String, Integer> idleMinutesByDriverId;

    public FakeDriverStatsSource(Map<String, Double> ratingsByDriverId, Map<String, Integer> idleMinutesByDriverId) {
        this.ratingsByDriverId = ratingsByDriverId;
        this.idleMinutesByDriverId = idleMinutesByDriverId;
    }

    @Override
    public double getAverageRatingOf(String driverId) {
        return ratingsByDriverId.get(driverId);
    }

    @Override
    public int getMinutesSinceLastRideOf(String driverId) {
        return idleMinutesByDriverId.get(driverId);
    }
}
