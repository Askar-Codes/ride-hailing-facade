package ridehailing.payment;

/** Lets the rider rate the driver (1-5 stars) after the payment was charged. */
public class RatingService {

    private final PaymentRepository paymentRepository;

    public RatingService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public void rateDriver(String paymentId, int numberOfStars) {
        if (numberOfStars < 1 || numberOfStars > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5 stars, got " + numberOfStars);
        }
        Payment payment = paymentRepository.findPaymentById(paymentId);
        if (payment.getStatus() != PaymentStatus.CHARGED) {
            throw new IllegalStateException(
                    "Only a charged payment can be rated, this one is " + payment.getStatus());
        }
        payment.setDriverRatingInStars(numberOfStars);
        paymentRepository.savePayment(payment);
    }
}
