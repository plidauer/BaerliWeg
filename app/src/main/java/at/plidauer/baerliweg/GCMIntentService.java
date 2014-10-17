package at.plidauer.baerliweg;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.location.LocationClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class GCMIntentService extends IntentService implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    LocationClient locationClient;


    public GCMIntentService() {
        super("GcmIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locationClient = new LocationClient(this, this, this);
        locationClient.connect();
    }

    @Override
    public void onDestroy() {
        locationClient.disconnect();
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        String messageType = gcm.getMessageType(intent);

        Log.i(MyActivity.TAG, "Received Message: " + messageType);

        if (!extras.isEmpty()) {
            String myAction = extras.getString("my_action");

            if (myAction.equals("STANDARD")) {

                int myStep = Integer.parseInt(extras.getString("my_step"));
                Location myLocation = new Location("reverseGeocoded");

                myLocation.setLatitude(Double.parseDouble(extras.getString("my_loc_lat")));
                myLocation.setLongitude(Double.parseDouble(extras.getString("my_loc_lon")));
                myLocation.setAccuracy(Float.parseFloat(extras.getString("my_loc_acc")));
                myLocation.setAltitude(Double.parseDouble(extras.getString("my_loc_alt")));
                myLocation.setTime(Long.parseLong(extras.getString("my_loc_tim")));
                myLocation.setBearing(Float.parseFloat(extras.getString("my_loc_ber")));

                Log.i(MyActivity.TAG, "Received step " + myStep + " with location " + myLocation.toString());

                if (myStep == 1) {
                    sendResponse(myLocation);
                } else if (myStep == 2) {
                    Log.i(MyActivity.TAG, "Received response!");
                } else {
                    throw new RuntimeException("Step is not 1 or 2!");
                }

            } else if (myAction.equals("TRANSMIT_REGID")) {
                String regid = extras.getString("my_regid");
                String regidOwner = extras.getString("my_regid_owner");
                Helper.storeRegistrationId(regid, this, regidOwner);
            }

        } else {
            throw new RuntimeException("No extras contained in intent!");
        }

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendResponse(final Location myLocation) {
        final Location currentLocation = locationClient.getLastLocation();

        try {
            JSONObject data = new JSONObject();
            data.put("my_action", "STANDARD");
            data.put("my_loc_lat", myLocation.getLatitude());
            data.put("my_loc_lon", myLocation.getLongitude());
            data.put("my_loc_acc", myLocation.getAccuracy());
            data.put("my_loc_alt", myLocation.getAltitude());
            data.put("my_loc_tim", myLocation.getTime());
            data.put("my_loc_ber", myLocation.getBearing());
            data.put("your_loc_lat", currentLocation.getLatitude());
            data.put("your_loc_lon", currentLocation.getLongitude());
            data.put("your_loc_acc", currentLocation.getAccuracy());
            data.put("your_loc_alt", currentLocation.getAltitude());
            data.put("your_loc_tim", currentLocation.getTime());
            data.put("your_loc_ber", currentLocation.getBearing());
            data.put("my_step", 2);

            //Helper.transmit(data, new JSONArray().put((MyActivity.regid.equals(MyActivity.regidPhilipp) ? MyActivity.regidEsther : MyActivity.regidPhilipp)));
            Helper.transmit(data, new JSONArray().put(MyActivity.regid));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("TAG", "Location Services connected!");
    }

    @Override
    public void onDisconnected() {
        Log.d("TAG", "Location Services disconnected!");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.w("TAG", "Location Services connection failed!");
        throw new RuntimeException("" + connectionResult.getErrorCode());
    }
}
