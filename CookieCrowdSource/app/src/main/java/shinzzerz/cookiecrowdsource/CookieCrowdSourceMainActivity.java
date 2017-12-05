package shinzzerz.cookiecrowdsource;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.wallet.Cart;
import com.pusher.android.PusherAndroid;
import com.pusher.android.notifications.PushNotificationRegistration;
import com.pusher.android.notifications.interests.InterestSubscriptionChangeListener;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.utils.CartManager;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import shinzzerz.KeyKt;
import shinzzerz.io.BakerIO;
import shinzzerz.io.CookieIO;
import shinzzerz.location.DistTypeEnum;
import shinzzerz.location.GetCurrentLocation;
import shinzzerz.location.SimpleDistance;
import shinzzerz.location.SimpleLocation;
import shinzzerz.restapi.CookieAPI;
import shinzzerz.stripe.PaymentActivity;
import shinzzerz.stripe.StoreUtils;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONObject;

/**
 * Created by administratorz on 9/2/2017.
 */
public class CookieCrowdSourceMainActivity extends AppCompatActivity {
    private static final double CDC_LAT = 39.691483;
    private static final double CDC_LONG = -84.101717;
    private static final double ISAIAH_LAT = 39.673647;
    private static final double ISAIAH_LONG = -83.977037;
    private static final double DISTANCE_RADIUS_FROM_CDC = 3.5;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 88;

    private GetCurrentLocation myLocation = new GetCurrentLocation();
    private boolean obtainedLocation = false;
    private boolean noahAvailable = false;
    private boolean isaiahAvailable = false;
    private Context context;
    private Subscription intervalSubscription;
    private Subscription locationSubscription;

    private MixpanelAPI mixpanel;

    CookieAPI cookieAPI;
    @BindView(R.id.main_layout_get_cookies_button)
    Button getCookiesButton;

    @BindView(R.id.first_dozen_free_image)
    ImageView firstDozenImage;


    @BindView(R.id.pacman_loading)
    AVLoadingIndicatorView pacmanLoader;

    @BindView(R.id.pacman_container)
    RelativeLayout pacmanContainer;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    @BindView(R.id.left_drawer_container)
    LinearLayout drawerContainer;

    @BindView(R.id.new_email_edit_text)
    EditText newBakerEmailText;

    @BindView(R.id.login_container)
    LinearLayout loginContainer;
    @BindView(R.id.baker_dashbaord_container)
    LinearLayout bakerDashboardContainer;
    @BindView(R.id.availability_text)
    TextView availabilityText;
    @BindView(R.id.available_switch)
    Switch availableSwitch;

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

        String projectToken = "";
        mixpanel = MixpanelAPI.getInstance(this, projectToken);

        ButterKnife.bind(this);
        initAndroidPay();

