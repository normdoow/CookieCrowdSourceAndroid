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
    @POST("charge")
    Observable<Void> createQueryCharge(
            @Field("amount") long amount,
            @Field("source") String source,
            @Field("customer_id") String customerId,
            @Field("email") String email);
}
