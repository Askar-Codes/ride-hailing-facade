package ridehailing.strategy.matching;

/** Our own window to the outside world: facts about a driver that the Driver class does not keep. */
public interface DriverStatsSource {

    double getAverageRatingOf(String driverId);

    int getMinutesSinceLastRideOf(String driverId);
}
