package ridehailing.strategy.pricing;

/** Our own window to the outside world: how busy the city is right now. */
public interface DemandSource {

    /** 1.0 means normal demand, 2.0 means twice as many requests as usual. */
    double getCurrentDemandMultiplier();
}
