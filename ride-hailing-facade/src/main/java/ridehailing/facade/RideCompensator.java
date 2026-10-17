package ridehailing.facade;

import ridehailing.driver.Driver;
import ridehailing.driver.DriverRepository;
import ridehailing.payment.Payment;
import ridehailing.payment.PaymentRepository;
import ridehailing.payment.PaymentStatus;
import ridehailing.pricing.Receipt;
import ridehailing.pricing.ReceiptRepository;
import ridehailing.pricing.ReceiptStatus;

/**
 * Undoes a ride. It does NOT trust any in-memory flag: it reads the ride from disk and
 * undoes only what the saved ride says was done, always in the order C -> B -> A.
 * Both facades use it (the booking facade after a failure, the admin facade for manual
 * cancellation and for recovery after a crash).
 * Every undo is guarded, so running it twice does no harm.
 */
public class RideCompensator {

    private final DriverRepository driverRepository;
    private final ReceiptRepository receiptRepository;
    private final PaymentRepository paymentRepository;
    private final RideOrderRepository rideOrderRepository;

    public RideCompensator(DriverRepository driverRepository,
                           ReceiptRepository receiptRepository,
                           PaymentRepository paymentRepository,
                           RideOrderRepository rideOrderRepository) {
        this.driverRepository = driverRepository;
        this.receiptRepository = receiptRepository;
        this.paymentRepository = paymentRepository;
        this.rideOrderRepository = rideOrderRepository;
    }

    public void compensateRide(String rideOrderId) {
        RideOrder rideOrder = rideOrderRepository.findRideOrderById(rideOrderId);
        if (rideOrder.getStatus() == RideOrderStatus.COMPENSATED) {
            throw new IllegalStateException("Ride " + rideOrderId + " is already compensated");
        }

        // Step C was tried -> give the money back if the card was really charged
        if (rideOrder.hasPaymentAttached()) {
            refundPaymentIfItWasCharged(rideOrder.getPaymentId());
        }
        // Step B was done -> void the receipt
        if (rideOrder.hasReceiptAttached()) {
            voidReceiptIfItIsNotVoidedYet(rideOrder.getReceiptId());
        }
        // Step A was always done -> the driver is free again
        makeDriverFreeIfHeIsNotFreeYet(rideOrder.getDriverId());

        rideOrder.markAsCompensated();
        rideOrderRepository.saveRideOrder(rideOrder);
        System.out.println("[COMPENSATED] Ride " + rideOrderId + " is rolled back");
    }

    private void refundPaymentIfItWasCharged(String paymentId) {
        Payment payment = paymentRepository.findPaymentById(paymentId);
        if (payment.getStatus() == PaymentStatus.CHARGED) {
            payment.markAsRefunded();
            paymentRepository.savePayment(payment);
            System.out.println("Compensating C: payment " + paymentId + " refunded");
        }
    }

    private void voidReceiptIfItIsNotVoidedYet(String receiptId) {
        Receipt receipt = receiptRepository.findReceiptById(receiptId);
        if (receipt.getStatus() != ReceiptStatus.VOIDED) {
            receipt.markAsVoided();
            receiptRepository.saveReceipt(receipt);
            System.out.println("Compensating B: receipt " + receiptId + " voided");
        }
    }

    private void makeDriverFreeIfHeIsNotFreeYet(String driverId) {
        Driver driver = driverRepository.findDriverById(driverId);
        if (!driver.isFreeForNewRide()) {
            driver.becomeFreeAgain();
            driverRepository.saveDriver(driver);
            System.out.println("Compensating A: driver " + driver.getDriverName() + " is FREE again");
        }
    }
}
