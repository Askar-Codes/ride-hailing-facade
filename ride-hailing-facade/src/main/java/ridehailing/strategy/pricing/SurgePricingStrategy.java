package ridehailing.strategy.pricing;

/** Standard price multiplied by the demand reported by the outside source. */
public class SurgePricingStrategy implements PricingStrategy {

    private final StandardPricingStrategy standardPricingStrategy = new StandardPricingStrategy();
    private final DemandSource demandSource;

    public SurgePricingStrategy(DemandSource demandSource) {
        this.demandSource = demandSource;
    }

    @Override
    public double calculatePrice(PriceInput priceInput) {
        double standardPrice = standardPricingStrategy.calculatePrice(priceInput);
        return Math.round(standardPrice * demandSource.getCurrentDemandMultiplier() * 100) / 100.0;
    }
}
