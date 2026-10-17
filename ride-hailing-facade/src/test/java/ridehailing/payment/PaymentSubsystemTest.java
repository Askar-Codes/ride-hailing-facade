package ridehailing.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Subsystem C tested ALONE: no facade, no other subsystem. */
class PaymentSubsystemTest {

    @TempDir
    Path folderWhereDataIsStored;

    // ---------- PaymentProcessor ----------

    @Test
    void bankChargesAnOrdinaryCard() {
        Payment payment = new PaymentProcessor().chargeCard("ORD-1", "4111111111111111", 1000.0);

        assertEquals(PaymentStatus.CHARGED, payment.getStatus());
        assertEquals("1111", payment.getCardLastFourDigits());
        assertEquals(1000.0, payment.getAmount(), 0.0001);
        assertEquals(0, payment.getDriverRatingInStars());
    }

    @Test
    void bankDeclinesACardEndingWithFourZeros() {
        Payment payment = new PaymentProcessor().chargeCard("ORD-1", "4000000000000000", 1000.0);

        assertEquals(PaymentStatus.DECLINED, payment.getStatus());
    }

    @Test
    void bankDeclinesAMissingOrTooShortCard() {
        Payment paymentWithoutCard = new PaymentProcessor().chargeCard("ORD-1", null, 1000.0);
        Payment paymentWithTooShortCard = new PaymentProcessor().chargeCard("ORD-1", "12", 1000.0);

        assertEquals(PaymentStatus.DECLINED, paymentWithoutCard.getStatus());
        assertEquals(PaymentStatus.DECLINED, paymentWithTooShortCard.getStatus());
    }

    // ---------- Payment status ----------

    @Test
    void chargedPaymentCanBeRefundedOnlyOnce() {
        Payment payment = new PaymentProcessor().chargeCard("ORD-1", "4111111111111111", 1000.0);

        payment.markAsRefunded();

        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertThrows(IllegalStateException.class, payment::markAsRefunded);
    }

    @Test
    void declinedPaymentCannotBeChargedOrRefunded() {
        Payment payment = new PaymentProcessor().chargeCard("ORD-1", "4000000000000000", 1000.0);

        assertThrows(IllegalStateException.class, payment::markAsCharged);
        assertThrows(IllegalStateException.class, payment::markAsRefunded);
    }

    @Test
    void everyStatusKnowsWhichStatusesItMayChangeTo() {
        assertFalse(PaymentStatus.NEW.isAllowedToChangeTo(PaymentStatus.REFUNDED));
        assertFalse(PaymentStatus.CHARGED.isAllowedToChangeTo(PaymentStatus.DECLINED));
        assertFalse(PaymentStatus.REFUNDED.isAllowedToChangeTo(PaymentStatus.CHARGED));
    }

    // ---------- PaymentRepository (real file) ----------

    @Test
    void savedPaymentIsReadBackByAnotherRepositoryObject() {
        Payment payment = new PaymentProcessor().chargeCard("ORD-1", "4111111111111111", 1000.0);
        new PaymentRepository(folderWhereDataIsStored).savePayment(payment);

        Payment paymentReadBack =
                new PaymentRepository(folderWhereDataIsStored).findPaymentById(payment.getPaymentId());

        assertEquals(PaymentStatus.CHARGED, paymentReadBack.getStatus());
        assertEquals("ORD-1", paymentReadBack.getRideOrderId());
        assertEquals("1111", paymentReadBack.getCardLastFourDigits());
    }

    @Test
    void unknownPaymentIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new PaymentRepository(folderWhereDataIsStored).findPaymentById("PAY-none"));
    }

    // ---------- RatingService ----------

    @Test
    void riderCanRateTheDriverAfterTheCardWasCharged() {
        PaymentRepository paymentRepository = new PaymentRepository(folderWhereDataIsStored);
        Payment payment = new PaymentProcessor().chargeCard("ORD-1", "4111111111111111", 1000.0);
        paymentRepository.savePayment(payment);

        new RatingService(paymentRepository).rateDriver(payment.getPaymentId(), 5);

        Payment paymentReadBack =
                new PaymentRepository(folderWhereDataIsStored).findPaymentById(payment.getPaymentId());
        assertEquals(5, paymentReadBack.getDriverRatingInStars());
    }

    @Test
    void ratingMustBeBetweenOneAndFiveStars() {
        PaymentRepository paymentRepository = new PaymentRepository(folderWhereDataIsStored);
        Payment payment = new PaymentProcessor().chargeCard("ORD-1", "4111111111111111", 1000.0);
        paymentRepository.savePayment(payment);
        RatingService ratingService = new RatingService(paymentRepository);

        assertThrows(IllegalArgumentException.class, () -> ratingService.rateDriver(payment.getPaymentId(), 0));
        assertThrows(IllegalArgumentException.class, () -> ratingService.rateDriver(payment.getPaymentId(), 6));
    }

    @Test
    void declinedPaymentCannotBeRated() {
        PaymentRepository paymentRepository = new PaymentRepository(folderWhereDataIsStored);
        Payment declinedPayment = new PaymentProcessor().chargeCard("ORD-1", "4000000000000000", 1000.0);
        paymentRepository.savePayment(declinedPayment);

        assertThrows(IllegalStateException.class,
                () -> new RatingService(paymentRepository).rateDriver(declinedPayment.getPaymentId(), 5));
    }
}
