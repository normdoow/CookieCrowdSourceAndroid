package shinzzerz.cookiecrowdsource;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.gms.wallet.Cart;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.utils.CartManager;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.IOException;
import java.util.concurrent.Callable;
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
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
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
    private Subscription mySubscription;

    @BindView(R.id.pacman_loading)
    AVLoadingIndicatorView pacmanLoader;

    @BindView(R.id.pacman_container)
    RelativeLayout pacmanContainer;

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

        //run async task that checks for location and api calls every 20 seconds to udpate the view
        mySubscription = Observable.interval(20, TimeUnit.SECONDS).startWith((long) 0)
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

    @Override
    public void onPause() {
        super.onPause();
        myLocation.stopLoadingLocation();
        if (mySubscription != null && !mySubscription.isUnsubscribed()) {
            mySubscription.unsubscribe();
        }
        obtainedLocation = false;
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
            myLocation.turnLocationOn(this, true);
        } else if (!obtainedLocation) {
            final SimpleDistance mySimpleDistance = new SimpleDistance();
            myLocation.getDistanceInMeters(this, new SimpleLocation(CDC_LAT, CDC_LONG), mySimpleDistance);

            Observable.fromCallable(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    while (!myLocation.isLocationInitialized()) {

                    }
                    return null;
                }
            }).subscribe(new Action1<Void>() {
                @Override
                public void call(Void aVoid) {
                    updateButtonBasedOnCookieLogic(mySimpleDistance);
                }
            });
        }
        pacmanLoader.show();
    }

    private void updateButtonBasedOnCookieLogic(SimpleDistance dist) {
        int distance = (int) Math.ceil(dist.getDistance(DistTypeEnum.Miles));

        //Do cookie logic!
        if (distance >= 400 && !cookiesAvailable) {
            outsideLocationAndNoCookies();
        } else if (distance >= 400 && cookiesAvailable) {
            outsideLocation();
        } else if (distance <= 359 && !cookiesAvailable) {
            noCookies();
        } else {
            haveCookies();
        }
    }

    private void outsideLocationAndNoCookies() {
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

    private void outsideLocation() {
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

    private void noCookies() {
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

    private void haveCookies() {
        getCookiesButton.setText("Get Cookies!");
        getCookiesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CartManager cartManager = new CartManager();
                cartManager.addLineItem(StoreUtils.getEmojiByUnicode(0x1F36A), (double) 1, 1000);
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
}
