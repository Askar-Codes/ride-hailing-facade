package ridehailing.pricing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Subsystem B tested ALONE: no facade, no other subsystem. */
class PricingSubsystemTest {

    @TempDir
    Path folderWhereDataIsStored;

    // ---------- PricingEngine ----------

    @Test
    void priceIsBaseFarePlusDistanceTimesThePriceOfTheZone() {
        assertEquals(1000.0, new PricingEngine().calculatePrice(5.0, "CENTER"), 0.0001);    // 500 + 5 * 100
        assertEquals(1150.0, new PricingEngine().calculatePrice(5.0, "SUBURB"), 0.0001);    // 500 + 5 * 130
        assertEquals(1400.0, new PricingEngine().calculatePrice(5.0, "AIRPORT"), 0.0001);   // 500 + 5 * 180
    }

    @Test
    void zoneNameIsNotCaseSensitive() {
        assertEquals(1000.0, new PricingEngine().calculatePrice(5.0, " center "), 0.0001);
    }

    @Test
    void unknownZoneHasNoTariff() {
        assertThrows(PricingException.class, () -> new PricingEngine().calculatePrice(5.0, "MARS"));
        assertThrows(PricingException.class, () -> new PricingEngine().calculatePrice(5.0, null));
    }

    @Test
    void priceAboveTheAllowedMaximumIsRejected() {
        assertThrows(PricingException.class, () -> new PricingEngine().calculatePrice(100.0, "CENTER"));
    }

    // ---------- ReceiptGenerator ----------

    @Test
    void generatedReceiptIsIssuedAndKnowsItsRideAndItsAmount() {
        Receipt receipt = new ReceiptGenerator().generateReceipt("ORD-1", "Aibek", 1000.0);

        assertEquals(ReceiptStatus.ISSUED, receipt.getStatus());
        assertEquals("ORD-1", receipt.getRideOrderId());
        assertEquals("Aibek", receipt.getRiderName());
        assertEquals(1000.0, receipt.getAmountToPay(), 0.0001);
        assertTrue(receipt.getReceiptId().startsWith("RCT-"));
    }

    // ---------- Receipt status ----------

    @Test
    void issuedReceiptCanBePaidAndThenVoided() {
        Receipt receipt = new ReceiptGenerator().generateReceipt("ORD-1", "Aibek", 1000.0);

        receipt.markAsPaid();
        assertEquals(ReceiptStatus.PAID, receipt.getStatus());
        receipt.markAsVoided();
        assertEquals(ReceiptStatus.VOIDED, receipt.getStatus());
    }

    @Test
    void issuedReceiptCanBeVoidedDirectly() {
        Receipt receipt = new ReceiptGenerator().generateReceipt("ORD-1", "Aibek", 1000.0);

        receipt.markAsVoided();

        assertEquals(ReceiptStatus.VOIDED, receipt.getStatus());
    }

    @Test
    void voidedReceiptCannotBePaidOrVoidedAgain() {
        Receipt receipt = new ReceiptGenerator().generateReceipt("ORD-1", "Aibek", 1000.0);
        receipt.markAsVoided();

        assertThrows(IllegalStateException.class, receipt::markAsPaid);
        assertThrows(IllegalStateException.class, receipt::markAsVoided);
    }

    @Test
    void paidReceiptCannotBePaidAgain() {
        Receipt receipt = new ReceiptGenerator().generateReceipt("ORD-1", "Aibek", 1000.0);
        receipt.markAsPaid();

        assertThrows(IllegalStateException.class, receipt::markAsPaid);
        assertFalse(ReceiptStatus.VOIDED.isAllowedToChangeTo(ReceiptStatus.ISSUED));
    }

    // ---------- ReceiptRepository (real file) ----------

    @Test
    void savedReceiptIsReadBackByAnotherRepositoryObject() {
        Receipt receipt = new ReceiptGenerator().generateReceipt("ORD-1", "Aibek", 1234.5);
        receipt.markAsPaid();
        new ReceiptRepository(folderWhereDataIsStored).saveReceipt(receipt);

        Receipt receiptReadBack =
                new ReceiptRepository(folderWhereDataIsStored).findReceiptById(receipt.getReceiptId());

        assertEquals(ReceiptStatus.PAID, receiptReadBack.getStatus());
        assertEquals(1234.5, receiptReadBack.getAmountToPay(), 0.0001);
        assertEquals("ORD-1", receiptReadBack.getRideOrderId());
    }

    @Test
    void savingAReceiptTwiceKeepsOnlyOneRow() {
        ReceiptRepository receiptRepository = new ReceiptRepository(folderWhereDataIsStored);
        Receipt receipt = new ReceiptGenerator().generateReceipt("ORD-1", "Aibek", 1000.0);
        receiptRepository.saveReceipt(receipt);
        receipt.markAsVoided();
        receiptRepository.saveReceipt(receipt);

        assertEquals(1, receiptRepository.findAllReceipts().size());
        assertEquals(ReceiptStatus.VOIDED, receiptRepository.findAllReceipts().get(0).getStatus());
    }

    @Test
    void unknownReceiptIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReceiptRepository(folderWhereDataIsStored).findReceiptById("RCT-none"));
    }
}
