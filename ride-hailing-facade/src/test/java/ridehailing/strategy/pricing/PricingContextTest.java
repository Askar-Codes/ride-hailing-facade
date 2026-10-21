package ridehailing.strategy.pricing;

import org.junit.jupiter.api.Test;
import ridehailing.strategy.TaxiClass;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingContextTest {

    private final PriceInput anyRide = new PriceInput(5, "CENTER", TaxiClass.REGULAR);

    @Test
    void contextReturnsWhatTheStrategyCalculates() {
        PricingContext pricingContext = new PricingContext(priceInput -> 42.0);
        assertEquals(42.0, pricingContext.calculatePrice(anyRide), 0.001);
    }

    @Test
    void contextUsesTheNewStrategyAfterSetStrategy() {
        PricingContext pricingContext = new PricingContext(priceInput -> 1.0);
        pricingContext.setStrategy(priceInput -> 2.0);
        assertEquals(2.0, pricingContext.calculatePrice(anyRide), 0.001);
    }
}
