package shinzzerz.cookiecrowdsource;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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

    CookieAPI cookieAPI;

    @BindView(R.id.get_cookies)
    Button getCookiesButton;

    @BindView(R.id.first_dozen_free_image)
    ImageView firstDozenImage;

    @Override
    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        //eventually do some application specific loading
        //depending on app & user state

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout mainActivity;
        mainActivity = (LinearLayout)getLayoutInflater().inflate(R.layout.main_layout, null);
        setContentView(mainActivity);

        ButterKnife.bind(this);

        initAndroidPay();

//        Intent locationIntent = new Intent(this, CookieCrowdSourceLocationActivity.class);
//        startActivity(locationIntent);

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
        if(CookieIO.hasBoughtCookies(this)) {
            firstDozenImage.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.get_cookies)
    public void onGetCookiesClicked(Button button) {
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

    @OnClick(R.id.ingredients_button)
    public void clickIngreditentsButton() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Ingredients");
        alertDialog.setMessage("Unsalted butter, sugar, brown sugar, eggs, flour, ground oats, semisweet chocolate chips, vanilla, salt, baking powder, baking soda");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }
}
