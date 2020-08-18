package fi.fivegear.remar.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import fi.fivegear.remar.ARManager;
import fi.fivegear.remar.Constants;
import fi.fivegear.remar.Detected;
import fi.fivegear.remar.MainActivity;
import fi.fivegear.remar.R;
import fi.fivegear.remar.fragments.fragmentCamera;

public class AugmentedRealityActivity extends AppCompatActivity implements LocationListener {

    private static final String TAG = "ReMAR";
    public static int screenWidth;
    public static int screenHeight;

    public static ImageView uploadStatus;
    public static ImageView downloadStatus;
    public static TextView currFrame;

    public SharedPreferences sharedPreferencesSetup;

    String currSessionNumber;
    TextView sessionGlanceString;

    FrameLayout settingsButton, statsButton;

    protected LocationManager locationManager;
    Location network_loc;
    protected double latitude;
    protected double longitude;
    protected double altitude;

    private DrawOnTop mDraw;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getScreenResolution(this); // required to draw annotations on screen
        setContentView(R.layout.activity_augmented_reality);
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragmentCamera.newInstance())
                    .commit();
        }

        // Status indicators for sending and receiving from server
        uploadStatus = findViewById(R.id.statusUpload);
        downloadStatus = findViewById(R.id.statusDownload);

        currFrame = findViewById(R.id.frameGlance);

        sharedPreferencesSetup = getSharedPreferences("currSetupSettings", Context.MODE_PRIVATE);
        currSessionNumber = sharedPreferencesSetup.getString("currSessionNumber", "0");
        sessionGlanceString = findViewById(R.id.sessionGlance);

        // changing session number to new increment
//        sessionGlanceString.setText("S" + newSessionNumber);

        // Settings button
        settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> openSettingsActivity());

        // statistics button
        statsButton = findViewById(R.id.statsButton);
        statsButton.setOnClickListener(v -> openStatsActivity());

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);

        // set the last location as the current variable in SharedPreferences
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        network_loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        String lat_long_alt = network_loc.getLatitude() + "," + network_loc.getLongitude() + ","
                + network_loc.getAltitude();

        SharedPreferences.Editor locationEditor = sharedPreferencesSetup.edit();
        locationEditor.putString("currLocation", lat_long_alt);
        locationEditor.apply();

        if (Constants.Show2DView) {
            mDraw = new AugmentedRealityActivity.DrawOnTop(this);
            addContentView(mDraw, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        ARManager.getInstance().init(this);
        ARManager.getInstance().setCallback(new ARManager.Callback() {
            @Override
            public void onObjectsDetected(Detected[] detected) {
                mDraw.invalidate();
                mDraw.updateData(detected);
            }
        });

    }

    private void getScreenResolution(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
    }

    public void openSettingsActivity(){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void openStatsActivity(){
        Intent intent = new Intent(this, StatsActivity.class);
        startActivity(intent);
    }

    public void onLocationChanged(Location location) {
        latitude =  location.getLatitude();
        longitude = location.getLongitude();
        altitude = location.getAltitude();
        String lat_long_alt = latitude + ","  + longitude + "," + altitude;
//        Toast.makeText(MainActivity.this, lat_long_alt, Toast.LENGTH_LONG).show();

        sharedPreferencesSetup = getSharedPreferences("currSetupSettings", Context.MODE_PRIVATE);
        SharedPreferences.Editor locationEditor = sharedPreferencesSetup.edit();
        locationEditor.putString("currLocation", lat_long_alt);
        locationEditor.apply();
    }

    public static String getNetworkClass(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null || !info.isConnected())
            return "-"; // not connected
        if (info.getType() == ConnectivityManager.TYPE_WIFI)
            return "WiFi";
        if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
            int networkType = info.getSubtype();
            switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:     // api< 8: replace by 11
                case TelephonyManager.NETWORK_TYPE_GSM:      // api<25: replace by 16
                    return "2G";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:   // api< 9: replace by 12
                case TelephonyManager.NETWORK_TYPE_EHRPD:    // api<11: replace by 14
                case TelephonyManager.NETWORK_TYPE_HSPAP:    // api<13: replace by 15
                case TelephonyManager.NETWORK_TYPE_TD_SCDMA: // api<25: replace by 17
                    return "3G";
                case TelephonyManager.NETWORK_TYPE_LTE:      // api<11: replace by 13
                case TelephonyManager.NETWORK_TYPE_IWLAN:    // api<25: replace by 18
                case 19: // LTE_CA
                    return "4G";
                case TelephonyManager.NETWORK_TYPE_NR:       // api<29: replace by 20
                    return "5G";
                default:
                    return "?";
            }
        }
        return "?";
    }

    class DrawOnTop extends View {
        Paint paintWord;
        Paint paintLine;
        Paint paintBackground;
        private float dispScale = Constants.dispScale;
        private Detected[] detecteds;

        public DrawOnTop(Context context) {
            super(context);

            paintBackground = new Paint();
            paintBackground.setColor(Color.parseColor("#FAFAFA"));
//            paintBackground.setAlpha(90);

            paintWord = new Paint();
            paintWord.setStyle(Paint.Style.STROKE);
            paintWord.setStrokeWidth(5);
            paintWord.setColor(Color.BLACK);
            paintWord.setTextAlign(Paint.Align.LEFT);
            paintWord.setTextSize(50);
//            paintWord.setAlpha(90);
            paintWord.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));

            paintLine = new Paint();
            paintLine.setStyle(Paint.Style.STROKE);
            paintLine.setStrokeWidth(7);
            paintLine.setColor(Color.parseColor("#FAFAFA"));
//            paintLine.setAlpha(90);

        }

        public void updateData(Detected[] detecteds) {
            this.detecteds = detecteds;
        }

        @Override
        protected void onDraw(final Canvas canvas) {
            super.onDraw(canvas);

            if(detecteds != null) {
                for (Detected detected : detecteds) {
                    float scale_width = 0; // (previewWidth-screenWidth);
                    float scale_height = -200; //(previewHeight-screenHeight+screenHeight/2-screenHeight/6);
                    canvas.drawRect((detected.left)*dispScale-scale_width-5, (detected.top)*(dispScale)-scale_height-50,
                            (detected.right)*dispScale-scale_width, (detected.top)*(dispScale)-scale_height, paintBackground);
                    canvas.drawText(detected.name +  " " + detected.prob,
                            (detected.left)*dispScale-scale_width, (detected.top)*dispScale-scale_height-10, paintWord);
                    canvas.drawRect((detected.left)*dispScale-scale_width, (detected.top)*(dispScale)-scale_height,
                            (detected.right)*dispScale-scale_width, (detected.bot)*(dispScale)-scale_height, paintLine);
                }

            }
        }
    }


}
