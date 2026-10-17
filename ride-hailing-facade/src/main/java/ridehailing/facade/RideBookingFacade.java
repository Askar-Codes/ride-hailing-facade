package ridehailing.facade;

import ridehailing.driver.CityMapPoint;
import ridehailing.driver.Driver;
import ridehailing.driver.DriverMatcher;
import ridehailing.driver.DriverRepository;
import ridehailing.driver.GpsRouter;
import ridehailing.payment.Payment;
import ridehailing.payment.PaymentDeclinedException;
import ridehailing.payment.PaymentProcessor;
import ridehailing.payment.PaymentRepository;
import ridehailing.payment.PaymentStatus;
import ridehailing.payment.RatingService;
import ridehailing.pricing.PricingEngine;
import ridehailing.pricing.Receipt;
import ridehailing.pricing.ReceiptGenerator;
import ridehailing.pricing.ReceiptRepository;

import java.nio.file.Path;

/**
 * Facade for the everyday client workflow: book a ride and rate it.
 * One call drives the chain A (reserve driver) -> B (price and receipt) -> C (charge card).
 * If B or C fails, the facade rolls back what was already saved.
 * Creating a new facade over the same folder is the same as restarting the application.
 */
public class RideBookingFacade {

    private final DriverRepository driverRepository;
    private final DriverMatcher driverMatcher;
    private final GpsRouter gpsRouter;
    private final PricingEngine pricingEngine;
    private final ReceiptGenerator receiptGenerator;
    private final ReceiptRepository receiptRepository;
    private final PaymentProcessor paymentProcessor;
    private final PaymentRepository paymentRepository;
    private final RatingService ratingService;
    private final RideOrderRepository rideOrderRepository;
    private final RideCompensator rideCompensator;

    public RideBookingFacade(Path folderWhereDataIsStored) {
        driverRepository = new DriverRepository(folderWhereDataIsStored);
        driverMatcher = new DriverMatcher();
        gpsRouter = new GpsRouter();
        pricingEngine = new PricingEngine();
        receiptGenerator = new ReceiptGenerator();
        receiptRepository = new ReceiptRepository(folderWhereDataIsStored);
        paymentProcessor = new PaymentProcessor();
        paymentRepository = new PaymentRepository(folderWhereDataIsStored);
        ratingService = new RatingService(paymentRepository);
        rideOrderRepository = new RideOrderRepository(folderWhereDataIsStored);
        rideCompensator = new RideCompensator(
                driverRepository, receiptRepository, paymentRepository, rideOrderRepository);
    }

    public RideOrder bookRide(RideRequest rideRequest) {
        checkRideRequestIsValid(rideRequest);

        // Step A: reserve the nearest free driver (if this fails, nothing is saved yet)
        RideOrder rideOrder = runStepAReserveNearestFreeDriver(rideRequest);

        // Step B: price the ride and issue the receipt (if this fails, undo A alone)
        runStepBAndUndoStepAIfItFails(rideOrder, rideRequest);

        // Step C: charge the card (if this fails, undo B, then A)
        try {
            runStepCChargeCardAndFinishRide(rideOrder, rideRequest.getCardNumber());
        } catch (RuntimeException failureOfStepC) {   // a declined card, but also any unexpected error
            System.out.println("[C] FAILED: " + failureOfStepC.getMessage());
            rideCompensator.compensateRide(rideOrder.getRideOrderId());
            throw failureOfStepC;
        }

        return rideOrder;
    }

    /**
     * For the demo and for tests: runs step A only and then "the application crashes".
     * The ride stays PENDING_B on disk and the driver stays RESERVED.
     */
    public RideOrder simulateCrashAfterStepA(RideRequest rideRequest) {
        checkRideRequestIsValid(rideRequest);
        RideOrder rideOrder = runStepAReserveNearestFreeDriver(rideRequest);
        System.out.println("!!! SIMULATED CRASH after step A. Ride " + rideOrder.getRideOrderId()
                + " is left as " + rideOrder.getStatus() + " on disk");
        return rideOrder;
    }

    /**
     * For the demo and for tests: runs steps A and B and then "the application crashes".
     * The ride stays PENDING_C on disk, the receipt stays ISSUED and the driver stays RESERVED.
     */
    public RideOrder simulateCrashAfterStepB(RideRequest rideRequest) {
        checkRideRequestIsValid(rideRequest);
        RideOrder rideOrder = runStepAReserveNearestFreeDriver(rideRequest);
        runStepBAndUndoStepAIfItFails(rideOrder, rideRequest);
        System.out.println("!!! SIMULATED CRASH after step B. Ride " + rideOrder.getRideOrderId()
                + " is left as " + rideOrder.getStatus() + " on disk");
        return rideOrder;
    }

    public void rateCompletedRide(String rideOrderId, int numberOfStars) {
        RideOrder rideOrder = rideOrderRepository.findRideOrderById(rideOrderId);
        if (rideOrder.getStatus() != RideOrderStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Only a completed ride can be rated, this one is " + rideOrder.getStatus());
        }
        ratingService.rateDriver(rideOrder.getPaymentId(), numberOfStars);
        System.out.println("Driver rated with " + numberOfStars + " stars for ride " + rideOrderId);
    }

