package shinzzerz.cookiecrowdsource;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by administratorz on 9/2/2017.
 */
public class CookieCrowdSourceLocationActivity extends AppCompatActivity {
    private View myLocLayout;
    protected @BindView(R.id.location_layout_button) Button findLocationButton;
    protected @BindView(R.id.location_layout_textView) TextView locationOutputView;
    @OnClick(R.id.location_layout_button)
    public void submit(){
        locationOutputView.setText("Ouch!");
    }

    @Override
    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);

        myLocLayout = getLayoutInflater().inflate(R.layout.layout, null);
        setContentView(myLocLayout);
        ButterKnife.bind(this);
    }

    @Override
    public void onStart(){
        super.onStart();
    }

    private void getLocation(){

    }
}
