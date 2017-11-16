package shinzzerz.restapi;


import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

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

    @GET("is_cook_available")
    Call<ResponseBody> isCookAvailable();

    @GET("is_isaiah_available")
    Call<ResponseBody> isIsaiahAvailable();

    @GET("create_customer")
    Call<ResponseBody> createCustomer();

}
