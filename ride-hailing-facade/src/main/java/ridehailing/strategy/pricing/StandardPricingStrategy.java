package ridehailing.strategy.pricing;

import ridehailing.pricing.PricingEngine;

/** Price from the tariff table of the zone, the same formula the facade already uses. */
public class StandardPricingStrategy implements PricingStrategy {

    private final PricingEngine standardPricingEngine = new PricingEngine();

    @Override
    public double calculatePrice(PriceInput priceInput) {
        return standardPricingEngine.calculatePrice(priceInput.getDistanceInKilometers(), priceInput.getCityZoneName());
    }
}
