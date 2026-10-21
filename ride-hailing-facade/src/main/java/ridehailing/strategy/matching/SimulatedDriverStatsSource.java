package ridehailing.strategy.matching;

/** Simulated outside source: repeatable rating and idle time made from the driver id. */
public class SimulatedDriverStatsSource implements DriverStatsSource {

    @Override
    public double getAverageRatingOf(String driverId) {
        return 3.5 + Math.abs(driverId.hashCode() % 15) / 10.0;
    }

    @Override
    public int getMinutesSinceLastRideOf(String driverId) {
        return Math.abs(driverId.hashCode() / 7 % 60);
    }
}
