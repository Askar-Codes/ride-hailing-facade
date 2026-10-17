package ridehailing.facade;

/** The request is wrong from the start. Nothing has been saved yet when this is thrown. */
public class InvalidRideRequestException extends RuntimeException {

    public InvalidRideRequestException(String message) {
        super(message);
    }
}
