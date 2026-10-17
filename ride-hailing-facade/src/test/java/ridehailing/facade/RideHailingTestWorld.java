package ridehailing.facade;

import ridehailing.driver.CityMapPoint;
import ridehailing.driver.Driver;
import ridehailing.driver.DriverRepository;
import ridehailing.driver.DriverStatus;
import ridehailing.payment.Payment;
import ridehailing.payment.PaymentProcessor;
import ridehailing.payment.PaymentRepository;
import ridehailing.payment.PaymentStatus;
import ridehailing.pricing.Receipt;
import ridehailing.pricing.ReceiptGenerator;
import ridehailing.pricing.ReceiptRepository;
import ridehailing.pricing.ReceiptStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The small testing language of this project, so a test reads like a sentence:
 *   world.givenFreeDriver("Ann", 1, 1);
 *   world.assertDriverIs("Ann", DriverStatus.FREE);
 * Every test gets its own empty data folder, so tests never depend on each other.
 * The "...OnDisk" repositories only LOOK at the files; the facades never use them.
 */
class RideHailingTestWorld {

    static final String CARD_THE_BANK_ACCEPTS = "4111111111111111";
    static final String CARD_THE_BANK_DECLINES = "4000000000000000";

    private final Path folderWhereDataIsStored;

    RideBookingFacade bookingFacade;
    RideAdminFacade adminFacade;

    private DriverRepository driversOnDisk;
    private ReceiptRepository receiptsOnDisk;
    private PaymentRepository paymentsOnDisk;
    private RideOrderRepository rideOrdersOnDisk;

    RideHailingTestWorld(Path folderWhereDataIsStored) {
        this.folderWhereDataIsStored = folderWhereDataIsStored;
        startApplication();
    }

    /** Throws away every object and opens the same files again, like a real restart. */
    void restartApplication() {
        startApplication();
    }

    private void startApplication() {
        bookingFacade = new RideBookingFacade(folderWhereDataIsStored);
        adminFacade = new RideAdminFacade(folderWhereDataIsStored);
        driversOnDisk = new DriverRepository(folderWhereDataIsStored);
        receiptsOnDisk = new ReceiptRepository(folderWhereDataIsStored);
        paymentsOnDisk = new PaymentRepository(folderWhereDataIsStored);
        rideOrdersOnDisk = new RideOrderRepository(folderWhereDataIsStored);
    }

    // ---------- build: drivers ----------

    void givenFreeDriver(String driverName, double xInKilometers, double yInKilometers) {
        driversOnDisk.saveDriver(new Driver("DRV-" + driverName, driverName,
                new CityMapPoint(xInKilometers, yInKilometers)));
    }

    // ---------- build: a broken disk (a folder where the file should be, so every read and write of it fails) ----------

    void givenTheStorageOfReceiptsIsBroken() {
        makeFolderInsteadOfFile("receipts.csv");
    }

    void givenTheStorageOfPaymentsIsBroken() {
        makeFolderInsteadOfFile("payments.csv");
    }

