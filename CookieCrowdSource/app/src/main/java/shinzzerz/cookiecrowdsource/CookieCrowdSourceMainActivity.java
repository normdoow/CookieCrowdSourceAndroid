package shinzzerz.cookiecrowdsource;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import shinzzerz.restapi.CookieAPI;

/**
 * Created by administratorz on 9/2/2017.
 */

public class CookieCrowdSourceMainActivity extends AppCompatActivity{

    CookieAPI cookieAPI;

    @Override
    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        //eventually do some application specific loading
        //depending on app & user state

        Intent locationIntent = new Intent(this, CookieCrowdSourceLocationActivity.class);
        startActivity(locationIntent);

        //init the Retrofit instance for rest api
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CookieAPI.BASE_URL)
                .build();

        cookieAPI = retrofit.create(CookieAPI.class);

        apiCalls();
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

        Call<ResponseBody> callCreateCustomer = cookieAPI.createCustomer();
        callCreateCustomer.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                String customerId = "";
                try {
                    customerId = response.body().string();
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
