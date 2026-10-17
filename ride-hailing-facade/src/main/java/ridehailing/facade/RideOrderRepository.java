package ridehailing.facade;

import ridehailing.storage.CsvFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Stores the progress of every ride in rides.csv, so the chain survives a restart. */
public class RideOrderRepository {

    private final CsvFile ridesCsvFile;

    public RideOrderRepository(Path folderWhereDataIsStored) {
        this.ridesCsvFile = new CsvFile(folderWhereDataIsStored.resolve("rides.csv"));
    }

    public void saveRideOrder(RideOrder rideOrder) {
        ridesCsvFile.saveOrReplaceRowById(new String[]{
                rideOrder.getRideOrderId(),
                rideOrder.getRiderName(),
                rideOrder.getDriverId(),
                rideOrder.getReceiptId(),
                rideOrder.getPaymentId(),
                rideOrder.getStatus().name()
        });
    }

    public List<RideOrder> findAllRideOrders() {
        List<RideOrder> allRideOrders = new ArrayList<>();
        for (String[] row : ridesCsvFile.readAllRowsFromFile()) {
            allRideOrders.add(new RideOrder(row[0], row[1], row[2], row[3], row[4],
                    RideOrderStatus.valueOf(row[5])));
        }
        return allRideOrders;
    }

    public RideOrder findRideOrderById(String rideOrderId) {
        for (RideOrder rideOrder : findAllRideOrders()) {
            if (rideOrder.getRideOrderId().equals(rideOrderId)) {
                return rideOrder;
            }
        }
        throw new IllegalArgumentException("Ride not found: " + rideOrderId);
    }
}
