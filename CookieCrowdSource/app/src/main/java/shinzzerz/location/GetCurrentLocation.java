package shinzzerz.location;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.Semaphore;

import shinzzerz.cookiecrowdsource.R;

import static android.content.Context.LOCATION_SERVICE;

/**
 * Created by administratorz on 9/10/2017.
 */

public class GetCurrentLocation {
    public static final int LOCATION_PERM_REQUEST = 0;

    private SimpleLocation aLocation;
    private final long LOCATION_REFRESH_TIME = 1;
    private final float LOCATION_REFRESH_DISTANCE = 0.01f;
    private Semaphore isLocationFoundNSet = new Semaphore(1, true);

    private LocationManager myLocationManager;
    private LocationListener myLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            aLocation.setLat(location.getLatitude());
            aLocation.setLong(location.getLongitude());

            //Stop updating once a new location has been found
            myLocationManager.removeUpdates(myLocationListener);
            isLocationFoundNSet.release();
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
    public void getLocation(final Activity callingActivity, boolean showExplanation, SimpleLocation location) {
        myLocationManager = (LocationManager) callingActivity.getSystemService(LOCATION_SERVICE);
        if (!isLocationPermAvailable(callingActivity)) {
            this.turnLocationPermOn(callingActivity, showExplanation);
        } else if (!isLocationOn(callingActivity)) {
            turnLocationOn(callingActivity, false);
        } else {
            aLocation = location;
            aLocation.setLat(Double.POSITIVE_INFINITY);
            aLocation.setLong(Double.POSITIVE_INFINITY);
            try {
                if(!isLocationFoundNSet.tryAcquire()){
                    return;
                }
                myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                        LOCATION_REFRESH_DISTANCE, myLocationListener);
            } catch (SecurityException e) {
                Log.e("GetCurrentLocation", "blah");
            }

            return;
        }
    }

    /**
     * This function will check if GPS is enabled on the phone.
     *
     * @return True: The phone has GPS location turned on. False: The phone has GPS location turned off.
     */
    public boolean isLocationOn(Activity callingActivity) {
        myLocationManager = (LocationManager) callingActivity.getSystemService(LOCATION_SERVICE);
        return myLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public boolean isLocationPermAvailable(Activity callingActivity) {
        return (ContextCompat.checkSelfPermission(callingActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * @param callingActivity
     */
    public void turnLocationOn(final Activity callingActivity, boolean showMessage) {
        if(!showMessage){
            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            callingActivity.startActivity(myIntent);
        }else{
            AlertDialog.Builder dialog = new AlertDialog.Builder(callingActivity);
            dialog.setMessage(R.string.turn_on_location_description);
            dialog.setPositiveButton(callingActivity.getResources().getString(R.string.turn_on_location_description), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    callingActivity.startActivity(myIntent);
                }
            });
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub
                }
            });

            dialog.show();
        }
    }


    /**
     * This function checks the permissions of the app first.
     *
     * @param callingActivity
     * @param showExplanation
     */
    public void turnLocationPermOn(Activity callingActivity, boolean showExplanation) {
        if (ContextCompat.checkSelfPermission(callingActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (showExplanation) {
                (Toast.makeText(callingActivity.getApplicationContext(), R.string.location_perm_explntion, Toast.LENGTH_LONG)).show();
            } else {
                ActivityCompat.requestPermissions(callingActivity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERM_REQUEST);
            }
        }
    }

    /**
     * Will return the distance from simpleLocation and the current location.
     * This function will not do any permission or location GPS checks.
     * @param callingActivity
     * @param simpleLocation
     * @return
     */
    public double getDistanceInMeters(Activity callingActivity, SimpleLocation simpleLocation) {
        SimpleLocation mySimpleLocation = new SimpleLocation();
        getLocation(callingActivity, false, mySimpleLocation);

        if(!isLocationFoundNSet.tryAcquire()){

        }
        else{
            float[] results = new float[1];
            Location.distanceBetween(simpleLocation.getLat(), simpleLocation.getLong(), mySimpleLocation.getLat(), mySimpleLocation.getLong(), results);
            //theDistance = results[0] / 1000;
        }

        return 0.0;
    }

    private class AquireLocation extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }
}

