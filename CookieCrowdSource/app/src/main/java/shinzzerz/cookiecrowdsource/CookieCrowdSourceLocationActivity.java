package shinzzerz.cookiecrowdsource;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by administratorz on 9/2/2017.
 * <p>
 * The purpose of this activity is for testing purposes only. Trying to
 * obtain the location of current user via FINE_LOCATION GPS setting.
 */
public class CookieCrowdSourceLocationActivity extends AppCompatActivity {
    private final String NEW_LINE = System.getProperty("line.separator");
    private static final long LOCATION_REFRESH_TIME = 3;
    private static final float LOCATION_REFRESH_DISTANCE = 0.01f;
    private static int LOCATION_PERM_REQUEST = 0;


    private View myLocLayout;
    private LocationManager myLocationManager;
    private LocationListener myLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

            String latLong = "Lat: " + location.getLatitude() + NEW_LINE;
            latLong = latLong.concat("Longitude: " + location.getLongitude());

            locationOutputView.setText(latLong);
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

    protected
    @BindView(R.id.location_layout_button)
    Button findLocationButton;
    protected
    @BindView(R.id.location_layout_textView)
    TextView locationOutputView;

    @OnClick(R.id.location_layout_button)
    public void submit() {
        requestLocationPerm(false);
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        myLocLayout = getLayoutInflater().inflate(R.layout.layout, null);
        setContentView(myLocLayout);
        ButterKnife.bind(this);

        if (requestLocationPerm(false)) {
            getLocation();
        }
    }

    private void getLocation() {
        if (myLocationListener != null) {
        }
        myLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        try {
            myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                    LOCATION_REFRESH_DISTANCE, myLocationListener);
        } catch (SecurityException ex) {
            requestLocationPerm(true);
        }

    }

    private boolean requestLocationPerm(boolean showExplanation) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) && showExplanation) {
                (Toast.makeText(CookieCrowdSourceLocationActivity.this, R.string.location_perm_explntion, Toast.LENGTH_LONG)).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERM_REQUEST);
            }
        }

        return true;
    }
}
