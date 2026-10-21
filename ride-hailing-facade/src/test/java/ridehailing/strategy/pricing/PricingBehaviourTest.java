package ridehailing.strategy.pricing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ridehailing.pricing.PricingException;
import ridehailing.strategy.TaxiClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** These tests must pass both before and after the refactoring to Strategy. */
class PricingBehaviourTest {

    private final PriceInput fiveKilometersInCenter = new PriceInput(5, "CENTER", TaxiClass.REGULAR);

    @Test
    void standardPriceIsBaseFarePlusZoneRatePerKilometer() {
        assertEquals(1000.0, PricingTestHelper.calculatePrice("standard", fiveKilometersInCenter, 1.0), 0.001);
    }

    @Test
    void standardPriceFailsForUnknownZone() {
        PriceInput unknownZone = new PriceInput(5, "MARS", TaxiClass.REGULAR);
        assertThrows(PricingException.class, () -> PricingTestHelper.calculatePrice("standard", unknownZone, 1.0));
    }

    @Test
    void surgePriceIsStandardPriceTimesCurrentDemand() {
        assertEquals(2000.0, PricingTestHelper.calculatePrice("surge", fiveKilometersInCenter, 2.0), 0.001);
    }

    @Test
    void premiumPriceIsComfortFeePlusFlatRatePerKilometer() {
        assertEquals(1900.0, PricingTestHelper.calculatePrice("premium", fiveKilometersInCenter, 1.0), 0.001);
    }

    @ParameterizedTest
    @ValueSource(strings = {"standard", "surge", "premium"})
    void priceIsNeverNegative(String algorithmName) {
        double[] distancesInKilometers = {0.1, 1, 5, 12};
        for (double distance : distancesInKilometers) {
            PriceInput priceInput = new PriceInput(distance, "SUBURB", TaxiClass.REGULAR);
            assertTrue(PricingTestHelper.calculatePrice(algorithmName, priceInput, 1.5) >= 0);
        }
    }
}
