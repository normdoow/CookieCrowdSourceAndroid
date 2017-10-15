package shinzzerz.cookiecrowdsource;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.gms.wallet.Cart;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.utils.CartManager;

import butterknife.BindView;

import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import shinzzerz.io.CookieIO;
import shinzzerz.location.GetCurrentLocation;
import shinzzerz.location.SimpleLocation;
import shinzzerz.restapi.CookieAPI;
import shinzzerz.stripe.PaymentActivity;
import shinzzerz.stripe.StoreUtils;

/**
 * Created by administratorz on 9/2/2017.
 */

public class CookieCrowdSourceMainActivity extends AppCompatActivity {
//    protected @BindView(R.id.main_layout)
//    RelativeLayout mainActivity;

    private static final String PUBLISHABLE_KEY = "pk_test_tAMChOZmT4OHrVNyhGvJmuLH";
    private GetCurrentLocation myLocation = new GetCurrentLocation();

    CookieAPI cookieAPI;
    @BindView(R.id.main_layout_get_cookies_button)
    Button getCookiesButton;

    @Override
    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        //eventually do some application specific loading
        //depending on app & user state

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        RelativeLayout mainActivity;
        mainActivity = (RelativeLayout)getLayoutInflater().inflate(R.layout.main_layout, null);
        setContentView(mainActivity);

        ButterKnife.bind(this);
        if(!myLocation.isLocationPermAvailable(this)){
            getCookiesButton.setText("Turn location on!");
            myLocation.turnLocationPermOn(this, false);
        }
        else if(!myLocation.isLocationOn(this)){
            getCookiesButton.setText("Turn location on!");
        }
        else{
            float distanceInMeters = myLocation.getDistanceInMeters(this, new SimpleLocation(R.string.CDC_lat, R.string.CDC_long));
            double distanceInMiles = distanceInMeters / 1600;
            if((int)Math.ceil(distanceInMiles) > 5){

            }
            else{

            }
        }

        initAndroidPay();

        //init the Retrofit instance for rest api
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CookieAPI.BASE_URL)
                .build();

        cookieAPI = retrofit.create(CookieAPI.class);

        apiCalls();
    }

    @OnClick(R.id.main_layout_get_cookies_button)
    public void onGetCookiesClicked(Button button) {
        if(!myLocation.isLocationPermAvailable(this)){
            myLocation.turnLocationPermOn(this, false);
        }
        else if(!myLocation.isLocationOn(this)){
            myLocation.turnLocationOn(this, false);
        }
        else{
            CartManager cartManager = new CartManager();
            cartManager.addLineItem(StoreUtils.getEmojiByUnicode(0x1F36A), (double) 1, 1000);
            try {
                Cart cart = cartManager.buildCart();
                Intent paymentLaunchIntent = PaymentActivity.createIntent(this, cart);
                startActivity(paymentLaunchIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initAndroidPay() {
        AndroidPayConfiguration payConfiguration =
                AndroidPayConfiguration.init(PUBLISHABLE_KEY, "USD");
        payConfiguration.setPhoneNumberRequired(false);
        payConfiguration.setShippingAddressRequired(true);
    }

    private void apiCalls() {
        Call<ResponseBody> callCookAvailable = cookieAPI.isCookAvailable();
        callCookAvailable.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                boolean isCookAvailable = false;
                try {
                    isCookAvailable = response.body().string().equals("True");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println();
            }
        });

        if(CookieIO.getCustomerId(this) == null) {      //create a new customer only if there isn't one already
            final Context con = this;
            Call<ResponseBody> callCreateCustomer = cookieAPI.createCustomer();
            callCreateCustomer.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        CookieIO.setCustomerId(con, response.body().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println();
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    System.out.println();
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case GetCurrentLocation.LOCATION_PERM_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    //The grantResult array length is greater than zero so permission
                    //was not cancelled
                    // permission was granted, turn location on
                    myLocation.turnLocationOn(this, false);

                } else {
                    //permission denied for some reason
                }
                return;
            }
        }
    }
}
