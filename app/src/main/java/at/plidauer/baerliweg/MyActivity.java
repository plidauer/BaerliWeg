package at.plidauer.baerliweg;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.location.LocationClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;


public class MyActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_REG_ID_ESTHER = "registration_id_esther";
    private static final String PROPERTY_REG_ID_PHILIPP = "registration_id_philipp";
    public static final String TAG = "BaerliWeg";
    private static final String SENDER_ID = "725840921883";


    Button button = null, again = null;
    ProgressBar progressBar = null;
    View question = null, answer = null;
    TextView distance = null, message = null;
    Context context;
    AtomicInteger msgId = new AtomicInteger();
    GoogleCloudMessaging gcm;
    SharedPreferences prefs;
    LocationClient locationClient;
    BroadcastReceiver receiver = null;
    float dist;

    final int STATE_INIT = 0;
    final int STATE_WAIT = 1;
    final int STATE_ANSWER = 2;

    int state = STATE_INIT;

    protected static String regid;
    protected static String regidPhilipp = "APA91bFlmb2KmXZW9XfU1b2vEu6vUxWcuxfWCZeJtt0aygfZJ_75s7cC3nXcUddd65ROpDT73MmcV_l14siFC-dFmN4c9vwdRZWnB05r5rSFI0ZWhu9j_O2846Zt0A40IqizOrzywTTkocElj0V86QyNdhMolAZhDQ";
    protected static String regidEsther = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        question = findViewById(R.id.question);
        answer = findViewById(R.id.answer);
        distance = (TextView) findViewById(R.id.distance);
        message = (TextView) findViewById(R.id.message);
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setState(STATE_WAIT);
                sendNotification();
            }
        });
        again = (Button) findViewById(R.id.again);
        again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setState(STATE_WAIT);
                sendNotification();
            }
        });


        context = getApplicationContext();
        locationClient = new LocationClient(this, this, this);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Location location1, location2;
                location1 = intent.getExtras().getParcelable("location1");
                location2 = intent.getExtras().getParcelable("location2");
                float dist = location1.distanceTo(location2);
                Log.i(TAG, "Distance between two phones: " + dist);

                MyActivity.this.dist = dist;

                if (dist < 100)
                    distance.setText("");
                else if (dist < 1000)
                    distance.setText("" + Math.round(dist) + " m");
                else
                    distance.setText("" + Math.round(dist/1000) + " km");

                if (dist < 100)
                    message.setText("ach Esther, er ist doch eh bei dir :)");
                else if (dist < 300)
                    message.setText("gleich um die Ecke :)");
                else if (dist < 1000)
                    message.setText("am Weg zu dir!");
                else if (dist < 5000)
                    message.setText("er mag auf einen Eiskaffee gehen, wetten?");
                else if (dist < 20000)
                    message.setText("nimmst ihm was gutes mit? :)");
                else if (dist < 100000)
                    message.setText("");
                else if (dist < 300000)
                    message.setText("in Wien und Oberösterreich :)");
                else if (dist < 1000000)
                    message.setText("ganz schön weit :-/");
                else
                    message.setText("Weltmeere dazwischen :'( schreib ihm, dass es dir gut geht");

                setState(STATE_ANSWER);
            }
        };
        this.registerReceiver(receiver, new IntentFilter("at.plidauer.baerliweg.FINISHED"));

        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = Helper.getStoredRegistrationId(context, PROPERTY_REG_ID);
            regidEsther = Helper.getStoredRegistrationId(context, PROPERTY_REG_ID_ESTHER);
            //regidPhilipp = getStoredId(context, PROPERTY_REG_ID_PHILIPP);

            if (regid.isEmpty()) {
                registerGCMInBackground();
            }

            if (!regid.equals(regidPhilipp)) {
                if (!regid.equals(regidEsther)) {
                    Helper.storeRegistrationId(regid, context, PROPERTY_REG_ID_ESTHER);
                }
                sendRegidToPhilipp();
            }

            Log.i(TAG, "My regid: " + regid);
            Log.i(TAG, "PH regid: " + regidPhilipp);
            Log.i(TAG, "ES regid: " + regidEsther);
            Log.i(TAG, "... which means, that this device is " + (regid.equals(regidPhilipp) ? "Philipps" : "Esthers") + " device.");

        } else {
            Log.i("BaerliWeg", "No valid Google Play Services APK found!");
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("state", state);
        savedInstanceState.putFloat("dist", dist);
        savedInstanceState.putString("dist_txt", distance.getText().toString());
        savedInstanceState.putString("msg_txt", message.getText().toString());
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        state = savedInstanceState.getInt("state");
        dist = savedInstanceState.getFloat("dist");
        distance.setText(savedInstanceState.getString("dist_txt"));
        message.setText(savedInstanceState.getString("msg_txt"));

        setState(state);

    }

    private void sendRegidToPhilipp() {
        Log.i(TAG, "I am Esther and sending my regid to Philipp :)");

        try {
            JSONObject data = new JSONObject();
            data.put("my_action", "TRANSMIT_REGID");
            data.put("my_regid", regid);
            data.put("my_regid_owner", PROPERTY_REG_ID_ESTHER);

            Helper.transmit(data, new JSONArray().put(regidPhilipp));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        locationClient.connect();
    }

    @Override
    protected void onStop() {
        locationClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unregisterGCMInBackground();
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendNotification() {
        Log.i(TAG, "Sending baerli request!");

        final Location currentLocation = locationClient.getLastLocation();

        try {
            JSONObject data = new JSONObject();
            data.put("my_action", "STANDARD");
            data.put("my_loc_lat", currentLocation.getLatitude());
            data.put("my_loc_lon", currentLocation.getLongitude());
            data.put("my_loc_acc", currentLocation.getAccuracy());
            data.put("my_loc_alt", currentLocation.getAltitude());
            data.put("my_loc_tim", currentLocation.getTime());
            data.put("my_loc_ber", currentLocation.getBearing());
            data.put("my_step", 1);

            //Helper.transmit(data, new JSONArray().put((regid.equals(regidPhilipp) ? regidEsther : regidPhilipp)));
            Helper.transmit(data, new JSONArray().put(regid));
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Helper.BaerliTransmitException e) {
            distance.setText("");
            message.setText("Etwas ist schiefgegangen :(\n" + e.toString());
            setState(STATE_ANSWER);
        }
    }

    private void cancelBaerliWeg(String msg) {
        Log.w(TAG, "CancelBaerliWeg: " + msg);
    }

    private void registerGCMInBackground() {
        new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] objects) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;
                    Log.i(TAG, msg);

                    Helper.storeRegistrationId(regid, context, PROPERTY_REG_ID);
                } catch (IOException e) {
                    msg = "Error:" + e.getMessage();
                    throw new RuntimeException(e);
                }

                return msg;
            }

        }.execute(null, null, null);
    }

    private void unregisterGCMInBackground() {
        new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] objects) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    gcm.unregister();
                    msg = "Device unregistered";
                } catch (IOException e) {
                    msg = "Error:" + e.getMessage();
                }

                return msg;
            }

        }.execute(null, null, null);
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, 9000).show();
            } else {
                Log.i("BaerliWeg", "This device is not supported!");
                finish();
            }
            return false;
        }
        return true;
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
        Toast.makeText(this, "Connection failed!", Toast.LENGTH_SHORT).show();

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, 9000);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, e.toString());
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("" + connectionResult.getErrorCode());
        }
    }

    public static final int HIDE = 0, SHOW = 1;

    public void setState(int state) {
        if (state == STATE_INIT) {
            question.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
            answer.setVisibility(View.INVISIBLE);
        } else if (state == STATE_WAIT) {
            question.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            answer.setVisibility(View.INVISIBLE);
        } else if (state == STATE_ANSWER) {
            question.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
            answer.setVisibility(View.VISIBLE);
        }

        this.state = state;
    }

}
