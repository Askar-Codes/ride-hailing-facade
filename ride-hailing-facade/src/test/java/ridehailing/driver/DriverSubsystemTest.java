package ridehailing.driver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Subsystem A tested ALONE: no facade, no other subsystem. */
class DriverSubsystemTest {

    @TempDir
    Path folderWhereDataIsStored;

    private Driver freeDriverNamed(String driverName, double xInKilometers, double yInKilometers) {
        return new Driver("DRV-" + driverName, driverName, new CityMapPoint(xInKilometers, yInKilometers));
    }

    // ---------- Driver status ----------

    @Test
    void driverGoesFromFreeToReservedToBusyAndBackToFree() {
        Driver driver = freeDriverNamed("Ann", 0, 0);

        driver.reserveForRide();
        assertEquals(DriverStatus.RESERVED, driver.getCurrentStatus());
        driver.startDrivingRide();
        assertEquals(DriverStatus.BUSY, driver.getCurrentStatus());
        driver.becomeFreeAgain();
        assertEquals(DriverStatus.FREE, driver.getCurrentStatus());
    }

    @Test
    void reservedDriverCanBecomeFreeWithoutDriving() {
        Driver driver = freeDriverNamed("Ann", 0, 0);
        driver.reserveForRide();

        driver.becomeFreeAgain();

        assertTrue(driver.isFreeForNewRide());
    }

    @Test
    void illegalStatusChangesAreRejectedByTheDriverItself() {
        Driver freeDriver = freeDriverNamed("Ann", 0, 0);
        assertThrows(IllegalStateException.class, freeDriver::startDrivingRide);
        assertThrows(IllegalStateException.class, freeDriver::becomeFreeAgain);

        Driver busyDriver = freeDriverNamed("Bob", 0, 0);
        busyDriver.reserveForRide();
        busyDriver.startDrivingRide();
        assertThrows(IllegalStateException.class, busyDriver::reserveForRide);
    }

    // ---------- DriverMatcher ----------

    @Test
    void matcherPicksTheNearestFreeDriver() {
        List<Driver> allDrivers = Arrays.asList(freeDriverNamed("Far", 10, 10), freeDriverNamed("Near", 1, 1));

        Driver chosenDriver = new DriverMatcher().findNearestFreeDriver(allDrivers, new CityMapPoint(0, 0));

        assertEquals("Near", chosenDriver.getDriverName());
    }

    @Test
    void matcherSkipsDriversThatAreNotFree() {
        Driver nearButReservedDriver = freeDriverNamed("Near", 1, 1);
        nearButReservedDriver.reserveForRide();
        List<Driver> allDrivers = Arrays.asList(nearButReservedDriver, freeDriverNamed("Far", 10, 10));

        Driver chosenDriver = new DriverMatcher().findNearestFreeDriver(allDrivers, new CityMapPoint(0, 0));

        assertEquals("Far", chosenDriver.getDriverName());
    }

    @Test
    void matcherFailsWhenNobodyIsFree() {
        assertThrows(NoFreeDriverException.class,
                () -> new DriverMatcher().findNearestFreeDriver(new ArrayList<>(), new CityMapPoint(0, 0)));
    }

    // ---------- GpsRouter and CityMapPoint ----------

    @Test
    void distanceInA3And4TriangleIs5() {
        double distance = new CityMapPoint(0, 0).calculateDistanceInKilometersTo(new CityMapPoint(3, 4));

        assertEquals(5.0, distance, 0.0001);
    }

    @Test
    void routerCalculatesTheDistanceBetweenTwoPoints() {
        double distance = new GpsRouter().calculateDistanceInKilometersBetween(
                new CityMapPoint(0, 0), new CityMapPoint(3, 4));

        assertEquals(5.0, distance, 0.0001);
    }

    @Test
    void routeOf5KmTakes8MinutesAt40KmPerHour() {
        int minutes = new GpsRouter().calculateEstimatedTravelTimeInMinutes(5.0);

        assertEquals(8, minutes);   // 5 / 40 * 60 = 7.5, rounded up
    }

    // ---------- DriverRepository (real file) ----------

    @Test
    void savedDriverIsReadBackByAnotherRepositoryObject() {
        Driver driver = freeDriverNamed("Ann", 1.5, 2.5);
        driver.reserveForRide();
        new DriverRepository(folderWhereDataIsStored).saveDriver(driver);

        Driver driverReadBack = new DriverRepository(folderWhereDataIsStored).findDriverById("DRV-Ann");

        assertEquals("Ann", driverReadBack.getDriverName());
        assertEquals(DriverStatus.RESERVED, driverReadBack.getCurrentStatus());
        assertEquals(1.5, driverReadBack.getCurrentLocation().getXInKilometers(), 0.0001);
        assertEquals(2.5, driverReadBack.getCurrentLocation().getYInKilometers(), 0.0001);
    }

    @Test
    void savingADriverTwiceKeepsOnlyOneRow() {
        DriverRepository driverRepository = new DriverRepository(folderWhereDataIsStored);
        Driver driver = freeDriverNamed("Ann", 0, 0);
        driverRepository.saveDriver(driver);
        driver.reserveForRide();
        driverRepository.saveDriver(driver);

        assertEquals(1, driverRepository.findAllDrivers().size());
    }

    @Test
    void emptyFolderMeansNoDrivers() {
        assertTrue(new DriverRepository(folderWhereDataIsStored).findAllDrivers().isEmpty());
    }

    @Test
    void unknownDriverIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new DriverRepository(folderWhereDataIsStored).findDriverById("DRV-none"));
    }

    @Test
    void aSeparatorTypedInANameCannotBreakTheFile() {
        DriverRepository driverRepository = new DriverRepository(folderWhereDataIsStored);
        driverRepository.saveDriver(new Driver("DRV-1", "Ann;Smith", new CityMapPoint(0, 0)));

        Driver driverReadBack = driverRepository.findDriverById("DRV-1");

        assertFalse(driverReadBack.getDriverName().contains(";"));
        assertEquals(DriverStatus.FREE, driverReadBack.getCurrentStatus());
    }
}
