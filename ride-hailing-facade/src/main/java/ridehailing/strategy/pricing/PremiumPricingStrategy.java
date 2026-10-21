package ridehailing.strategy.pricing;

/** Comfort fee plus one flat rate per kilometre, the same in every zone. */
public class PremiumPricingStrategy implements PricingStrategy {

    private static final double COMFORT_FEE = 800.0;
    private static final double PRICE_PER_KILOMETER = 220.0;

    @Override
    public double calculatePrice(PriceInput priceInput) {
        return COMFORT_FEE + priceInput.getDistanceInKilometers() * PRICE_PER_KILOMETER;
    }
}
