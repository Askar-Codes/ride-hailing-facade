package ridehailing.strategy.pricing;

/** Test double: always returns the demand it was given. */
public class FakeDemandSource implements DemandSource {

    private final double demandMultiplier;

    public FakeDemandSource(double demandMultiplier) {
        this.demandMultiplier = demandMultiplier;
    }

    @Override
    public double getCurrentDemandMultiplier() {
        return demandMultiplier;
    }
}
