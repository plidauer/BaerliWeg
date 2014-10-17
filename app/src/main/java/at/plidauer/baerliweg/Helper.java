package at.plidauer.baerliweg;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Philipp on 14.10.14.
 */
public class Helper {

    private static final String SERVER_API_KEY = "AIzaSyDuar7phKALwWzJSPFUuuDqTNkogoBIbA0";
    private static final String TAG = "BaerliWeg";
    private static final String PROPERTY_APP_VERSION = "appVersion";




    protected static class BaerliTransmitException extends RuntimeException {
        private Exception innerException;
        private int returnType;
        public BaerliTransmitException(String message) {
            super();
            this.innerException = new Exception(message);
        }
        public BaerliTransmitException(int returnType) {
            super();
            this.returnType = returnType;
        }
        public BaerliTransmitException(Exception innerException) {
            super();
            this.innerException = innerException;
        }
        @Override
        public String toString() {
            if (innerException != null)
                return innerException.toString();
            else if (returnType != 0)
                return this.getClass().getSimpleName() + ": Return Type = " + returnType;
            else
                return super.toString();
        }
        @Override
        public void printStackTrace() {
            if (innerException != null)
                innerException.printStackTrace();
            else
                super.printStackTrace();
        }
    }


    protected static void transmit(final JSONObject body, final JSONArray regids) throws BaerliTransmitException {
        AsyncTask sendTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                HttpsURLConnection conn = null;
                try {
                    JSONObject json = new JSONObject();
                    json.put("registration_ids", regids);
                    json.put("delay_while_idle", false);
                    json.put("collapse_key", "1");

                    json.put("data", body);

                    byte[] bytes = json.toString().getBytes();

                    URL url = new URL("https://android.googleapis.com/gcm/send");
                    conn = (HttpsURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setFixedLengthStreamingMode(bytes.length);
                    conn.setRequestMethod("POST");

                    conn.addRequestProperty("Content-Type", "application/json");
                    conn.addRequestProperty("Authorization", "key=" + SERVER_API_KEY);


                    Log.i(TAG, "Posting to GCM Server!");


                    OutputStream out = conn.getOutputStream();
                    out.write(bytes);
                    out.close();


                    int status = conn.getResponseCode();
                    if (status != 200 && status != 201 && status != 202) {
                        if (status >= 400 && status <= 599) {
                            Log.e(TAG, conn.getResponseMessage() + " " + status);
                            throw new BaerliTransmitException(status);
                        } else {
                            Log.w(TAG, conn.getResponseMessage() + " " + status);
                        }
                    } else {
                        Log.i(TAG, "Message sent successfully.");
                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    if (conn != null)
                        conn.disconnect();
                }
                return null;
            }
        };

        sendTask.execute(null, null, null);

    }


    protected static String getStoredRegistrationId(Context context, String propertyTag) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(propertyTag, "");
        if (registrationId.isEmpty())
            return "";

        /*int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentAppVersion = getAppVersion(context);
        if (registeredVersion != currentAppVersion)
            return "";*/
        return registrationId;
    }

    protected static void storeRegistrationId(String regid, Context context, String propertyRegId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(propertyRegId, regid);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    private static SharedPreferences getGCMPreferences(Context context) {
        return context.getSharedPreferences(MyActivity.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get Package name: " + e);
        }
    }
}
