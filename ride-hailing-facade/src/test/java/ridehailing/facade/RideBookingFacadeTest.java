package ridehailing.facade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ridehailing.driver.DriverStatus;
import ridehailing.driver.NoFreeDriverException;
import ridehailing.payment.PaymentDeclinedException;
import ridehailing.payment.PaymentStatus;
import ridehailing.pricing.PricingException;
import ridehailing.pricing.ReceiptStatus;

import java.io.UncheckedIOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RideBookingFacadeTest {

    @TempDir
    Path folderWhereDataIsStored;

    RideHailingTestWorld world;

    @BeforeEach
    void openApplicationOnEmptyFolder() {
        world = new RideHailingTestWorld(folderWhereDataIsStored);
    }

    // ---------- happy path ----------

    @Test
    void successfulRideChargesTheCardAndKeepsTheDriverBusy() {
        world.givenFreeDriver("Ann", 1, 1);

        RideOrder rideOrder = world.bookingFacade.bookRide(world.successfulRideRequest());

        assertEquals(RideOrderStatus.COMPLETED, rideOrder.getStatus());
        world.assertTheOnlyRideIs(RideOrderStatus.COMPLETED);
        world.assertDriverIs("Ann", DriverStatus.BUSY);
        world.assertTheOnlyReceiptIs(ReceiptStatus.PAID);
        world.assertTheOnlyPaymentIs(PaymentStatus.CHARGED);
    }

    @Test
    void nearestFreeDriverGetsTheRide() {
        world.givenFreeDriver("Far", 50, 50);
        world.givenFreeDriver("Near", 1, 1);

        world.bookingFacade.bookRide(world.successfulRideRequest());

        world.assertDriverIs("Near", DriverStatus.BUSY);
        world.assertDriverIs("Far", DriverStatus.FREE);
    }

    @Test
    void busyDriverIsNotGivenASecondRide() {
        world.givenFreeDriver("Ann", 1, 1);
        world.bookingFacade.bookRide(world.successfulRideRequest());

        assertThrows(NoFreeDriverException.class,
                () -> world.bookingFacade.bookRide(world.successfulRideRequest()));
    }

    // ---------- compensation depth 1: B fails after A ----------

    @Test
    void whenStepBFailsAfterStepA_onlyTheDriverIsCompensated() {
        world.givenFreeDriver("Ann", 1, 1);

        assertThrows(PricingException.class,
                () -> world.bookingFacade.bookRide(world.rideRequestToUnknownZone()));

        world.assertTheOnlyRideIs(RideOrderStatus.COMPENSATED);
        world.assertDriverIs("Ann", DriverStatus.FREE);
        world.assertNoReceiptsExist();
        world.assertNoPaymentsExist();
    }

    @Test
    void whenThePriceIsTooHigh_stepBFailsAndTheDriverIsFreeAgain() {
        world.givenFreeDriver("Ann", 1, 1);

        assertThrows(PricingException.class,
                () -> world.bookingFacade.bookRide(world.rideRequestTooLongToPrice()));

        world.assertTheOnlyRideIs(RideOrderStatus.COMPENSATED);
        world.assertDriverIs("Ann", DriverStatus.FREE);
    }

    @Test
    void whenStepBFails_onlyStepAIsCompensated_accordingToTheLog() {
        world.givenFreeDriver("Ann", 1, 1);

        String log = world.consoleOutputWhile(() -> assertThrows(PricingException.class,
                () -> world.bookingFacade.bookRide(world.rideRequestToUnknownZone())));

        assertTrue(log.contains("Compensating A"));
        assertFalse(log.contains("Compensating B"), "step B never finished, so it must not be undone");
        assertFalse(log.contains("Compensating C"), "step C never ran, so it must not be undone");
    }

    // ---------- compensation depth 2: C fails after A and B ----------

    @Test
    void whenStepCFailsAfterStepsAAndB_theReceiptAndTheDriverAreCompensated() {
        world.givenFreeDriver("Ann", 1, 1);

        assertThrows(PaymentDeclinedException.class,
                () -> world.bookingFacade.bookRide(world.rideRequestWithDeclinedCard()));

        world.assertTheOnlyRideIs(RideOrderStatus.COMPENSATED);
        world.assertTheOnlyReceiptIs(ReceiptStatus.VOIDED);
        world.assertTheOnlyPaymentIs(PaymentStatus.DECLINED);
        world.assertDriverIs("Ann", DriverStatus.FREE);
    }

    @Test
    void whenStepCFails_stepBIsUndoneFirstAndThenStepA() {
        world.givenFreeDriver("Ann", 1, 1);

        String log = world.consoleOutputWhile(() -> assertThrows(PaymentDeclinedException.class,
                () -> world.bookingFacade.bookRide(world.rideRequestWithDeclinedCard())));

        int positionOfStepBUndo = log.indexOf("Compensating B");
        int positionOfStepAUndo = log.indexOf("Compensating A");
        assertTrue(positionOfStepBUndo >= 0, "step B must be compensated");
        assertTrue(positionOfStepAUndo > positionOfStepBUndo, "step A must be compensated AFTER step B");
    }

    @Test
    void afterAFailedRideTheDriverCanTakeTheNextRide() {
        world.givenFreeDriver("Ann", 1, 1);
        assertThrows(PaymentDeclinedException.class,
                () -> world.bookingFacade.bookRide(world.rideRequestWithDeclinedCard()));

        world.bookingFacade.bookRide(world.successfulRideRequest());

        world.assertDriverIs("Ann", DriverStatus.BUSY);
    }

    // ---------- an unexpected error (not a "normal" business failure) in step B or step C ----------

    @Test
    void whenTheStorageBreaksInStepB_stepAIsStillCompensated() {
        world.givenFreeDriver("Ann", 1, 1);
        world.givenTheStorageOfReceiptsIsBroken();

        assertThrows(UncheckedIOException.class,
                () -> world.bookingFacade.bookRide(world.successfulRideRequest()));

        world.assertTheOnlyRideIs(RideOrderStatus.COMPENSATED);
        world.assertDriverIs("Ann", DriverStatus.FREE);
    }

    @Test
    void whenTheStorageBreaksInStepC_receiptAndDriverAreStillCompensated() {
        world.givenFreeDriver("Ann", 1, 1);
        world.givenTheStorageOfPaymentsIsBroken();

        assertThrows(UncheckedIOException.class,
                () -> world.bookingFacade.bookRide(world.successfulRideRequest()));

        world.assertTheOnlyRideIs(RideOrderStatus.COMPENSATED);
        world.assertTheOnlyReceiptIs(ReceiptStatus.VOIDED);
        world.assertDriverIs("Ann", DriverStatus.FREE);
    }

    // ---------- failures before anything is saved ----------

    @Test
    void rideWithoutRiderNameIsRejectedBeforeAnythingIsSaved() {
        world.givenFreeDriver("Ann", 1, 1);
        RideRequest requestWithoutRiderName =
                new RideRequest(" ", 0, 0, 3, 4, "CENTER", RideHailingTestWorld.CARD_THE_BANK_ACCEPTS);

        assertThrows(InvalidRideRequestException.class,
                () -> world.bookingFacade.bookRide(requestWithoutRiderName));

        world.assertNoRidesExist();
        world.assertDriverIs("Ann", DriverStatus.FREE);
    }

    @Test
    void rideWithEmptyZoneOrEmptyCardIsRejected() {
        world.givenFreeDriver("Ann", 1, 1);
        RideRequest requestWithEmptyZone =
                new RideRequest("Aibek", 0, 0, 3, 4, "", RideHailingTestWorld.CARD_THE_BANK_ACCEPTS);
        RideRequest requestWithEmptyCard = new RideRequest("Aibek", 0, 0, 3, 4, "CENTER", "");

        assertThrows(InvalidRideRequestException.class, () -> world.bookingFacade.bookRide(requestWithEmptyZone));
        assertThrows(InvalidRideRequestException.class, () -> world.bookingFacade.bookRide(requestWithEmptyCard));
        world.assertNoRidesExist();
    }

    @Test
    void rideWherePickupAndDropoffAreTheSamePlaceIsRejected() {
        world.givenFreeDriver("Ann", 1, 1);
        RideRequest rideGoingNowhere =
                new RideRequest("Aibek", 2, 2, 2, 2, "CENTER", RideHailingTestWorld.CARD_THE_BANK_ACCEPTS);

        assertThrows(InvalidRideRequestException.class, () -> world.bookingFacade.bookRide(rideGoingNowhere));

        world.assertNoRidesExist();
    }

    @Test
    void whenNoDriverIsFree_stepAFailsAndNothingIsSaved() {
        assertThrows(NoFreeDriverException.class,
                () -> world.bookingFacade.bookRide(world.successfulRideRequest()));

        world.assertNoRidesExist();
        world.assertNoReceiptsExist();
        world.assertNoPaymentsExist();
    }

    // ---------- the crash simulation used by the demo ----------

    @Test
    void simulatedCrashAfterStepALeavesARidePendingBOnDisk() {
        world.givenFreeDriver("Ann", 1, 1);

        world.bookingFacade.simulateCrashAfterStepA(world.successfulRideRequest());

        world.assertTheOnlyRideIs(RideOrderStatus.PENDING_B);
        world.assertDriverIs("Ann", DriverStatus.RESERVED);
        world.assertNoReceiptsExist();
    }

    @Test
    void simulatedCrashAfterStepBLeavesARidePendingCOnDisk() {
        world.givenFreeDriver("Ann", 1, 1);

        world.bookingFacade.simulateCrashAfterStepB(world.successfulRideRequest());

        world.assertTheOnlyRideIs(RideOrderStatus.PENDING_C);
        world.assertDriverIs("Ann", DriverStatus.RESERVED);
        world.assertTheOnlyReceiptIs(ReceiptStatus.ISSUED);
        world.assertNoPaymentsExist();
    }

    @Test
    void simulatedCrashAfterStepB_stillCompensatesStepAWhenStepBItselfFails() {
        world.givenFreeDriver("Ann", 1, 1);

        assertThrows(PricingException.class,
                () -> world.bookingFacade.simulateCrashAfterStepB(world.rideRequestToUnknownZone()));

        world.assertTheOnlyRideIs(RideOrderStatus.COMPENSATED);
        world.assertDriverIs("Ann", DriverStatus.FREE);
    }

    @Test
    void simulatedCrashStillRejectsAnInvalidRequestBeforeAnythingIsSaved() {
        world.givenFreeDriver("Ann", 1, 1);
        RideRequest requestWithoutRiderName =
                new RideRequest("", 0, 0, 3, 4, "CENTER", RideHailingTestWorld.CARD_THE_BANK_ACCEPTS);

        assertThrows(InvalidRideRequestException.class,
                () -> world.bookingFacade.simulateCrashAfterStepA(requestWithoutRiderName));
        assertThrows(InvalidRideRequestException.class,
                () -> world.bookingFacade.simulateCrashAfterStepB(requestWithoutRiderName));

        world.assertNoRidesExist();
    }

    // ---------- rating ----------

    @Test
    void completedRideCanBeRatedAndTheRatingIsSaved() {
        world.givenFreeDriver("Ann", 1, 1);
        RideOrder rideOrder = world.bookingFacade.bookRide(world.successfulRideRequest());

        world.bookingFacade.rateCompletedRide(rideOrder.getRideOrderId(), 5);

        assertEquals(5, world.driverRatingOfTheOnlyPayment());
    }

    @Test
    void compensatedRideCannotBeRated() {
        world.givenFreeDriver("Ann", 1, 1);
        assertThrows(PaymentDeclinedException.class,
                () -> world.bookingFacade.bookRide(world.rideRequestWithDeclinedCard()));
        String idOfCompensatedRide = world.theOnlyRide().getRideOrderId();

        assertThrows(IllegalStateException.class,
                () -> world.bookingFacade.rateCompletedRide(idOfCompensatedRide, 5));
    }
}
