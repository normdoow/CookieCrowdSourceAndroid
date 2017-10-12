package shinzzerz.io;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by noahbragg on 10/9/17.
 */

public class CookieIO {

    private static String PREF = "pref";
    private static String CUSTOMER_ID = "customer_id";
    private static String EMAIL = "email";
    private static String NAME = "name";
    private static String PHONE = "phone";
    private static String CITY = "city";
    private static String LINE1 = "line1";
    private static String LINE2 = "line2";
    private static String POSTAL_CODE = "postal-code";

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

    public static String getEmail(Context context) {
        return getStringHelper(context, EMAIL);
    }

    public static void setEmail(Context context, String value) {
        setStringhelper(context, EMAIL, value);
    }

    public static String getName(Context context) {
        return getStringHelper(context, NAME);
    }

    public static void setName(Context context, String value) {
        setStringhelper(context, NAME, value);
    }

    public static String getPhone(Context context) {
        return getStringHelper(context, PHONE);
    }

    public static void setPhone(Context context, String value) {
        setStringhelper(context, PHONE, value);
    }

    public static String getCity(Context context) {
        return getStringHelper(context, CITY);
    }

    public static void setCity(Context context, String value) {
        setStringhelper(context, CITY, value);
    }

    public static String getLine1(Context context) {
        return getStringHelper(context, LINE1);
    }

    public static void setLine1(Context context, String value) {
        setStringhelper(context, LINE1, value);
    }

    public static String getLine2(Context context) {
        return getStringHelper(context, LINE2);
    }

    public static void setLine2(Context context, String value) {
        setStringhelper(context, LINE2, value);
    }

    public static String getPostalCode(Context context) {
        return getStringHelper(context, POSTAL_CODE);
    }

    public static void setPostalCode(Context context, String value) {
        setStringhelper(context, POSTAL_CODE, value);
    }

    private static String getStringHelper(Context context, String key) {
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences(PREF, context.MODE_PRIVATE);
        return pref.getString(key, "");
    }

    private static void setStringhelper(Context context, String key, String value) {
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences(PREF, context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value);
        editor.commit();
    }
}
