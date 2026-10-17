package ridehailing.facade;

import ridehailing.driver.CityMapPoint;
import ridehailing.driver.Driver;
import ridehailing.driver.DriverRepository;
import ridehailing.payment.Payment;
import ridehailing.payment.PaymentRepository;
import ridehailing.pricing.Receipt;
import ridehailing.pricing.ReceiptRepository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Narrow facade for administration: look at what is saved, finish a trip,
 * cancel a ride by hand and recover rides that a crash left in the middle of the chain.
 */
public class RideAdminFacade {

    private final DriverRepository driverRepository;
    private final ReceiptRepository receiptRepository;
    private final PaymentRepository paymentRepository;
    private final RideOrderRepository rideOrderRepository;
    private final RideCompensator rideCompensator;

    public RideAdminFacade(Path folderWhereDataIsStored) {
        driverRepository = new DriverRepository(folderWhereDataIsStored);
        receiptRepository = new ReceiptRepository(folderWhereDataIsStored);
        paymentRepository = new PaymentRepository(folderWhereDataIsStored);
        rideOrderRepository = new RideOrderRepository(folderWhereDataIsStored);
        rideCompensator = new RideCompensator(
                driverRepository, receiptRepository, paymentRepository, rideOrderRepository);
    }

    public void registerNewDriver(String driverName, double xInKilometers, double yInKilometers) {
        String driverId = "DRV-" + UUID.randomUUID().toString().substring(0, 8);
        driverRepository.saveDriver(new Driver(driverId, driverName, new CityMapPoint(xInKilometers, yInKilometers)));
    }

    public List<String> describeAllDrivers() {
        List<String> descriptions = new ArrayList<>();
        for (Driver driver : driverRepository.findAllDrivers()) {
            descriptions.add(driver.getDriverName()
                    + " at (" + driver.getCurrentLocation().getXInKilometers()
                    + ", " + driver.getCurrentLocation().getYInKilometers()
                    + ") [" + driver.getCurrentStatus() + "]");
        }
        return descriptions;
    }

    public List<String> describeAllRides() {
        List<String> descriptions = new ArrayList<>();
        for (RideOrder rideOrder : rideOrderRepository.findAllRideOrders()) {
            descriptions.add(describeRide(rideOrder));
        }
        return descriptions;
    }

    /** Manual compensation: undoes whatever the saved ride says was done. */
    public void cancelRideManually(String rideOrderId) {
        rideCompensator.compensateRide(rideOrderId);
    }

    /** The trip is over: the driver becomes FREE again. The ride stays COMPLETED. */
    public void finishTripAndMakeDriverFree(String rideOrderId) {
        RideOrder rideOrder = rideOrderRepository.findRideOrderById(rideOrderId);
        if (rideOrder.getStatus() != RideOrderStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Only a completed ride can be finished, this one is " + rideOrder.getStatus());
        }
        Driver driver = driverRepository.findDriverById(rideOrder.getDriverId());
        driver.becomeFreeAgain();
        driverRepository.saveDriver(driver);
        System.out.println("Trip " + rideOrderId + " finished, driver " + driver.getDriverName() + " is FREE");
    }

    /**
     * Run after a restart: a ride that is still PENDING_B or PENDING_C on disk was interrupted
     * in the middle of the chain, so it is compensated from what is saved.
     * Returns how many rides were recovered.
     */
    public int recoverUnfinishedRides() {
        int numberOfRecoveredRides = 0;
        for (RideOrder rideOrder : rideOrderRepository.findAllRideOrders()) {
            boolean rideWasInterrupted = rideOrder.getStatus() == RideOrderStatus.PENDING_B
                    || rideOrder.getStatus() == RideOrderStatus.PENDING_C;
            if (rideWasInterrupted) {
                System.out.println("Recovery: ride " + rideOrder.getRideOrderId()
                        + " was left as " + rideOrder.getStatus() + " on disk");
                rideCompensator.compensateRide(rideOrder.getRideOrderId());
                numberOfRecoveredRides++;
            }
        }
        return numberOfRecoveredRides;
    }

    private String describeRide(RideOrder rideOrder) {
        Driver driver = driverRepository.findDriverById(rideOrder.getDriverId());

        String receiptDescription = "no receipt";
        if (rideOrder.hasReceiptAttached()) {
            Receipt receipt = receiptRepository.findReceiptById(rideOrder.getReceiptId());
            receiptDescription = "receipt " + receipt.getAmountToPay() + " " + receipt.getStatus();
        }

        String paymentDescription = "no payment";
        if (rideOrder.hasPaymentAttached()) {
            Payment payment = paymentRepository.findPaymentById(rideOrder.getPaymentId());
            paymentDescription = "payment " + payment.getStatus();
            if (payment.getDriverRatingInStars() > 0) {
                paymentDescription += ", rated " + payment.getDriverRatingInStars() + " stars";
            }
        }

        return rideOrder.getRideOrderId() + " [" + rideOrder.getStatus() + "] rider " + rideOrder.getRiderName()
                + " | driver " + driver.getDriverName()
                + " | " + receiptDescription + " | " + paymentDescription;
    }
}
