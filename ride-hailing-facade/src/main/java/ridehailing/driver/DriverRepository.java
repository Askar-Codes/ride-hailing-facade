package ridehailing.driver;

import ridehailing.storage.CsvFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Persistence gateway of subsystem A: talks to drivers.csv directly. */
public class DriverRepository {

    private final CsvFile driversCsvFile;

    public DriverRepository(Path folderWhereDataIsStored) {
        this.driversCsvFile = new CsvFile(folderWhereDataIsStored.resolve("drivers.csv"));
    }

    public void saveDriver(Driver driver) {
        driversCsvFile.saveOrReplaceRowById(new String[]{
                driver.getDriverId(),
                driver.getDriverName(),
                String.valueOf(driver.getCurrentLocation().getXInKilometers()),
                String.valueOf(driver.getCurrentLocation().getYInKilometers()),
                driver.getCurrentStatus().name()
        });
    }

    public List<Driver> findAllDrivers() {
        List<Driver> allDrivers = new ArrayList<>();
        for (String[] row : driversCsvFile.readAllRowsFromFile()) {
            CityMapPoint location = new CityMapPoint(Double.parseDouble(row[2]), Double.parseDouble(row[3]));
            allDrivers.add(new Driver(row[0], row[1], location, DriverStatus.valueOf(row[4])));
        }
        return allDrivers;
    }

    public Driver findDriverById(String driverId) {
        for (Driver driver : findAllDrivers()) {
            if (driver.getDriverId().equals(driverId)) {
                return driver;
            }
        }
        throw new IllegalArgumentException("Driver not found: " + driverId);
    }
}
