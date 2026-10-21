package ridehailing.strategy.pricing;

import ridehailing.pricing.PricingEngine;

/** The "before" version: all tariffs live in one class and a switch picks the formula. */
public class SwitchBasedPriceCalculator {

    private static final double PREMIUM_COMFORT_FEE = 800.0;
    private static final double PREMIUM_PRICE_PER_KILOMETER = 220.0;

    private final PricingEngine standardPricingEngine = new PricingEngine();
    private final DemandSource demandSource;
    private PricingMode pricingMode;

    public SwitchBasedPriceCalculator(PricingMode pricingMode, DemandSource demandSource) {
        this.pricingMode = pricingMode;
        this.demandSource = demandSource;
    }

    public void setPricingMode(PricingMode pricingMode) {
        this.pricingMode = pricingMode;
    }

    public double calculatePrice(PriceInput priceInput) {
        switch (pricingMode) {
            case STANDARD:
                return standardPricingEngine.calculatePrice(
                        priceInput.getDistanceInKilometers(), priceInput.getCityZoneName());
            case SURGE:
                double standardPrice = standardPricingEngine.calculatePrice(
                        priceInput.getDistanceInKilometers(), priceInput.getCityZoneName());
                return Math.round(standardPrice * demandSource.getCurrentDemandMultiplier() * 100) / 100.0;
            case PREMIUM:
                return PREMIUM_COMFORT_FEE + priceInput.getDistanceInKilometers() * PREMIUM_PRICE_PER_KILOMETER;
            default:
                throw new IllegalStateException("Unknown pricing mode " + pricingMode);
        }
    }
}
