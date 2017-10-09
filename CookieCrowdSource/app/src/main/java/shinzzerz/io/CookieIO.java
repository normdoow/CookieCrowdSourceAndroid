package shinzzerz.io;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by noahbragg on 10/9/17.
 */

public class CookieIO {

    private static String PREF = "pref";
    private static String CUSTOMER_ID = "customer_id";

    public static String getCustomerId(Context context) {
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences(PREF, context.MODE_PRIVATE);
        return pref.getString(CUSTOMER_ID, null);
    }

    public static void setCustomerId(Context context, String id) {
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences(PREF, context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(CUSTOMER_ID, id);
        editor.commit();
    }
}
