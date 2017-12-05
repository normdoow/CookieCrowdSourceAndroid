package shinzzerz.io;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by noahbragg on 12/4/17.
 */

public class BakerIO {

    private static String PREF = "pref";
    private static String BAKER_EMAIL = "baker-email";
    private static String AVAILABLE_TO_CUSTOMERS = "available_to_customers";

    public static Boolean isAvailableToCustomers(Context context) {
        return getBoolHelper(context, AVAILABLE_TO_CUSTOMERS);
    }

    public static void setAvailableToCustomers(Context context, boolean value) {
        setBoolHelper(context, AVAILABLE_TO_CUSTOMERS, value);
    }

    public static String getBakerEmail(Context context) {
        return getStringHelper(context, BAKER_EMAIL);
    }

    public static void setBakerEmail(Context context, String value) {
        setStringHelper(context, BAKER_EMAIL, value);
    }

    private static String getStringHelper(Context context, String key) {
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences(PREF, context.MODE_PRIVATE);
        return pref.getString(key, "");
    }

    private static void setStringHelper(Context context, String key, String value) {
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences(PREF, context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value);
        editor.commit();
    }

    private static boolean getBoolHelper(Context context, String key) {
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences(PREF, context.MODE_PRIVATE);
        return pref.getBoolean(key, false);
    }

    private static void setBoolHelper(Context context, String key, boolean value) {
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences(PREF, context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }
}
