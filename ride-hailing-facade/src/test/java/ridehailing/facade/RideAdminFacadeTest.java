package ridehailing.facade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ridehailing.driver.DriverStatus;
import ridehailing.payment.PaymentStatus;
import ridehailing.pricing.ReceiptStatus;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Persistence-reload tests (restart in the middle of the chain) and the administration workflow. */
class RideAdminFacadeTest {

    @TempDir
    Path folderWhereDataIsStored;

    RideHailingTestWorld world;

    @BeforeEach
    void openApplicationOnEmptyFolder() {
        world = new RideHailingTestWorld(folderWhereDataIsStored);
    }

    // ---------- restart in the middle of the chain ----------

    @Test
    void crashAfterStepA_restart_recovery_usingOnlyTheFacades() {
        world.givenFreeDriver("Ann", 1, 1);
        world.bookingFacade.simulateCrashAfterStepA(world.successfulRideRequest());

        world.restartApplication();
        world.adminFacade.recoverUnfinishedRides();

        world.assertTheOnlyRideIs(RideOrderStatus.COMPENSATED);
        world.assertDriverIs("Ann", DriverStatus.FREE);
    }

    @Test
    void crashAfterStepB_restart_recovery_usingOnlyTheFacades() {
        world.givenFreeDriver("Ann", 1, 1);
        world.bookingFacade.simulateCrashAfterStepB(world.successfulRideRequest());

        world.restartApplication();
        world.assertTheOnlyRideIs(RideOrderStatus.PENDING_C);   // still on disk after the restart
        world.adminFacade.recoverUnfinishedRides();

        world.assertTheOnlyRideIs(RideOrderStatus.COMPENSATED);
        world.assertTheOnlyReceiptIs(ReceiptStatus.VOIDED);
        world.assertDriverIs("Ann", DriverStatus.FREE);
    }

    @Test
    void afterRestart_aRideInterruptedAfterStepA_isCompensatedFromWhatIsOnDisk() {
        world.givenRideInterruptedAfterStepA("Ann");

        world.restartApplication();
        world.assertTheOnlyRideIs(RideOrderStatus.PENDING_B);   // still on disk after the restart
        world.assertDriverIs("Ann", DriverStatus.RESERVED);
        int numberOfRecoveredRides = world.adminFacade.recoverUnfinishedRides();

        assertEquals(1, numberOfRecoveredRides);
        world.assertTheOnlyRideIs(RideOrderStatus.COMPENSATED);
        world.assertDriverIs("Ann", DriverStatus.FREE);
    }

    @Test
    void afterRestart_aRideInterruptedAfterStepB_hasItsReceiptAndDriverCompensated() {
        world.givenRideInterruptedAfterStepB("Ann");

        world.restartApplication();
        world.assertTheOnlyRideIs(RideOrderStatus.PENDING_C);   // still on disk after the restart
        world.assertTheOnlyReceiptIs(ReceiptStatus.ISSUED);
        int numberOfRecoveredRides = world.adminFacade.recoverUnfinishedRides();

        assertEquals(1, numberOfRecoveredRides);
        world.assertTheOnlyRideIs(RideOrderStatus.COMPENSATED);
        world.assertTheOnlyReceiptIs(ReceiptStatus.VOIDED);
        world.assertDriverIs("Ann", DriverStatus.FREE);
    }

    @Test
    void afterRestart_aCardChargedJustBeforeTheCrashIsRefunded() {
        world.givenRideInterruptedAfterTheCardWasCharged("Ann");

        world.restartApplication();
        world.adminFacade.recoverUnfinishedRides();

        world.assertTheOnlyRideIs(RideOrderStatus.COMPENSATED);
        world.assertTheOnlyPaymentIs(PaymentStatus.REFUNDED);
        world.assertTheOnlyReceiptIs(ReceiptStatus.VOIDED);
        world.assertDriverIs("Ann", DriverStatus.FREE);
    }

    @Test
    void afterRestart_aCompletedRideIsLeftAsItIs() {
        world.givenFreeDriver("Ann", 1, 1);
        world.bookingFacade.bookRide(world.successfulRideRequest());

        world.restartApplication();
        int numberOfRecoveredRides = world.adminFacade.recoverUnfinishedRides();

        assertEquals(0, numberOfRecoveredRides);
        world.assertTheOnlyRideIs(RideOrderStatus.COMPLETED);
        world.assertDriverIs("Ann", DriverStatus.BUSY);
        world.assertTheOnlyPaymentIs(PaymentStatus.CHARGED);
    }

    @Test
    void recoveryIsSafeToRunTwice() {
        world.givenRideInterruptedAfterStepB("Ann");

        world.restartApplication();
        world.adminFacade.recoverUnfinishedRides();
        int numberOfRidesRecoveredSecondTime = world.adminFacade.recoverUnfinishedRides();

        assertEquals(0, numberOfRidesRecoveredSecondTime);
        world.assertTheOnlyRideIs(RideOrderStatus.COMPENSATED);
    }

