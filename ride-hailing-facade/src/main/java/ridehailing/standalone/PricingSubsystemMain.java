package ridehailing.standalone;

import ridehailing.pricing.PricingEngine;
import ridehailing.pricing.Receipt;
import ridehailing.pricing.ReceiptGenerator;
import ridehailing.pricing.ReceiptRepository;

import java.nio.file.Path;

/** Subsystem B used on its own, without the facade. Run it from the IDE. */
public class PricingSubsystemMain {

    public static void main(String[] args) {
        double price = new PricingEngine().calculatePrice(5.0, "CENTER");
        System.out.println("Price for 5 km in CENTER: " + price);

        Receipt receipt = new ReceiptGenerator().generateReceipt("ORD-DEMO", "Aibek", price);
        ReceiptRepository receiptRepository = new ReceiptRepository(Path.of("data-standalone"));
        receiptRepository.saveReceipt(receipt);

        receipt.markAsPaid();
        receiptRepository.saveReceipt(receipt);
        System.out.println("Saved status: " + receiptRepository.findReceiptById(receipt.getReceiptId()).getStatus());
    }
}
