package ridehailing.strategy.pricing;

/** One way to calculate the price of a ride. */
public interface PricingStrategy {

    double calculatePrice(PriceInput priceInput);
}
