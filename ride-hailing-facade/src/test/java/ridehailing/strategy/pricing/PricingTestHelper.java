package ridehailing.strategy.pricing;

/** Builds the pricing algorithm by its name, so the tests do not care how it is implemented. */
public class PricingTestHelper {

    public static double calculatePrice(String algorithmName, PriceInput priceInput, double demandMultiplier) {
        PricingMode pricingMode = PricingMode.valueOf(algorithmName.toUpperCase());
        return new SwitchBasedPriceCalculator(pricingMode, new FakeDemandSource(demandMultiplier))
                .calculatePrice(priceInput);
    }
}
