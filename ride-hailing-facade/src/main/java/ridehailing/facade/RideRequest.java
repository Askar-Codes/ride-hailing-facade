package ridehailing.facade;

/**
 * Everything the client has to tell the facade to book a ride.
 * Only plain numbers and texts, so the client never needs a class from a subsystem.
 */
public class RideRequest {

    private final String riderName;
    private final double pickupXInKilometers;
    private final double pickupYInKilometers;
    private final double dropoffXInKilometers;
    private final double dropoffYInKilometers;
    private final String cityZoneName;
    private final String cardNumber;

    public RideRequest(String riderName,
                       double pickupXInKilometers, double pickupYInKilometers,
                       double dropoffXInKilometers, double dropoffYInKilometers,
                       String cityZoneName, String cardNumber) {
        this.riderName = riderName;
        this.pickupXInKilometers = pickupXInKilometers;
        this.pickupYInKilometers = pickupYInKilometers;
        this.dropoffXInKilometers = dropoffXInKilometers;
        this.dropoffYInKilometers = dropoffYInKilometers;
        this.cityZoneName = cityZoneName;
        this.cardNumber = cardNumber;
    }

    public String getRiderName() {
        return riderName;
    }

    public double getPickupXInKilometers() {
        return pickupXInKilometers;
    }

    public double getPickupYInKilometers() {
        return pickupYInKilometers;
    }

    public double getDropoffXInKilometers() {
        return dropoffXInKilometers;
    }

    public double getDropoffYInKilometers() {
        return dropoffYInKilometers;
    }

    public String getCityZoneName() {
        return cityZoneName;
    }

    public String getCardNumber() {
        return cardNumber;
    }
}
