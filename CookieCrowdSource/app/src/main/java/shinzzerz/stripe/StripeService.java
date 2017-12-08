package shinzzerz.stripe;

import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import rx.Observable;

/**
 * The {@link retrofit2.Retrofit} interface that creates our API service.
 */
public interface StripeService {

    // For simplicity, we have URL encoded our body data, but your code will likely
    // want a model class send up as JSON
    @FormUrlEncoded
    @POST("charge_v2")
    Observable<Void> createQueryCharge(
            @Field("amount") long amount,
            @Field("source") String source,
            @Field("customer_id") String customerId,
            @Field("baker_email") String bakerEmail,
            @Field("email") String email);

    @FormUrlEncoded
    @POST("update_customer_address")
    Observable<Void> updateCustomerAddress(
            @Field("name") String name,
            @Field("email") String email,
            @Field("phone") String phone,
            @Field("city") String city,
            @Field("state") String state,
            @Field("customer_id") String customerId,
            @Field("line_1") String line1,
            @Field("line_2") String line2,
            @Field("postal_code") String postalCode);
}