    // ---------- manual compensation and other administration ----------

    @Test
    void cancellingACompletedRideRefundsVoidsAndFreesInTheOrderCThenBThenA() {
        world.givenFreeDriver("Ann", 1, 1);
        RideOrder rideOrder = world.bookingFacade.bookRide(world.successfulRideRequest());

        String log = world.consoleOutputWhile(
                () -> world.adminFacade.cancelRideManually(rideOrder.getRideOrderId()));

        world.assertTheOnlyRideIs(RideOrderStatus.COMPENSATED);
        world.assertTheOnlyPaymentIs(PaymentStatus.REFUNDED);
        world.assertTheOnlyReceiptIs(ReceiptStatus.VOIDED);
        world.assertDriverIs("Ann", DriverStatus.FREE);
        assertTrue(log.indexOf("Compensating C") < log.indexOf("Compensating B"));
        assertTrue(log.indexOf("Compensating B") < log.indexOf("Compensating A"));
    }

    @Test
    void aRideCannotBeCancelledTwice() {
        world.givenFreeDriver("Ann", 1, 1);
        RideOrder rideOrder = world.bookingFacade.bookRide(world.successfulRideRequest());
        world.adminFacade.cancelRideManually(rideOrder.getRideOrderId());

        assertThrows(IllegalStateException.class,
                () -> world.adminFacade.cancelRideManually(rideOrder.getRideOrderId()));
    }

    @Test
    void cancellingAnUnknownRideIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> world.adminFacade.cancelRideManually("ORD-unknown"));
    }

    @Test
    void finishingATripFreesTheDriverButKeepsTheRideCompleted() {
        world.givenFreeDriver("Ann", 1, 1);
        RideOrder rideOrder = world.bookingFacade.bookRide(world.successfulRideRequest());

        world.adminFacade.finishTripAndMakeDriverFree(rideOrder.getRideOrderId());

        world.assertDriverIs("Ann", DriverStatus.FREE);
        world.assertTheOnlyRideIs(RideOrderStatus.COMPLETED);
    }

    @Test
    void cancellingAfterTheTripWasFinishedStillRefundsTheMoney() {
        world.givenFreeDriver("Ann", 1, 1);
        RideOrder rideOrder = world.bookingFacade.bookRide(world.successfulRideRequest());
        world.adminFacade.finishTripAndMakeDriverFree(rideOrder.getRideOrderId());

        world.adminFacade.cancelRideManually(rideOrder.getRideOrderId());

        world.assertTheOnlyPaymentIs(PaymentStatus.REFUNDED);
        world.assertDriverIs("Ann", DriverStatus.FREE);
    }

    @Test
    void aRideThatIsNotCompletedCannotBeFinished() {
        world.givenRideInterruptedAfterStepA("Ann");
        String idOfPendingRide = world.theOnlyRide().getRideOrderId();

        assertThrows(IllegalStateException.class,
                () -> world.adminFacade.finishTripAndMakeDriverFree(idOfPendingRide));
    }

    // ---------- looking at what is saved ----------

    @Test
    void describeAllDriversShowsNameLocationAndStatus() {
        world.adminFacade.registerNewDriver("Bob", 5, 5);

        List<String> descriptions = world.adminFacade.describeAllDrivers();

        assertEquals(1, descriptions.size());
        assertTrue(descriptions.get(0).contains("Bob"));
        assertTrue(descriptions.get(0).contains("FREE"));
    }

    @Test
    void describeAllRidesShowsEveryStepThatIsSavedOnDisk() {
        world.givenFreeDriver("Ann", 1, 1);
        RideOrder rideOrder = world.bookingFacade.bookRide(world.successfulRideRequest());
        world.bookingFacade.rateCompletedRide(rideOrder.getRideOrderId(), 4);

        List<String> descriptions = world.adminFacade.describeAllRides();

        assertEquals(1, descriptions.size());
        assertTrue(descriptions.get(0).contains("COMPLETED"));
        assertTrue(descriptions.get(0).contains("PAID"));
        assertTrue(descriptions.get(0).contains("CHARGED"));
        assertTrue(descriptions.get(0).contains("rated 4 stars"));
    }

    @Test
    void describeAllRidesForARideInterruptedAfterStepAShowsNoReceiptAndNoPayment() {
        world.givenRideInterruptedAfterStepA("Ann");

        List<String> descriptions = world.adminFacade.describeAllRides();

        assertTrue(descriptions.get(0).contains("PENDING_B"));
        assertTrue(descriptions.get(0).contains("no receipt"));
        assertTrue(descriptions.get(0).contains("no payment"));
    }
}
