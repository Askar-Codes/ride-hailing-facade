package ridehailing.strategy.pricing;

import java.util.Map;

/** Builds the pricing algorithm by its name, so the tests do not care how it is implemented. */
public class PricingTestHelper {

    public static double calculatePrice(String algorithmName, PriceInput priceInput, double demandMultiplier) {
        Map<String, PricingStrategy> strategiesByName = Map.of(
                "standard", new StandardPricingStrategy(),
                "surge", new SurgePricingStrategy(new FakeDemandSource(demandMultiplier)),
                "premium", new PremiumPricingStrategy());
        return new PricingContext(strategiesByName.get(algorithmName)).calculatePrice(priceInput);
    }
}
