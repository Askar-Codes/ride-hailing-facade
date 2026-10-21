package ridehailing.strategy.matching;

import ridehailing.driver.Driver;
import ridehailing.driver.NoFreeDriverException;

import java.util.List;
import java.util.stream.Collectors;

/** Shared helper of the matching strategies: only free drivers may get a ride. */
final class FreeDriversFilter {

    private FreeDriversFilter() {
    }

    static List<Driver> keepOnlyFreeDriversOrThrow(List<Driver> allDrivers) {
        List<Driver> freeDrivers = allDrivers.stream()
                .filter(Driver::isFreeForNewRide)
                .collect(Collectors.toList());
        if (freeDrivers.isEmpty()) {
            throw new NoFreeDriverException("No free driver is available right now");
        }
        return freeDrivers;
    }
}
