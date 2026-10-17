package ridehailing.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The saved progress of a ride enforces its own allowed changes and survives being written and read again. */
class RideOrderTest {

    @TempDir
    Path folderWhereDataIsStored;

    @Test
    void newRideStartsAsPendingBWithoutReceiptAndWithoutPayment() {
        RideOrder rideOrder = new RideOrder("Aibek", "DRV-1");

        assertEquals(RideOrderStatus.PENDING_B, rideOrder.getStatus());
        assertFalse(rideOrder.hasReceiptAttached());
        assertFalse(rideOrder.hasPaymentAttached());
    }

    @Test
    void rideWalksThroughTheWholeChainInOrder() {
        RideOrder rideOrder = new RideOrder("Aibek", "DRV-1");

        rideOrder.attachReceiptAndMoveToPendingC("RCT-1");
        assertEquals(RideOrderStatus.PENDING_C, rideOrder.getStatus());
        rideOrder.attachPayment("PAY-1");
        rideOrder.markAsCompleted();

        assertEquals(RideOrderStatus.COMPLETED, rideOrder.getStatus());
        assertTrue(rideOrder.hasReceiptAttached());
        assertTrue(rideOrder.hasPaymentAttached());
    }

    @Test
    void stepCCannotBeSkipped() {
        RideOrder rideOrder = new RideOrder("Aibek", "DRV-1");

        assertThrows(IllegalStateException.class, rideOrder::markAsCompleted);
        assertThrows(IllegalStateException.class, () -> rideOrder.attachPayment("PAY-1"));
    }

    @Test
    void compensatedRideCannotChangeAnymore() {
        RideOrder rideOrder = new RideOrder("Aibek", "DRV-1");
        rideOrder.markAsCompensated();

        assertThrows(IllegalStateException.class, () -> rideOrder.attachReceiptAndMoveToPendingC("RCT-1"));
        assertThrows(IllegalStateException.class, rideOrder::markAsCompensated);
    }

    @Test
    void everyStatusKnowsWhichStatusesItMayChangeTo() {
        assertTrue(RideOrderStatus.PENDING_B.isAllowedToChangeTo(RideOrderStatus.PENDING_C));
        assertTrue(RideOrderStatus.PENDING_B.isAllowedToChangeTo(RideOrderStatus.COMPENSATED));
        assertFalse(RideOrderStatus.PENDING_B.isAllowedToChangeTo(RideOrderStatus.COMPLETED));
        assertTrue(RideOrderStatus.PENDING_C.isAllowedToChangeTo(RideOrderStatus.COMPLETED));
        assertTrue(RideOrderStatus.PENDING_C.isAllowedToChangeTo(RideOrderStatus.COMPENSATED));
        assertTrue(RideOrderStatus.COMPLETED.isAllowedToChangeTo(RideOrderStatus.COMPENSATED));
        assertFalse(RideOrderStatus.COMPLETED.isAllowedToChangeTo(RideOrderStatus.PENDING_B));
        assertFalse(RideOrderStatus.COMPENSATED.isAllowedToChangeTo(RideOrderStatus.PENDING_B));
    }

    @Test
    void rideIsReadBackFromDiskExactlyAsItWasSaved() {
        RideOrder rideOrder = new RideOrder("Aibek", "DRV-1");
        rideOrder.attachReceiptAndMoveToPendingC("RCT-1");
        new RideOrderRepository(folderWhereDataIsStored).saveRideOrder(rideOrder);

        RideOrder rideOrderReadBack =
                new RideOrderRepository(folderWhereDataIsStored).findRideOrderById(rideOrder.getRideOrderId());

        assertEquals(RideOrderStatus.PENDING_C, rideOrderReadBack.getStatus());
        assertEquals("RCT-1", rideOrderReadBack.getReceiptId());
        assertEquals("DRV-1", rideOrderReadBack.getDriverId());
        assertFalse(rideOrderReadBack.hasPaymentAttached());
    }

    @Test
    void savingTheSameRideTwiceUpdatesItInsteadOfAddingAnotherRow() {
        RideOrderRepository rideOrderRepository = new RideOrderRepository(folderWhereDataIsStored);
        RideOrder rideOrder = new RideOrder("Aibek", "DRV-1");
        rideOrderRepository.saveRideOrder(rideOrder);
        rideOrder.markAsCompensated();
        rideOrderRepository.saveRideOrder(rideOrder);

        assertEquals(1, rideOrderRepository.findAllRideOrders().size());
        assertEquals(RideOrderStatus.COMPENSATED, rideOrderRepository.findAllRideOrders().get(0).getStatus());
    }

    @Test
    void unknownRideIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new RideOrderRepository(folderWhereDataIsStored).findRideOrderById("ORD-none"));
    }
}
