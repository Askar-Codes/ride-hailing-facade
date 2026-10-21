package ridehailing.strategy.matching;

import ridehailing.strategy.TaxiClass;

import java.time.LocalTime;

/** Picks the matching strategy that fits the taxi class and the time of day. */
public class DriverMatchingStrategySelector {

    private static final int NIGHT_STARTS_AT_HOUR = 22;
    private static final int NIGHT_ENDS_AT_HOUR = 6;

    private final DriverMatchingStrategy nearestDriverStrategy = new NearestDriverStrategy();
    private final DriverMatchingStrategy highestRatedDriverStrategy;
    private final DriverMatchingStrategy longestIdleDriverStrategy;

    public DriverMatchingStrategySelector(DriverStatsSource driverStatsSource) {
        this.highestRatedDriverStrategy = new HighestRatedDriverStrategy(driverStatsSource);
        this.longestIdleDriverStrategy = new LongestIdleDriverStrategy(driverStatsSource);
    }

    public DriverMatchingStrategy chooseStrategyFor(TaxiClass taxiClass, LocalTime timeOfOrder) {
        if (taxiClass == TaxiClass.PREMIUM) {
            return highestRatedDriverStrategy;
        }
        boolean isNight = timeOfOrder.getHour() >= NIGHT_STARTS_AT_HOUR || timeOfOrder.getHour() < NIGHT_ENDS_AT_HOUR;
        if (isNight) {
            return longestIdleDriverStrategy;
        }
        return nearestDriverStrategy;
    }
}
