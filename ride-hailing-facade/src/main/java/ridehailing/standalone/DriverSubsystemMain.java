package ridehailing.standalone;

import ridehailing.driver.CityMapPoint;
import ridehailing.driver.Driver;
import ridehailing.driver.DriverMatcher;
import ridehailing.driver.DriverRepository;
import ridehailing.driver.GpsRouter;

import java.nio.file.Path;

/** Subsystem A used on its own, without the facade. Run it from the IDE. */
public class DriverSubsystemMain {

    public static void main(String[] args) {
        DriverRepository driverRepository = new DriverRepository(Path.of("data-standalone"));
        driverRepository.saveDriver(new Driver("DRV-1", "Ann", new CityMapPoint(1, 1)));
        driverRepository.saveDriver(new Driver("DRV-2", "Bob", new CityMapPoint(5, 5)));

        CityMapPoint pickupPoint = new CityMapPoint(4, 4);
        Driver nearestFreeDriver = new DriverMatcher().findNearestFreeDriver(driverRepository.findAllDrivers(), pickupPoint);
        System.out.println("Nearest free driver: " + nearestFreeDriver.getDriverName());

        nearestFreeDriver.reserveForRide();
        driverRepository.saveDriver(nearestFreeDriver);
        System.out.println("Saved status: " + driverRepository.findDriverById(nearestFreeDriver.getDriverId()).getCurrentStatus());

        GpsRouter gpsRouter = new GpsRouter();
        double distanceInKilometers = gpsRouter.calculateDistanceInKilometersBetween(pickupPoint, new CityMapPoint(7, 8));
        System.out.println("Route: " + distanceInKilometers + " km, "
                + gpsRouter.calculateEstimatedTravelTimeInMinutes(distanceInKilometers) + " minutes");
    }
}
