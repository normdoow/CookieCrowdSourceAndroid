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
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 88;

    private GetCurrentLocation myLocation = new GetCurrentLocation();
    private boolean obtainedLocation = false;
    private Context context;
    private Subscription intervalSubscription;
    private Subscription locationSubscription;
    private String whyNoCookiesText;

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
    @BindView(R.id.password_edit_text)
    EditText passwordEditText;
    @BindView(R.id.email_edit_text)
    EditText emailEditText;

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

        String projectToken = "2f1bf0154e0e5c93761c28e0060cc30b";
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
        intervalSubscription = Observable.interval(15, TimeUnit.SECONDS)
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
        Call<ResponseBody> callLoginBaker = cookieAPI.loginBaker(passwordEditText.getText().toString(), emailEditText.getText().toString());
        callLoginBaker.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if(response.body().string().equals("success")) {
                        BakerIO.setBakerEmail(context, emailEditText.getText().toString());
                        setupDrawer();
                    } else {
                        showNotification("Invalid Login", "The Email or password is incorrect.");
                    }
                } catch (IOException e) {
                    showNotification("Invalid Login", "The Email or password is incorrect.");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showNotification("Invalid Login", "The Email or password is incorrect.");
            }
        });
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

            Call<ResponseBody> callChangeBaker = cookieAPI.changeBakerAvailability(BakerIO.getBakerEmail(context), bakerSwitch.isChecked()? "Yes" : "No");
            callChangeBaker.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        if(response.body().string().equals("success")) {
                            dialog.dismiss();
                            BakerIO.setAvailableToCustomers(context, bakerSwitch.isChecked());
                            setupDrawer();
                        } else {
                            bakerSwitch.setChecked(!bakerSwitch.isChecked());
                            dialog.dismiss();
                            showNotification("Failed", "Failed to change your availability");
                        }
                    } catch (IOException e) {
                        bakerSwitch.setChecked(!bakerSwitch.isChecked());
                        dialog.dismiss();
                        showNotification("Failed", "Failed to change your availability");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    bakerSwitch.setChecked(!bakerSwitch.isChecked());
                    dialog.dismiss();
                    showNotification("Failed", "Failed to change your availability");
                }
            });
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
            Call<ResponseBody> callChangeBaker = cookieAPI.changeBakerAvailability(BakerIO.getBakerEmail(context), "No");
            callChangeBaker.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        if(response.body().string().equals("success")) {
                            dialog.dismiss();
                            BakerIO.setAvailableToCustomers(context, false);
                            BakerIO.setBakerEmail(context, "");
                            setupDrawer();
                        } else {
                            dialog.dismiss();
                            showNotification("Failed", "Failed to change your availability");
                        }
                    } catch (IOException e) {
                        dialog.dismiss();
                        showNotification("Failed", "Failed to change your availability");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    dialog.dismiss();
                    showNotification("Failed", "Failed to change your availability");
                }
            });
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

            locationSubscription = myLocation.getDistanceInMeters(this, new SimpleLocation(), new SimpleDistance())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe( location -> {
                        Call<ResponseBody> callCookAvailable = cookieAPI.getCookAvailable(location.getLat() + "", location.getLong() + "");
                        callCookAvailable.enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                try {
                                    String value = response.body().string();
                                    if (value.contains("@")) {
                                        whyNoCookiesText = "";
                                        CookieIO.setMyBakerEmail(context, value);
                                    } else {
                                        whyNoCookiesText = value;
                                        CookieIO.setMyBakerEmail(context, "");
                                    }

                                } catch (IOException e) {
                                    whyNoCookiesText = "Failed to Connect.";
                                    CookieIO.setMyBakerEmail(context, "");
                                }
                                updateGetCookiesButton();
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                whyNoCookiesText = "Failed to Connect.";
                                CookieIO.setMyBakerEmail(context, "");
                                updateGetCookiesButton();
                            }
                        });
                    });
        }
    }

    private void updateGetCookiesButton() {
        hideLoadingPacMan();

        if(whyNoCookiesText != "") {
            whyNoCookiesButton();
        } else {
            haveCookies();
        }
    }

    private void whyNoCookiesButton() {
        getCookiesButton.setText("Why can't I get cookies?");
        getCookiesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMixPanelAnaltyics(whyNoCookiesText);
                AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                alertDialog.setTitle("Why can't I get cookies?");
                alertDialog.setMessage(whyNoCookiesText);
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