        //init the Retrofit instance for rest api
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CookieAPI.BASE_URL)
                .build();

        cookieAPI = retrofit.create(CookieAPI.class);

        //sign up for notifications
        if (playServicesAvailable()) {
            PusherAndroid pusher = new PusherAndroid(KeyKt.getPUSHER_APP_KEY());
            PushNotificationRegistration nativePusher = pusher.nativePusher();

            // pulled from your google-services.json
            String defaultSenderId = getString(R.string.gcm_defaultSenderId);
            try {
                nativePusher.registerGCM(this, defaultSenderId);
            } catch(Exception e) {
                System.out.println(e);
            }
            nativePusher.subscribe("cook_available", new InterestSubscriptionChangeListener() {
                @Override
                public void onSubscriptionChangeSucceeded() {
                    System.out.println("Success!");
                }

                @Override
                public void onSubscriptionChangeFailed(int statusCode, String response) {
                    System.out.println(":(: received " + statusCode + " with" + response);
                }
            });

            // Ready to subscribe to topics!
        }

        setupDrawer();
    }

    @Override
    public void onResume() {
        super.onResume();

        //run async task that checks for location and api calls every 20 seconds to update the view
        intervalSubscription = Observable.interval(20, TimeUnit.SECONDS)
                .startWith((long) 0)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        apiCalls();
                        setupLocation();
                    }
                });
        if (CookieIO.hasBoughtCookies(this)) {
            firstDozenImage.setVisibility(View.GONE);
        }
    }

    private void setupDrawer() {
        if(BakerIO.getBakerEmail(context).equals("")) {
            loginContainer.setVisibility(View.VISIBLE);
            bakerDashboardContainer.setVisibility(View.GONE);
        } else {
            loginContainer.setVisibility(View.GONE);
            bakerDashboardContainer.setVisibility(View.VISIBLE);
        }
        if(BakerIO.isAvailableToCustomers(context)) {
            availabilityText.setText("Available to Customers");
            availableSwitch.setChecked(true);
        } else {
            availabilityText.setText("Not Available to Customers");
            availableSwitch.setChecked(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        myLocation.stopLoadingLocation();
        if (intervalSubscription != null && !intervalSubscription.isUnsubscribed()) {
            intervalSubscription.unsubscribe();
        }
        if(locationSubscription != null && !locationSubscription.isUnsubscribed()){
            locationSubscription.unsubscribe();
        }
        hideLoadingPacMan();
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

    @OnClick(R.id.hamburger_icon)
    public void clickHamburgerButton() {
        drawer.openDrawer(drawerContainer);
    }

    @OnClick(R.id.login_button)
    public void clickLoginBaker() {
        showNotification("Invalid Login", "The Email or password is incorrect.");
        BakerIO.setBakerEmail(context, "noahbragg@cedarville.edu");
        setupDrawer();
    }

    @OnClick(R.id.send_email_button)
    public void clickSendEmailButton() {
        String newEmail = newBakerEmailText.getText().toString();
        Call<ResponseBody> callNewBaker = cookieAPI.sendNewBakerEmail(newEmail);
        if(!newEmail.equals("")) {
            callNewBaker.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        if (response.body().string().equals("success sending email")) {
                            showNotification("Success", "We now have your email and will get in contact with you about making Crowd Cookies!");
                        } else {
                            showNotification("Error", "We weren't able to receive your email. Try checking your internet connection.");
                        }
                    } catch (Exception e) {
                        showNotification("Error", "We weren't able to receive your email. Try checking your internet connection.");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    showNotification("Error", "We weren't able to receive your email. Try checking your internet connection.");
                }
            });
        } else {
            showNotification("No Email", "Please provide an email for us to be able to contact you.");
        }
    }

    @OnClick(R.id.available_switch)
    public void bakerAvailableChange(Switch bakerSwitch) {
        String message = "You are turning ON your availabilty to bake cookies. Are you sure you are ready to fulfill orders?";
        if (!bakerSwitch.isChecked()) {
            message = "Are you sure that you want to turn OFF your availabilty to bake cookies?";
        }
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Are You Sure?");
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES", (DialogInterface dialog, int which) -> {
            dialog.dismiss();
            BakerIO.setAvailableToCustomers(context, bakerSwitch.isChecked());
            setupDrawer();
        });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "NO", (DialogInterface dialog, int which) -> {
            bakerSwitch.setChecked(!bakerSwitch.isChecked());
            dialog.dismiss();
        });
        alertDialog.show();
    }

    @OnClick(R.id.sign_out_button)
    public void signOutButton() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Are You Sure?");
        alertDialog.setMessage("Signing out will automatically turn off your availability for baking.");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES", (DialogInterface dialog, int which) -> {
            dialog.dismiss();
            BakerIO.setAvailableToCustomers(context, false);
            BakerIO.setBakerEmail(context, "");
            setupDrawer();
        });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "NO", (DialogInterface dialog, int which) -> {
            dialog.dismiss();
        });
        alertDialog.show();
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
     * 3. Then find the location based off of an Observer
     */
    private void setupLocation() {
        if (!myLocation.isLocationPermAvailable(this)) {
            getCookiesButton.setText("Turn location on!");
            myLocation.turnLocationPermOn(this, false);
        } else if (!myLocation.isLocationOn(this)) {
            getCookiesButton.setText("Turn location on!");
            myLocation.turnLocationOn(this, true);
        } else if (!obtainedLocation) {
            showLoadingPacMan();
            SimpleDistance distanceAwayFromCdc = new SimpleDistance();
            SimpleDistance distanceAwayFromIsaiah = new SimpleDistance();

            Observable<SimpleDistance> observable = myLocation.getDistanceInMeters(this, new SimpleLocation(CDC_LAT, CDC_LONG), distanceAwayFromCdc);

            Observable<SimpleDistance> observableIsaiah = myLocation.getDistanceInMeters(this, new SimpleLocation(ISAIAH_LAT, ISAIAH_LONG), distanceAwayFromIsaiah);

            locationSubscription = Observable.concat(observable, observableIsaiah)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((Void) -> updateButtonBasedOnCookieLogic(distanceAwayFromCdc, distanceAwayFromIsaiah));
        }
    }

    private void updateButtonBasedOnCookieLogic(SimpleDistance dist, SimpleDistance dist2) {
        int distNoah = (int) Math.ceil(dist.getDistance(DistTypeEnum.Miles));
        int distIsaiah = (int) Math.ceil(dist2.getDistance(DistTypeEnum.Miles));

        hideLoadingPacMan();

        boolean isNoahRightLocation = distNoah <= DISTANCE_RADIUS_FROM_CDC;
        boolean isIsaiahRightLocation = distIsaiah <= DISTANCE_RADIUS_FROM_CDC;

        //Do cookie logic!
        if (!noahAvailable && !isNoahRightLocation && !isaiahAvailable && !isIsaiahRightLocation) {
            outsideLocationAndNoCookies();
        } else if ((!noahAvailable && isNoahRightLocation || !isaiahAvailable && isIsaiahRightLocation) && !(isIsaiahRightLocation && isNoahRightLocation)) {
            noCookies();
        } else if (!isNoahRightLocation && !isIsaiahRightLocation) {
            outsideLocation();
        } else {
            haveCookies();
        }
    }

    private void outsideLocationAndNoCookies() {
        getCookiesButton.setText("Why can't I get cookies?");
        getCookiesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMixPanelAnaltyics("cookeis_unavailable_ Wrong Location and no cookies");
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

    private void outsideLocation() {
        getCookiesButton.setText("Why can't I get cookies?");
        getCookiesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMixPanelAnaltyics("cookeis_unavailable_ Wrong Location");
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

    private void noCookies() {
        getCookiesButton.setText("Why can't I get cookies?");
        getCookiesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMixPanelAnaltyics("cookeis_unavailable_ no cookies");
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

    private void haveCookies() {
        getCookiesButton.setText("Get Cookies!");
        getCookiesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMixPanelAnaltyics("Selected Get Hot Cookies");
                CartManager cartManager = new CartManager();
                cartManager.addLineItem(StoreUtils.getEmojiByUnicode(0x1F36A), (double) 1, 1200);
                try {
                    Cart cart = cartManager.buildCart();
                    Intent paymentLaunchIntent = PaymentActivity.createIntent(context, cart);
                    startActivity(paymentLaunchIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showLoadingPacMan() {
        pacmanContainer.setVisibility(View.VISIBLE);
        getCookiesButton.setVisibility(View.GONE);
        pacmanLoader.show();
    }

    private void hideLoadingPacMan() {
        pacmanLoader.hide();
        pacmanContainer.setVisibility(View.GONE);
        getCookiesButton.setVisibility(View.VISIBLE);
    }

    private void initAndroidPay() {
        AndroidPayConfiguration payConfiguration =
                AndroidPayConfiguration.init(KeyKt.getSTRIPE_PUBLISHABLE_KEY(), "USD");
        payConfiguration.setPhoneNumberRequired(false);
        payConfiguration.setShippingAddressRequired(true);
    }

    /**
     * This function starts the calls to determine if a cookie is available.
     */
    private void apiCalls() {
        Call<ResponseBody> callCookAvailable = cookieAPI.isCookAvailable();
        callCookAvailable.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    noahAvailable = response.body().string().equals("True");
                } catch (IOException e) {
                    noahAvailable = false;
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                noahAvailable = false;
            }
        });

        Call<ResponseBody> callIsaiahAvailable = cookieAPI.isIsaiahAvailable();
        callIsaiahAvailable.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    isaiahAvailable = response.body().string().equals("True");
                } catch (IOException e) {
                    isaiahAvailable = false;
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                isaiahAvailable = false;
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

    //needed to check for push notifications
    private boolean playServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                finish();
            }
            return false;
        }
        return true;
    }

    private void showNotification(String title, String message) {

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
//                        finish();
                    }
                });
        alertDialog.show();
    }

    private void sendMixPanelAnaltyics(String message) {
//        try {
            JSONObject props = new JSONObject();
            mixpanel.track(message, props);
//        } catch (JSONException e) {
//            Log.e("MYAPP", "Unable to add properties to JSONObject", e);
//        }
    }
}
