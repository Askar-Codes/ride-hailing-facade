package ridehailing.strategy.pricing;

import org.junit.jupiter.api.Test;
import ridehailing.strategy.TaxiClass;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingStrategySelectorTest {

    private PricingStrategy chooseFor(TaxiClass taxiClass, double demandMultiplier) {
        PricingStrategySelector selector = new PricingStrategySelector(new FakeDemandSource(demandMultiplier));
        return selector.chooseStrategyFor(new PriceInput(5, "CENTER", taxiClass));
    }

    @Test
    void premiumTaxiGetsPremiumPricing() {
        assertTrue(chooseFor(TaxiClass.PREMIUM, 1.0) instanceof PremiumPricingStrategy);
    }

    @Test
    void premiumTaxiGetsPremiumPricingEvenWhenDemandIsHigh() {
        assertTrue(chooseFor(TaxiClass.PREMIUM, 3.0) instanceof PremiumPricingStrategy);
    }

    @Test
    void regularTaxiGetsSurgePricingWhenDemandIsHigh() {
        assertTrue(chooseFor(TaxiClass.REGULAR, 1.5) instanceof SurgePricingStrategy);
    }

    @Test
    void regularTaxiGetsStandardPricingWhenDemandIsNormal() {
        assertTrue(chooseFor(TaxiClass.REGULAR, 1.0) instanceof StandardPricingStrategy);
    }
}
