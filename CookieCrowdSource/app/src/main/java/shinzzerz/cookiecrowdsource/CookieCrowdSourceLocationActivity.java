package shinzzerz.cookiecrowdsource;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import shinzzerz.location.GetCurrentLocation;
import shinzzerz.location.SimpleLocation;

/**
 * Created by administratorz on 9/2/2017.
 * <p>
 * The purpose of this activity is for testing purposes only. Trying to
 * obtain the shinzzer.location of current user via FINE_LOCATION GPS setting.
 */
public class CookieCrowdSourceLocationActivity extends AppCompatActivity {
    private final String NEW_LINE = System.getProperty("line.separator");
    private SimpleLocation location;
    private GetCurrentLocation getCurrentLocation;
    private View myLocLayout;
    private double lat;
    private double longg;

    protected
    @BindView(R.id.location_layout_textView)
    TextView locationOutputView;

    protected
    @BindView(R.id.location_find_on_map_button)
    Button mapButton;

    @OnClick(R.id.location_layout_button)
    public void submit() {
        if (location.getLat() != Double.POSITIVE_INFINITY && location.getLong() != Double.POSITIVE_INFINITY) {
            lat = location.getLat();
            longg = location.getLong();
            String locationString = "Lat: " + String.valueOf(lat);
            locationString += NEW_LINE + "Long: " + String.valueOf(longg);
            locationOutputView.setText(locationString);

            if(mapButton.getVisibility() == View.INVISIBLE || mapButton.getVisibility() == View.GONE){
                mapButton.setVisibility(View.VISIBLE);
            }

            //update the location after hitting button
            getCurrentLocation.getLocation(this, false, location);
        }
    }

    @OnClick(R.id.location_find_on_map_button)
    public void click() {
        Uri gmmIntentUri = Uri.parse("geo:<" + lat  + ">,<" + longg + ">?q=<" + lat  + ">,<" + longg + ">(" + "current location" + ")");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        }
        else{
            Toast.makeText(getApplicationContext(), "Could not find google maps!", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        location = new SimpleLocation();
        getCurrentLocation = new GetCurrentLocation();
        getCurrentLocation.getLocation(this, false, location);

        myLocLayout = getLayoutInflater().inflate(R.layout.layout, null);
        setContentView(myLocLayout);
        ButterKnife.bind(this);
    }
}
