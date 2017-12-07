package shinzzerz.restapi;


import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by noahbragg on 10/1/17.
 */

public interface CookieAPI {

//    public static String BASE_URL = "http://192.168.0.7:5000";
    public static String BASE_URL = "http://noahbragg.pythonanywhere.com";

//    @GET("group/{id}/users")
//    Call<List<User>> groupList(@Path("id") int groupId);
//
//    @GET("group/{id}/users")
//    Call<List<User>> groupList(@Path("id") int groupId, @Query("sort") String sort);
//
//    @POST("users/new")
//    Call<String> createUser(@Body String user);

    @GET("is_isaiah_available")
    Call<ResponseBody> isIsaiahAvailable();

    @GET("send_new_baker_email")
    Call<ResponseBody> sendNewBakerEmail(@Query("email") String address);

    @GET("send_rating_email")
    Call<ResponseBody> sendRating(@Query("rating") String rating, @Query("comments") String comments,
                                         @Query("isWarm") String isWarm, @Query("isRecommend") String isRecommend);

    @GET("change_baker_availability")
    Call<ResponseBody> changeBakerAvailability(@Query("baker_email") String bakerEmail, @Query("is_available") String isAvailableText);

    @GET("login_baker")
    Call<ResponseBody> loginBaker(@Query("pw") String pw, @Query("email") String email);

    @GET("cook_available")
    Call<ResponseBody> getCookAvailable(@Query("lat") String lat, @Query("long") String lon);

    @GET("create_customer")
    Call<ResponseBody> createCustomer();

}
