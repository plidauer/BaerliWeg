package at.plidauer.baerliweg;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
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

    final int STATE_INIT = 0;
    final int STATE_WAIT = 1;
    final int STATE_ANSWER = 2;

    int state = STATE_INIT;

    protected static String regid;
    protected static String regidPhilipp = "APA91bHUV5wWKAedXWEdyHb4pgvdfADJ-ERHocdGUA3IrQWyGihV-uU1huriuYHFtPd5lQGSyUOWD6SnwxCNRqzQTy4lMsM7hwpj8WY537QQdJxUvmTfGyi9uaq61MPcnY2lTbSfbROdNJZRQunF1iHPF31UOBe15w";
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
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
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

    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            int state = message.arg1;
            switch (state) {
                case HIDE:
                    progressBar.setVisibility(View.INVISIBLE);
                    break;
                case SHOW:
                    progressBar.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }
}
