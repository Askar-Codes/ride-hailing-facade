package ridehailing.strategy.pricing;

/** Holds the current pricing strategy and asks it for the price. */
public class PricingContext {

    private PricingStrategy pricingStrategy;

    public PricingContext(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    public void setStrategy(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    public double calculatePrice(PriceInput priceInput) {
        return pricingStrategy.calculatePrice(priceInput);
    }
}
