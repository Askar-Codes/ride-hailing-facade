package ridehailing.driver;

/** A place on a flat city map. Both coordinates are in kilometres. */
public class CityMapPoint {

    private final double xInKilometers;
    private final double yInKilometers;

    public CityMapPoint(double xInKilometers, double yInKilometers) {
        this.xInKilometers = xInKilometers;
        this.yInKilometers = yInKilometers;
    }

    public double getXInKilometers() {
        return xInKilometers;
    }

    public double getYInKilometers() {
        return yInKilometers;
    }

    public double calculateDistanceInKilometersTo(CityMapPoint otherPoint) {
        double differenceInX = xInKilometers - otherPoint.xInKilometers;
        double differenceInY = yInKilometers - otherPoint.yInKilometers;
        return Math.sqrt(differenceInX * differenceInX + differenceInY * differenceInY);
    }
}
