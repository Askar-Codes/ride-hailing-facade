package ridehailing.standalone;

import ridehailing.payment.Payment;
import ridehailing.payment.PaymentProcessor;
import ridehailing.payment.PaymentRepository;
import ridehailing.payment.RatingService;

import java.nio.file.Path;

/** Subsystem C used on its own, without the facade. Run it from the IDE. */
public class PaymentSubsystemMain {

    public static void main(String[] args) {
        PaymentRepository paymentRepository = new PaymentRepository(Path.of("data-standalone"));

        Payment chargedPayment = new PaymentProcessor().chargeCard("ORD-DEMO", "4111111111111111", 1000.0);
        paymentRepository.savePayment(chargedPayment);
        System.out.println("Payment status: " + chargedPayment.getStatus());

        new RatingService(paymentRepository).rateDriver(chargedPayment.getPaymentId(), 5);
        System.out.println("Saved rating: "
                + paymentRepository.findPaymentById(chargedPayment.getPaymentId()).getDriverRatingInStars());

        Payment declinedPayment = new PaymentProcessor().chargeCard("ORD-DEMO-2", "4000000000000000", 1000.0);
        System.out.println("Second payment status: " + declinedPayment.getStatus());
    }
}
