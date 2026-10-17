package ridehailing.pricing;

import java.util.Map;

/** Business logic only: price = base fare + distance * price per kilometre of the zone. */
public class PricingEngine {

    private static final double BASE_FARE = 500.0;
    private static final double MAX_ALLOWED_PRICE = 5000.0;

    private final Map<String, Double> pricePerKilometerByZoneName = Map.of(
            "CENTER", 100.0,
            "SUBURB", 130.0,
            "AIRPORT", 180.0);

    public double calculatePrice(double distanceInKilometers, String cityZoneName) {
        if (cityZoneName == null || !pricePerKilometerByZoneName.containsKey(cityZoneName.trim().toUpperCase())) {
            throw new PricingException("No tariff is defined for zone '" + cityZoneName + "'");
        }
        double pricePerKilometer = pricePerKilometerByZoneName.get(cityZoneName.trim().toUpperCase());

        double price = Math.round((BASE_FARE + distanceInKilometers * pricePerKilometer) * 100) / 100.0;
        if (price > MAX_ALLOWED_PRICE) {
            throw new PricingException(
                    "Price " + price + " is above the allowed maximum " + MAX_ALLOWED_PRICE);
        }
        return price;
    }
}
