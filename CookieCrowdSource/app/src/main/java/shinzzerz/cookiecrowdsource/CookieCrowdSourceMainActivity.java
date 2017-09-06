package shinzzerz.cookiecrowdsource;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by administratorz on 9/2/2017.
 */

public class CookieCrowdSourceMainActivity extends AppCompatActivity{

    @Override
    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        //eventually do some application specific loading
        //depending on app & user state

        Intent locationIntent = new Intent(this, CookieCrowdSourceLocationActivity.class);
        startActivity(locationIntent);
    }
}
