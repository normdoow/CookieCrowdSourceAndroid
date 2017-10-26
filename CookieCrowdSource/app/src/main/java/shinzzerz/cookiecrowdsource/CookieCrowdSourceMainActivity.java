package shinzzerz.cookiecrowdsource;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.gms.wallet.Cart;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.utils.CartManager;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import shinzzerz.io.CookieIO;
import shinzzerz.location.DistTypeEnum;
import shinzzerz.location.GetCurrentLocation;
import shinzzerz.location.SimpleDistance;
import shinzzerz.location.SimpleLocation;
import shinzzerz.restapi.CookieAPI;
import shinzzerz.stripe.KeyKt;
import shinzzerz.stripe.PaymentActivity;
import shinzzerz.stripe.StoreUtils;

/**
 * Created by administratorz on 9/2/2017.
 */

public class CookieCrowdSourceMainActivity extends AppCompatActivity {
//    protected @BindView(R.id.main_layout)
//    RelativeLayout mainActivity;

    private static final double CDC_LAT = 39.691483;
    private static final double CDC_LONG = -84.101717;
    private GetCurrentLocation myLocation = new GetCurrentLocation();
    private boolean obtainedLocation = false;
    private long timeBeforeLocationTimeout = 30000;
    private boolean cookiesAvailable = false;
    private Context context;

    CookieAPI cookieAPI;
    @BindView(R.id.main_layout_get_cookies_button)
    Button getCookiesButton;

    @BindView(R.id.first_dozen_free_image)
    ImageView firstDozenImage;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        //eventually do some application specific loading
        //depending on app & user state
        context = this;

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout mainActivity;
        mainActivity = (LinearLayout) getLayoutInflater().inflate(R.layout.main_layout, null);
        setContentView(mainActivity);

        ButterKnife.bind(this);
        initAndroidPay();

        //init the Retrofit instance for rest api
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CookieAPI.BASE_URL)
                .build();

        cookieAPI = retrofit.create(CookieAPI.class);

        apiCalls();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupLocation();
        if (CookieIO.hasBoughtCookies(this)) {
            firstDozenImage.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        obtainedLocation = false;
    }

    @OnClick(R.id.ingredients_button)
    public void clickIngreditentsButton() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Ingredients");
        alertDialog.setMessage(getResources().getString(R.string.ingredients));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    private void initAndroidPay() {
        AndroidPayConfiguration payConfiguration =
                AndroidPayConfiguration.init(KeyKt.getPUBLISHABLE_KEY(), "USD");
        payConfiguration.setPhoneNumberRequired(false);
        payConfiguration.setShippingAddressRequired(true);
    }

    private void apiCalls() {
        Call<ResponseBody> callCookAvailable = cookieAPI.isCookAvailable();
        callCookAvailable.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    cookiesAvailable = response.body().string().equals("True");
                } catch (IOException e) {
                    cookiesAvailable = false;
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                cookiesAvailable = false;
            }
        });

        if (CookieIO.getCustomerId(this) == null) {      //create a new customer only if there isn't one already
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

    /**
     * The purpose of this function is to get the location if:
     * 1. Permissions are available.
     * 2. Location is turned on
     */
    private void setupLocation() {
        if (!myLocation.isLocationPermAvailable(this)) {
            getCookiesButton.setText("Turn location on!");
            myLocation.turnLocationPermOn(this, false);
        } else if (!myLocation.isLocationOn(this)) {
            getCookiesButton.setText("Turn location on!");
        }
        else if(!obtainedLocation) {
            SimpleDistance mySimpleDistance = new SimpleDistance();
            myLocation.getDistanceInMeters(this, new SimpleLocation(CDC_LAT, CDC_LONG), mySimpleDistance);
            (new onLocationAcquired()).execute(mySimpleDistance, this);
        }
    }

    private class onLocationAcquired extends AsyncTask<Object, Void, Void> {
        private SimpleDistance dist;
        private onLocationAcquired onALocationAcquired;
        private CookieCrowdSourceMainActivity cookieCrowdSourceMainActivity;

        //This preExecute will start a timer to shut off this onLocationAcquired
        @Override
        protected void onPreExecute() {
            onALocationAcquired = this;

            new CountDownTimer(timeBeforeLocationTimeout, 7000) {
                public void onTick(long millisUntilFinished) {
                    // You can monitor the progress here as well by changing the onTick() time
                }
                public void onFinish() {
                    // stop async task if not in progress
                    if (onALocationAcquired.getStatus() == Status.RUNNING) {
                        onALocationAcquired.cancel(true);
                        // Add any specific task you wish to do as your extended class variable works here as well.
                    }
                }
            }.start();
        }

        @Override
        protected Void doInBackground(Object... params) {
            dist = (SimpleDistance)params[0];
            cookieCrowdSourceMainActivity = (CookieCrowdSourceMainActivity)params[1];

            //spinlock this thread
            while (!myLocation.isLocationInitialized() && !isCancelled()) {
                //wait
            }

            if(isCancelled()){//This will only be true if a timeout occurred after 30 seconds on location
                outsideLocation();
                myLocation.stopLoadingLocation();
            }

            return null; //Return is necessary to explicitly notify that the doInBackground is done.
        }

        @Override
        protected void onPostExecute(Void params) {
            obtainedLocation = true;
            int distance = (int) Math.ceil(dist.getDistance(DistTypeEnum.Miles));
//            myButton.setText("Distance: " + distance);

            apiCalls();


            //Do cookie logic!
            if(distance >= 400 && !cookiesAvailable){
                outsideLocationAndNoCookies();
            }
            else if(distance >= 400 && cookiesAvailable){
                outsideLocation();
            }else if(distance <= 359 && !cookiesAvailable){
                noCookies();
            }
            else{
                haveCookies();
            }

        }

        private void outsideLocationAndNoCookies(){
            getCookiesButton.setText("Why can't I get cookies?");
            getCookiesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                    alertDialog.setTitle("Wrong Location and No Cookies");
                    alertDialog.setMessage("There are no cooks that are making cookies currently. Try again in the evening from 5pm to 9pm. There is more chance that we will be making cookies then! You also must be in a location that is in a 5 mile radius around the Greene to be able to order cookies. Thank you for your patience while we are getting this new business idea up and running!");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            });
        }

        private void outsideLocation(){
            getCookiesButton.setText("Why can't I get cookies?");
            getCookiesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                    alertDialog.setTitle("Wrong Location");
                    alertDialog.setMessage("You must be in a location that is in a 5 mile radius from the Greene for you to be able to order cookies. We will hopefully be coming to a location closer to you soon! Thank you for your patience while we are getting this new business idea up and running!");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            });
        }

        private void noCookies(){
            getCookiesButton.setText("Why can't I get cookies?");
            getCookiesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                    alertDialog.setTitle("No Cookies");
                    alertDialog.setMessage("There are no cooks that are making cookies currently. Try again in the evening from 5pm to 9pm. There is more chance that we will be making cookies then! Thank you for your patience while we are getting this new business idea up and running!");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            });
        }

        private void haveCookies(){
            getCookiesButton.setText("Get Cookies!");
            getCookiesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CartManager cartManager = new CartManager();
                    cartManager.addLineItem(StoreUtils.getEmojiByUnicode(0x1F36A), (double) 1, 1000);
                    try {
                        Cart cart = cartManager.buildCart();
                        Intent paymentLaunchIntent = PaymentActivity.createIntent(cookieCrowdSourceMainActivity, cart);
                        startActivity(paymentLaunchIntent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
