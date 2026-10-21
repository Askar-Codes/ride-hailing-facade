package ridehailing.strategy.pricing;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulatedDemandSourceTest {

    private double demandAtHour(int hour) {
        Clock clock = Clock.fixed(Instant.parse(String.format("2026-01-01T%02d:30:00Z", hour)), ZoneOffset.UTC);
        return new SimulatedDemandSource(clock).getCurrentDemandMultiplier();
    }

    @Test
    void demandIsDoubleInTheMorningRush() {
        assertEquals(2.0, demandAtHour(9), 0.001);
    }

    @Test
    void demandIsDoubleInTheEveningRush() {
        assertEquals(2.0, demandAtHour(18), 0.001);
    }

    @Test
    void demandIsNormalAtMidday() {
        assertEquals(1.0, demandAtHour(13), 0.001);
    }
}
