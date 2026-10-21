package ridehailing.strategy.pricing;

import ridehailing.strategy.TaxiClass;

/** Everything a pricing algorithm needs to know about one ride. */
public class PriceInput {

    private final double distanceInKilometers;
    private final String cityZoneName;
    private final TaxiClass taxiClass;

    public PriceInput(double distanceInKilometers, String cityZoneName, TaxiClass taxiClass) {
        this.distanceInKilometers = distanceInKilometers;
        this.cityZoneName = cityZoneName;
        this.taxiClass = taxiClass;
    }

    public double getDistanceInKilometers() {
        return distanceInKilometers;
    }

    public String getCityZoneName() {
        return cityZoneName;
    }

    public TaxiClass getTaxiClass() {
        return taxiClass;
    }
}
