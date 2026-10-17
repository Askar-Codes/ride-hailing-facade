package ridehailing.app;

import ridehailing.facade.RideAdminFacade;
import ridehailing.facade.RideBookingFacade;
import ridehailing.facade.RideRequest;

import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

/**
 * The client of the demo. It knows only the two facades and RideRequest.
 * Run: mvn -q compile exec:java    (or run main() from the IDE)
 * Reset to a clean state: delete the "data" folder.
 */
public class ConsoleApp {

    private static final Path FOLDER_WHERE_DATA_IS_STORED = Path.of("data");

    private static final String CARD_THE_BANK_ACCEPTS = "4111111111111111";
    private static final String CARD_THE_BANK_DECLINES = "4000000000000000";   // ends with 0000

    private static RideBookingFacade rideBookingFacade;
    private static RideAdminFacade rideAdminFacade;
    private static Scanner keyboardScanner;

    public static void main(String[] args) {
        keyboardScanner = new Scanner(System.in);
        startApplication();

        boolean applicationIsRunning = true;
        while (applicationIsRunning) {
            printMenu();
            String menuChoice = keyboardScanner.nextLine().trim();
            try {
                applicationIsRunning = handleMenuChoice(menuChoice);
            } catch (RuntimeException exception) {
                System.out.println(">>> FAILED: " + exception.getClass().getSimpleName()
                        + ": " + exception.getMessage());
                System.out.println(">>> What is saved on disk now:");
                showWhatIsSavedOnDisk();
            }
        }
        System.out.println("Bye. Everything is saved in the folder: " + FOLDER_WHERE_DATA_IS_STORED.toAbsolutePath());
    }

    /**
     * Creates the facades over the saved files (this is what a restart means) and
     * repairs the rides that a crash left in the middle of the chain.
     */
    private static void startApplication() {
        rideBookingFacade = new RideBookingFacade(FOLDER_WHERE_DATA_IS_STORED);
        rideAdminFacade = new RideAdminFacade(FOLDER_WHERE_DATA_IS_STORED);

        registerThreeDriversIfThereAreNoDriversYet();
        int numberOfRecoveredRides = rideAdminFacade.recoverUnfinishedRides();
        System.out.println("Application started. Rides recovered from disk: " + numberOfRecoveredRides);
    }

    private static void registerThreeDriversIfThereAreNoDriversYet() {
        if (rideAdminFacade.describeAllDrivers().isEmpty()) {
            System.out.println("First run: registering three drivers");
            rideAdminFacade.registerNewDriver("Ann", 1, 1);
            rideAdminFacade.registerNewDriver("Bob", 5, 5);
            rideAdminFacade.registerNewDriver("Carl", 9, 2);
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=========== RIDE-HAILING ===========");
        System.out.println("1  Book a ride (you type all the data)");
        System.out.println("2  DEMO: step B fails -> only A is compensated (unknown zone)");
        System.out.println("3  DEMO: step C fails -> B, then A are compensated (card declined)");
        System.out.println("4  DEMO: crash after step A (the ride stays PENDING_B on disk)");
        System.out.println("5  DEMO: crash after step B (the ride stays PENDING_C on disk)");
        System.out.println("6  Restart the application (open the files again and recover)");
        System.out.println("7  Show drivers");
        System.out.println("8  Show rides");
        System.out.println("9  Rate a completed ride");
        System.out.println("10 Finish a trip (driver becomes free)");
        System.out.println("11 Cancel a ride manually (admin compensation)");
        System.out.println("12 Add a driver");
        System.out.println("0  Exit");
        System.out.print("Your choice: ");
    }

    /** Returns false when the user wants to leave the application. */
    private static boolean handleMenuChoice(String menuChoice) {
        switch (menuChoice) {
            case "1":
                rideBookingFacade.bookRide(askUserForRideRequest());
                showWhatIsSavedOnDisk();
                break;
            case "2":
                rideBookingFacade.bookRide(createDemoRideRequest("MARS", CARD_THE_BANK_ACCEPTS));
                showWhatIsSavedOnDisk();
                break;
            case "3":
                rideBookingFacade.bookRide(createDemoRideRequest("CENTER", CARD_THE_BANK_DECLINES));
                showWhatIsSavedOnDisk();
                break;
            case "4":
                rideBookingFacade.simulateCrashAfterStepA(createDemoRideRequest("CENTER", CARD_THE_BANK_ACCEPTS));
                showWhatIsSavedOnDisk();
                break;
            case "5":
                rideBookingFacade.simulateCrashAfterStepB(createDemoRideRequest("CENTER", CARD_THE_BANK_ACCEPTS));
                showWhatIsSavedOnDisk();
                break;
            case "6":
                startApplication();
                showWhatIsSavedOnDisk();
                break;
            case "7":
                printAllLines(rideAdminFacade.describeAllDrivers());
                break;
            case "8":
                showWhatIsSavedOnDisk();
                break;
            case "9":
                rideBookingFacade.rateCompletedRide(askUser("Ride id"), Integer.parseInt(askUser("Stars (1-5)")));
                break;
            case "10":
                rideAdminFacade.finishTripAndMakeDriverFree(askUser("Ride id"));
                break;
            case "11":
                rideAdminFacade.cancelRideManually(askUser("Ride id"));
                break;
            case "12":
                rideAdminFacade.registerNewDriver(askUser("Driver name"),
                        Double.parseDouble(askUser("Location x")), Double.parseDouble(askUser("Location y")));
                break;
            case "0":
                return false;
            default:
                System.out.println("Unknown choice");
        }
        return true;
    }

    private static RideRequest askUserForRideRequest() {
        return new RideRequest(
                askUser("Rider name"),
                Double.parseDouble(askUser("Pickup x")), Double.parseDouble(askUser("Pickup y")),
                Double.parseDouble(askUser("Drop-off x")), Double.parseDouble(askUser("Drop-off y")),
                askUser("Zone (CENTER, SUBURB, AIRPORT)"),
                askUser("Card number (a card ending with 0000 is declined)"));
    }

    private static RideRequest createDemoRideRequest(String cityZoneName, String cardNumber) {
        System.out.println("Demo ride: rider Demo, from (0, 0) to (3, 4), zone " + cityZoneName
                + ", card " + cardNumber);
        return new RideRequest("Demo", 0, 0, 3, 4, cityZoneName, cardNumber);
    }

    private static void showWhatIsSavedOnDisk() {
        List<String> descriptionsOfRides = rideAdminFacade.describeAllRides();
        if (descriptionsOfRides.isEmpty()) {
            System.out.println("(no rides yet)");
        }
        printAllLines(descriptionsOfRides);
        System.out.println("Drivers:");
        printAllLines(rideAdminFacade.describeAllDrivers());
    }

    private static void printAllLines(List<String> lines) {
        for (String line : lines) {
            System.out.println("  " + line);
        }
    }

    private static String askUser(String question) {
        System.out.print(question + ": ");
        return keyboardScanner.nextLine().trim();
    }
}
