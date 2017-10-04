package shinzzerz.cookiecrowdsource;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;

import butterknife.BindView;

/**
 * Created by administratorz on 9/2/2017.
 */

public class CookieCrowdSourceMainActivity extends AppCompatActivity{
//    protected @BindView(R.id.main_layout)
//    RelativeLayout mainActivity;

    @Override
    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        //eventually do some application specific loading
        //depending on app & user state
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        RelativeLayout mainActivity;
        mainActivity = (RelativeLayout)getLayoutInflater().inflate(R.layout.main_layout, null);
        setContentView(mainActivity);

//        Intent locationIntent = new Intent(this, CookieCrowdSourceLocationActivity.class);
//        startActivity(locationIntent);
    }
}
