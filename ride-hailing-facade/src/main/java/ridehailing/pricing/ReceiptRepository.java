package ridehailing.pricing;

import ridehailing.storage.CsvFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Persistence gateway of subsystem B: talks to receipts.csv directly. */
public class ReceiptRepository {

    private final CsvFile receiptsCsvFile;

    public ReceiptRepository(Path folderWhereDataIsStored) {
        this.receiptsCsvFile = new CsvFile(folderWhereDataIsStored.resolve("receipts.csv"));
    }

    public void saveReceipt(Receipt receipt) {
        receiptsCsvFile.saveOrReplaceRowById(new String[]{
                receipt.getReceiptId(),
                receipt.getRideOrderId(),
                receipt.getRiderName(),
                String.valueOf(receipt.getAmountToPay()),
                receipt.getStatus().name()
        });
    }

    public List<Receipt> findAllReceipts() {
        List<Receipt> allReceipts = new ArrayList<>();
        for (String[] row : receiptsCsvFile.readAllRowsFromFile()) {
            allReceipts.add(new Receipt(row[0], row[1], row[2],
                    Double.parseDouble(row[3]), ReceiptStatus.valueOf(row[4])));
        }
        return allReceipts;
    }

    public Receipt findReceiptById(String receiptId) {
        for (Receipt receipt : findAllReceipts()) {
            if (receipt.getReceiptId().equals(receiptId)) {
                return receipt;
            }
        }
        throw new IllegalArgumentException("Receipt not found: " + receiptId);
    }
}
