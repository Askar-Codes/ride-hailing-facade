package ridehailing.payment;

/** Step C: the bank did not accept the card. */
public class PaymentDeclinedException extends RuntimeException {

    public PaymentDeclinedException(String message) {
        super(message);
    }
}
