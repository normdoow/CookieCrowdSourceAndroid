package shinzzerz.location;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import shinzzerz.cookiecrowdsource.R;

import static android.content.Context.LOCATION_SERVICE;

/**
 * Created by administratorz on 9/10/2017.
 */

public class GetCurrentLocation {
    private int locationUpdateCount = 0;
    private SimpleLocation aLocation;
    private final String NEW_LINE = System.getProperty("line.separator");
    private final long LOCATION_REFRESH_TIME = 3;
    private final float LOCATION_REFRESH_DISTANCE = 0.01f;
    private int LOCATION_PERM_REQUEST = 0;
    private Activity callingActivity;
    private LocationManager myLocationManager;
    private LocationListener myLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            aLocation.setLat(location.getLatitude());
            aLocation.setLong(location.getLongitude());

            //Stop updating once a new location has been found
            myLocationManager.removeUpdates(myLocationListener);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    /**
     * This function will set the shinzzer.location of the SimpleLocation object once the
     * 'Fine Location' has been determined.
     *
     * @param callingActivity
     * @param showExplanation
     * @param location
     * @return
     */
    public void getLocation(Activity callingActivity, boolean showExplanation, SimpleLocation location) {
        myLocationManager = (LocationManager) callingActivity.getSystemService(LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(callingActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (showExplanation) {
                (Toast.makeText(callingActivity.getApplicationContext(), R.string.location_perm_explntion, Toast.LENGTH_LONG)).show();
            } else {
                ActivityCompat.requestPermissions(callingActivity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERM_REQUEST);
            }
        } else {
            aLocation = location;
            aLocation.setLat(Double.POSITIVE_INFINITY);
            aLocation.setLong(Double.POSITIVE_INFINITY);

            myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                    LOCATION_REFRESH_DISTANCE, myLocationListener);

            return;
        }
    }
}