    private void makeFolderInsteadOfFile(String fileName) {
        try {
            Files.createDirectories(folderWhereDataIsStored.resolve(fileName));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------- build: ride requests (a 5 km ride from (0, 0) to (3, 4); in CENTER it costs 1000) ----------

    RideRequest successfulRideRequest() {
        return new RideRequest("Aibek", 0, 0, 3, 4, "CENTER", CARD_THE_BANK_ACCEPTS);
    }

    /** Step B fails: there is no tariff for this zone. */
    RideRequest rideRequestToUnknownZone() {
        return new RideRequest("Aibek", 0, 0, 3, 4, "MARS", CARD_THE_BANK_ACCEPTS);
    }

    /** Step C fails: the bank declines this card. */
    RideRequest rideRequestWithDeclinedCard() {
        return new RideRequest("Aibek", 0, 0, 3, 4, "CENTER", CARD_THE_BANK_DECLINES);
    }

    /** Step B fails: about 141 km in CENTER is above the allowed maximum price. */
    RideRequest rideRequestTooLongToPrice() {
        return new RideRequest("Aibek", 0, 0, 100, 100, "CENTER", CARD_THE_BANK_ACCEPTS);
    }

    // ---------- build: a ride that a crash left in the middle of the chain (written straight to the files) ----------

    void givenRideInterruptedAfterStepA(String driverName) {
        saveRideWhereOnlyStepAIsDone(driverName);
    }

    void givenRideInterruptedAfterStepB(String driverName) {
        saveRideWhereStepsAAndBAreDone(driverName);
    }

    /** The bank charged the card, but the application died before the ride became COMPLETED. */
    void givenRideInterruptedAfterTheCardWasCharged(String driverName) {
        RideOrder rideOrder = saveRideWhereStepsAAndBAreDone(driverName);
        Payment chargedPayment = new PaymentProcessor().chargeCard(
                rideOrder.getRideOrderId(), CARD_THE_BANK_ACCEPTS, 1000.0);
        paymentsOnDisk.savePayment(chargedPayment);
        rideOrder.attachPayment(chargedPayment.getPaymentId());
        rideOrdersOnDisk.saveRideOrder(rideOrder);
    }

    private RideOrder saveRideWhereOnlyStepAIsDone(String driverName) {
        Driver reservedDriver = new Driver("DRV-" + driverName, driverName, new CityMapPoint(1, 1));
        reservedDriver.reserveForRide();
        driversOnDisk.saveDriver(reservedDriver);

        RideOrder rideOrder = new RideOrder("Aibek", reservedDriver.getDriverId());
        rideOrdersOnDisk.saveRideOrder(rideOrder);
        return rideOrder;
    }

    private RideOrder saveRideWhereStepsAAndBAreDone(String driverName) {
        RideOrder rideOrder = saveRideWhereOnlyStepAIsDone(driverName);

        Receipt issuedReceipt = new ReceiptGenerator().generateReceipt(rideOrder.getRideOrderId(), "Aibek", 1000.0);
        receiptsOnDisk.saveReceipt(issuedReceipt);
        rideOrder.attachReceiptAndMoveToPendingC(issuedReceipt.getReceiptId());
        rideOrdersOnDisk.saveRideOrder(rideOrder);
        return rideOrder;
    }

    // ---------- look at the files ----------

    RideOrder theOnlyRide() {
        assertEquals(1, rideOrdersOnDisk.findAllRideOrders().size(), "number of rides on disk");
        return rideOrdersOnDisk.findAllRideOrders().get(0);
    }

    void assertTheOnlyRideIs(RideOrderStatus expectedStatus) {
        assertEquals(expectedStatus, theOnlyRide().getStatus());
    }

    void assertNoRidesExist() {
        assertEquals(0, rideOrdersOnDisk.findAllRideOrders().size(), "rides on disk");
    }

    void assertDriverIs(String driverName, DriverStatus expectedStatus) {
        for (Driver driver : driversOnDisk.findAllDrivers()) {
            if (driver.getDriverName().equals(driverName)) {
                assertEquals(expectedStatus, driver.getCurrentStatus(), "status of driver " + driverName);
                return;
            }
        }
        fail("No driver named " + driverName + " on disk");
    }

    void assertTheOnlyReceiptIs(ReceiptStatus expectedStatus) {
        assertEquals(1, receiptsOnDisk.findAllReceipts().size(), "number of receipts on disk");
        assertEquals(expectedStatus, receiptsOnDisk.findAllReceipts().get(0).getStatus());
    }

    void assertNoReceiptsExist() {
        assertEquals(0, receiptsOnDisk.findAllReceipts().size(), "receipts on disk");
    }

    void assertTheOnlyPaymentIs(PaymentStatus expectedStatus) {
        assertEquals(1, paymentsOnDisk.findAllPayments().size(), "number of payments on disk");
        assertEquals(expectedStatus, paymentsOnDisk.findAllPayments().get(0).getStatus());
    }

    void assertNoPaymentsExist() {
        assertEquals(0, paymentsOnDisk.findAllPayments().size(), "payments on disk");
    }

    int driverRatingOfTheOnlyPayment() {
        return paymentsOnDisk.findAllPayments().get(0).getDriverRatingInStars();
    }

    // ---------- console ----------

    /** Runs the action and returns everything it printed (used to check the ORDER of the compensation steps). */
    String consoleOutputWhile(Runnable action) {
        PrintStream originalConsole = System.out;
        ByteArrayOutputStream capturedText = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedText, true));
        try {
            action.run();
        } finally {
            System.setOut(originalConsole);
        }
        return capturedText.toString();
    }
}
