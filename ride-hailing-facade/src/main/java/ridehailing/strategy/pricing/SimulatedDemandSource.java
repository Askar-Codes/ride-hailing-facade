package ridehailing.strategy.pricing;

import java.time.Clock;
import java.time.LocalTime;

/** Simulated outside source: the city is twice as busy during the morning and evening rush hours. */
public class SimulatedDemandSource implements DemandSource {

    private final Clock clock;

    public SimulatedDemandSource(Clock clock) {
        this.clock = clock;
    }

    @Override
    public double getCurrentDemandMultiplier() {
        int hour = LocalTime.now(clock).getHour();
        boolean isRushHour = (hour >= 8 && hour < 10) || (hour >= 17 && hour < 20);
        return isRushHour ? 2.0 : 1.0;
    }
}
