package ridehailing.strategy.pricing;

import ridehailing.strategy.TaxiClass;

/** Picks the pricing strategy that fits the taxi class and the current demand. */
public class PricingStrategySelector {

    private static final double HIGH_DEMAND_MULTIPLIER = 1.5;

    private final DemandSource demandSource;
    private final PricingStrategy standardPricingStrategy = new StandardPricingStrategy();
    private final PricingStrategy surgePricingStrategy;
    private final PricingStrategy premiumPricingStrategy = new PremiumPricingStrategy();

    public PricingStrategySelector(DemandSource demandSource) {
        this.demandSource = demandSource;
        this.surgePricingStrategy = new SurgePricingStrategy(demandSource);
    }

    public PricingStrategy chooseStrategyFor(PriceInput priceInput) {
        if (priceInput.getTaxiClass() == TaxiClass.PREMIUM) {
            return premiumPricingStrategy;
        }
        if (demandSource.getCurrentDemandMultiplier() >= HIGH_DEMAND_MULTIPLIER) {
            return surgePricingStrategy;
        }
        return standardPricingStrategy;
    }
}
