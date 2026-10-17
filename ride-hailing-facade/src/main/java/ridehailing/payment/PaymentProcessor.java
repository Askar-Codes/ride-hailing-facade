package ridehailing.payment;

import java.util.UUID;

/**
 * Business logic only: decides whether the (fake) bank accepts the card.
 * Rule of our fake bank: a card number shorter than 4 characters or ending with 0000 is declined.
 * The returned payment is CHARGED or DECLINED; saving it is the repository's job.
 */
public class PaymentProcessor {

    public Payment chargeCard(String rideOrderId, String cardNumber, double amountToCharge) {
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
        Payment payment = new Payment(paymentId, rideOrderId, getLastFourDigitsOf(cardNumber),
                amountToCharge, PaymentStatus.NEW, 0);

        if (doesFakeBankAcceptCard(cardNumber)) {
            payment.markAsCharged();
        } else {
            payment.markAsDeclined();
        }
        return payment;
    }

    private boolean doesFakeBankAcceptCard(String cardNumber) {
        return cardNumber != null && cardNumber.length() >= 4 && !cardNumber.endsWith("0000");
    }

    private String getLastFourDigitsOf(String cardNumber) {
        if (cardNumber == null) {
            return "----";
        }
        return cardNumber.substring(Math.max(0, cardNumber.length() - 4));
    }
}
