package shinzzerz.stripe;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.wallet.Cart;
import com.jakewharton.rxbinding.view.RxView;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;
import com.stripe.android.model.SourceParams;
import com.stripe.android.view.CardInputWidget;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.activity.StripeAndroidPayActivity;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Retrofit;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import shinzzerz.cookiecrowdsource.R;
import shinzzerz.io.CookieIO;

public class PaymentActivity extends AppCompatActivity {

    @BindView(R.id.email)
    EditText email;
    @BindView(R.id.name)
    EditText name;
    @BindView(R.id.phone)
    EditText phone;
    @BindView(R.id.city)
    EditText city;
    @BindView(R.id.line1)
    EditText line1;
    @BindView(R.id.line2)
    EditText line2;
    @BindView(R.id.postal_code)
    EditText postalCode;

    private CardInputWidget mCardInputWidget;
    private CompositeSubscription mCompositeSubscription;
    private ProgressDialogFragment mProgressDialogFragment;
    private Stripe mStripe;
    private Button mConfirmPaymentButton;

    public static Intent createIntent(@NonNull Context context, @NonNull Cart cart) {
        Intent intent = new Intent(context, PaymentActivity.class);
        intent.putExtra(StripeAndroidPayActivity.EXTRA_CART, cart);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        ButterKnife.bind(this);

        Bundle extras = getIntent().getExtras();

        mCompositeSubscription = new CompositeSubscription();

        mCardInputWidget = (CardInputWidget) findViewById(R.id.card_input_widget);
        mProgressDialogFragment = ProgressDialogFragment.newInstance(R.string.completing_purchase);

        mConfirmPaymentButton = (Button) findViewById(R.id.btn_purchase);

        email.setText(CookieIO.getEmail(this));
        name.setText(CookieIO.getName(this));
        phone.setText(CookieIO.getPhone(this));
        city.setText(CookieIO.getCity(this));
        line1.setText(CookieIO.getLine1(this));
        line2.setText(CookieIO.getLine2(this));
        postalCode.setText(CookieIO.getPostalCode(this));

        setupChangeListeners();

        RxView.clicks(mConfirmPaymentButton)
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        updateCustomerAddress();
                    }
                });

        mStripe = new Stripe(this);
    }

    /*
     * Cleaning up all Rx subscriptions in onDestroy.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCompositeSubscription != null) {
            mCompositeSubscription.unsubscribe();
            mCompositeSubscription = null;
        }
    }

    @Size(value = 4)
    private TextView[] getItemViews(View view) {
        TextView labelView = (TextView) view.findViewById(R.id.tv_cart_emoji);
        TextView quantityView = (TextView) view.findViewById(R.id.tv_cart_quantity);
        TextView unitPriceView = (TextView) view.findViewById(R.id.tv_cart_unit_price);
        TextView totalPriceView = (TextView) view.findViewById(R.id.tv_cart_total_price);
        TextView[] itemViews = { labelView, quantityView, unitPriceView, totalPriceView };
        return itemViews;
    }

    private void attemptPurchase() {
        Card card = mCardInputWidget.getCard();
        if (card == null) {
            displayError("Card Input Error");
            return;
        }
        dismissKeyboard();

        final SourceParams cardParams = SourceParams.createCardParams(card);
        Observable<Source> cardSourceObservable =
                Observable.fromCallable(new Callable<Source>() {
                    @Override
                    public Source call() throws Exception {
                        return mStripe.createSourceSynchronous(
                                cardParams,
                                AndroidPayConfiguration.getInstance().getPublicApiKey());
                    }
                });

        final FragmentManager fragmentManager = this.getSupportFragmentManager();
        mCompositeSubscription.add(cardSourceObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(
                        new Action0() {
                            @Override
                            public void call() {
                                mProgressDialogFragment.show(fragmentManager, "progress");
                            }
                        })
                .subscribe(
                        new Action1<Source>() {
                            @Override
                            public void call(Source source) {
                                proceedWithPurchaseIf3DSCheckIsNotNecessary(source);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                if (mProgressDialogFragment != null) {
                                    mProgressDialogFragment.dismiss();
                                }
                                displayError(throwable.getLocalizedMessage());
                            }
                        }));
    }

    private void updateCustomerAddress() {
        Retrofit retrofit = RetrofitFactory.getInstance();
        StripeService stripeService = retrofit.create(StripeService.class);
        String customerName = name.getText().toString();
        String customerEmail = email.getText().toString();
        String customerPhone = phone.getText().toString();

        Address address = new Address(city.getText().toString(), "USA", line1.getText().toString(), line2.getText().toString(), postalCode.getText().toString(), "OH");
        if(address.isValidAddress() && !customerEmail.isEmpty() && !customerName.isEmpty() && !customerPhone.isEmpty()) {
            Observable<Void> stripeResponse = stripeService.updateCustomerAddress(customerName, customerEmail, customerPhone,
                    address.getCity(), address.getState(), CookieIO.getCustomerId(this),
                    address.getLine1(), address.getLine2(), address.getPostalCode());
            mCompositeSubscription.add(stripeResponse
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            new Action1<Void>() {
                                @Override
                                public void call(Void aVoid) {
                                    attemptPurchase();
                                }
                            },
                            new Action1<Throwable>() {
                                @Override
                                public void call(Throwable throwable) {
                                    displayError(throwable.getLocalizedMessage());
                                }
                            }));
        } else {            //not a valid address
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Invalid Address Info");
            alertDialog.setMessage("You must fill out all of the Address info.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
    }

    private void proceedWithPurchaseIf3DSCheckIsNotNecessary(Source source) {
        if (source == null || !Source.CARD.equals(source.getType())) {
            displayError("Something went wrong - this should be rare");
            return;
        }

        SourceCardData cardData = (SourceCardData) source.getSourceTypeModel();
        if (SourceCardData.REQUIRED.equals(cardData.getThreeDSecureStatus())) {
            // In this case, you would need to ask the user to verify the purchase.
            // You can see an example of how to do this in the 3DS example application.
            // In stripe-android/example.
        } else {
            // If 3DS is not required, you can charge the source.
            completePurchase(source.getId());
        }
    }

    private void completePurchase(String sourceId) {
        Retrofit retrofit = RetrofitFactory.getInstance();
        StripeService stripeService = retrofit.create(StripeService.class);
        Long price = 1000L;

        if (price == null) {
            // This should be rare, and only occur if there is somehow a mix of currencies in
            // the CartManager (only possible if those are put in as LineItem objects manually).
            // If this is the case, you can put in a cart total price manually by calling
            // CartManager.setTotalPrice.
            return;
        }

        Observable<Void> stripeResponse = stripeService.createQueryCharge(price, sourceId, CookieIO.getCustomerId(this), email.getText().toString());
        final FragmentManager fragmentManager = getSupportFragmentManager();
        mCompositeSubscription.add(stripeResponse
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(
                        new Action0() {
                            @Override
                            public void call() {
                                if (mProgressDialogFragment != null &&
                                        !mProgressDialogFragment.isAdded())
                                    mProgressDialogFragment.show(fragmentManager, "progress");
                            }
                        })
                .doOnUnsubscribe(
                        new Action0() {
                            @Override
                            public void call() {
                                if (mProgressDialogFragment != null
                                        && mProgressDialogFragment.isVisible()) {
                                    mProgressDialogFragment.dismiss();
                                }
                            }
                        })
                .subscribe(
                        new Action1<Void>() {
                            @Override
                            public void call(Void aVoid) {
                                finishCharge();
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                displayError(throwable.getLocalizedMessage());
                            }
                        }));
    }

    private void displayError(String errorMessage) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage(errorMessage);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    private void finishCharge() {
        mProgressDialogFragment.dismiss();

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Thank You!");
        alertDialog.setMessage("Thank you for your order! You will receive a dozen \uD83C\uDF6As in 30 to 40 minutes!");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Yay!!!",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });
        alertDialog.show();
    }

    private void dismissKeyboard() {
        InputMethodManager inputManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(0, 0);
    }

    private void setupChangeListeners() {

        final Context context = this;

        email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                CookieIO.setEmail(context, s.toString());
            }
        });
        name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                CookieIO.setName(context, s.toString());
            }
        });
        phone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                CookieIO.setPhone(context, s.toString());
            }
        });
        city.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                CookieIO.setCity(context, s.toString());
            }
        });
        line1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                CookieIO.setLine1(context, s.toString());
            }
        });
        line2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                CookieIO.setLine2(context, s.toString());
            }
        });
        postalCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                CookieIO.setPostalCode(context, s.toString());
            }
        });
    }

}
