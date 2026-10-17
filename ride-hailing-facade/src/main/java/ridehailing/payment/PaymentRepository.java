package ridehailing.payment;

import ridehailing.storage.CsvFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Persistence gateway of subsystem C: talks to payments.csv directly. */
public class PaymentRepository {

    private final CsvFile paymentsCsvFile;

    public PaymentRepository(Path folderWhereDataIsStored) {
        this.paymentsCsvFile = new CsvFile(folderWhereDataIsStored.resolve("payments.csv"));
    }

    public void savePayment(Payment payment) {
        paymentsCsvFile.saveOrReplaceRowById(new String[]{
                payment.getPaymentId(),
                payment.getRideOrderId(),
                payment.getCardLastFourDigits(),
                String.valueOf(payment.getAmount()),
                payment.getStatus().name(),
                String.valueOf(payment.getDriverRatingInStars())
        });
    }

    public List<Payment> findAllPayments() {
        List<Payment> allPayments = new ArrayList<>();
        for (String[] row : paymentsCsvFile.readAllRowsFromFile()) {
            allPayments.add(new Payment(row[0], row[1], row[2], Double.parseDouble(row[3]),
                    PaymentStatus.valueOf(row[4]), Integer.parseInt(row[5])));
        }
        return allPayments;
    }

    public Payment findPaymentById(String paymentId) {
        for (Payment payment : findAllPayments()) {
            if (payment.getPaymentId().equals(paymentId)) {
                return payment;
            }
        }
        throw new IllegalArgumentException("Payment not found: " + paymentId);
    }
}
