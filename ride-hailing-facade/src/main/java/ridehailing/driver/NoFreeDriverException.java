package ridehailing.driver;

/** Step A cannot find anybody to drive. Nothing has been saved yet when this is thrown. */
public class NoFreeDriverException extends RuntimeException {

    public NoFreeDriverException(String message) {
        super(message);
    }
}