    private void checkRideRequestIsValid(RideRequest rideRequest) {
        if (rideRequest.getRiderName() == null || rideRequest.getRiderName().isBlank()) {
            throw new InvalidRideRequestException("Rider name must not be empty");
        }
        if (rideRequest.getCityZoneName() == null || rideRequest.getCityZoneName().isBlank()) {
            throw new InvalidRideRequestException("Zone must not be empty");
        }
        if (rideRequest.getCardNumber() == null || rideRequest.getCardNumber().isBlank()) {
            throw new InvalidRideRequestException("Card number must not be empty");
        }
        boolean pickupAndDropoffAreTheSamePlace =
                rideRequest.getPickupXInKilometers() == rideRequest.getDropoffXInKilometers()
                && rideRequest.getPickupYInKilometers() == rideRequest.getDropoffYInKilometers();
        if (pickupAndDropoffAreTheSamePlace) {
            throw new InvalidRideRequestException("Pickup and drop-off must be different places");
        }
    }

    private RideOrder runStepAReserveNearestFreeDriver(RideRequest rideRequest) {
        CityMapPoint pickupPoint = new CityMapPoint(
                rideRequest.getPickupXInKilometers(), rideRequest.getPickupYInKilometers());
        Driver nearestFreeDriver = driverMatcher.findNearestFreeDriver(driverRepository.findAllDrivers(), pickupPoint);

        // Save the ride first, then reserve the driver: after a crash the ride is always on disk.
        RideOrder rideOrder = new RideOrder(rideRequest.getRiderName(), nearestFreeDriver.getDriverId());
        rideOrderRepository.saveRideOrder(rideOrder);

        nearestFreeDriver.reserveForRide();
        driverRepository.saveDriver(nearestFreeDriver);
        System.out.println("[A] Driver " + nearestFreeDriver.getDriverName() + " reserved. Ride "
                + rideOrder.getRideOrderId() + " is " + rideOrder.getStatus());
        return rideOrder;
    }

    private void runStepBAndUndoStepAIfItFails(RideOrder rideOrder, RideRequest rideRequest) {
        double distanceInKilometers = gpsRouter.calculateDistanceInKilometersBetween(
                new CityMapPoint(rideRequest.getPickupXInKilometers(), rideRequest.getPickupYInKilometers()),
                new CityMapPoint(rideRequest.getDropoffXInKilometers(), rideRequest.getDropoffYInKilometers()));
        try {
            runStepBPriceRideAndIssueReceipt(rideOrder, distanceInKilometers, rideRequest.getCityZoneName());
        } catch (RuntimeException failureOfStepB) {   // an unknown zone or a too high price, but also any unexpected error
            System.out.println("[B] FAILED: " + failureOfStepB.getMessage());
            rideCompensator.compensateRide(rideOrder.getRideOrderId());
            throw failureOfStepB;
        }
    }

    private void runStepBPriceRideAndIssueReceipt(RideOrder rideOrder, double distanceInKilometers, String cityZoneName) {
        double price = pricingEngine.calculatePrice(distanceInKilometers, cityZoneName);
        Receipt receipt = receiptGenerator.generateReceipt(
                rideOrder.getRideOrderId(), rideOrder.getRiderName(), price);
        receiptRepository.saveReceipt(receipt);

        rideOrder.attachReceiptAndMoveToPendingC(receipt.getReceiptId());
        rideOrderRepository.saveRideOrder(rideOrder);
        System.out.println("[B] Receipt " + receipt.getReceiptId() + " issued for " + price
                + ". Ride is " + rideOrder.getStatus());
    }

    private void runStepCChargeCardAndFinishRide(RideOrder rideOrder, String cardNumber) {
        Receipt receipt = receiptRepository.findReceiptById(rideOrder.getReceiptId());
        Payment payment = paymentProcessor.chargeCard(
                rideOrder.getRideOrderId(), cardNumber, receipt.getAmountToPay());
        paymentRepository.savePayment(payment);

        rideOrder.attachPayment(payment.getPaymentId());
        rideOrderRepository.saveRideOrder(rideOrder);

        if (payment.getStatus() == PaymentStatus.DECLINED) {
            throw new PaymentDeclinedException(
                    "The bank declined the card ending " + payment.getCardLastFourDigits());
        }

        receipt.markAsPaid();
        receiptRepository.saveReceipt(receipt);

        Driver driver = driverRepository.findDriverById(rideOrder.getDriverId());
        driver.startDrivingRide();
        driverRepository.saveDriver(driver);

        rideOrder.markAsCompleted();
        rideOrderRepository.saveRideOrder(rideOrder);
        System.out.println("[C] Payment " + payment.getPaymentId() + " charged. Ride "
                + rideOrder.getRideOrderId() + " is " + rideOrder.getStatus()
                + ", driver " + driver.getDriverName() + " is on the way");
    }
}
