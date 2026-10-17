package ridehailing.pricing;

/** Step B cannot price the ride (unknown zone, or the price is above the allowed maximum). */
public class PricingException extends RuntimeException {

    public PricingException(String message) {
        super(message);
    }
}
