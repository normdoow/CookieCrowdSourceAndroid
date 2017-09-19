package shinzzerz.location;

/**
 * Created by administratorz on 9/16/2017.
 */

public class SimpleLocation {
    private double Lat;
    private double Long;

    public SimpleLocation(){
        this(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    public SimpleLocation(double lat, double longg){
        Lat = lat;
        Long = longg;
    }

    public void setLat(double lat) {
        Lat = lat;
    }

    public double getLat() {
        return Lat;
    }

    public double getLong() {
        return Long;
    }

    public void setLong(double aLong) {
        Long = aLong;
    }
}