package ridehailing.driver;

/** Business logic only (no state, no storage): straight-line distance and travel time. */
public class GpsRouter {

    private static final double AVERAGE_SPEED_IN_KILOMETERS_PER_HOUR = 40.0;

    public double calculateDistanceInKilometersBetween(CityMapPoint startPoint, CityMapPoint endPoint) {
        return startPoint.calculateDistanceInKilometersTo(endPoint);
    }

    public int calculateEstimatedTravelTimeInMinutes(double distanceInKilometers) {
        return (int) Math.ceil(distanceInKilometers / AVERAGE_SPEED_IN_KILOMETERS_PER_HOUR * 60);
    }
}
