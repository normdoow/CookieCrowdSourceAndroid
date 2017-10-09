package shinzzerz.stripe;

/**
 * Created by noahbragg on 10/9/17.
 */

public class Address {

    private String mCity;
    private String mCountry;
    private String mLine1;
    private String mLine2;
    private String mPostalCode;
    private String mState;

    public Address(
            String city,
            String country,
            String line1,
            String line2,
            String postalCode,
            String state) {
        mCity = city;
        mCountry = country;
        mLine1 = line1;
        mLine2 = line2;
        mPostalCode = postalCode;
        mState = state;
    }

    public String getCity() {
        return mCity;
    }

    public void setCity(String city) {
        mCity = city;
    }

    public String getCountry() {
        return mCountry;
    }

    public void setCountry(String country) {
        mCountry = country;
    }

    public String getLine1() {
        return mLine1;
    }

    public void setLine1(String line1) {
        mLine1 = line1;
    }

    public String getLine2() {
        return mLine2;
    }

    public void setLine2(String line2) {
        mLine2 = line2;
    }

    public String getPostalCode() {
        return mPostalCode;
    }

    public void setPostalCode(String postalCode) {
        mPostalCode = postalCode;
    }

    public String getState() {
        return mState;
    }

    public void setState(String state) {
        mState = state;
    }

    public boolean isValidAddress() {
        return !mCity.isEmpty() && !mCountry.isEmpty() && !mLine1.isEmpty() && !mLine2.isEmpty() && !mPostalCode.isEmpty() && !mState.isEmpty();
    }
}
