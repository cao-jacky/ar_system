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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
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

    private static int screenWidth;
    private static int screenHeight;

    // public to be accessible in other classes
    public static ImageView uploadStatus;
    public static ImageView downloadStatus;
    public static TextView currFrame;

    private SharedPreferences sharedPreferencesSetup;

    String currSessionNumber;
    TextView sessionGlanceString;

    FrameLayout settingsButton, statsButton, killButton;

    protected LocationManager locationManager;
    Location network_loc;
    protected double latitude;
    protected double longitude;
    protected double altitude;

    private DrawOnTop mDraw;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // prevent screen from sleeping
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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
        sessionGlanceString.setText("S"+currSessionNumber);

        // Settings button
        settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> openSettingsActivity());

        // statistics button
        statsButton = findViewById(R.id.statsButton);
        statsButton.setOnClickListener(v -> openStatsActivity());

        // kill app button
        killButton = findViewById(R.id.killButton);
        killButton.setOnClickListener(v -> killApp());

        locationManager = (LocationManager)getSystemService(this.LOCATION_SERVICE);
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
        ARManager.getInstance().setCallback(detected -> {
            mDraw.invalidate();
            mDraw.updateData(detected);
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

    public void killApp(){
        finishAffinity();
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

    public void onResume() {
//        setContentView(R.layout.activity_main);
        super.onResume();
    }

    public void onStop() {
        ARManager.getInstance().stop();
//        finishAffinity();
//        System.exit(0); // brute force exiting app
//        finishAndRemoveTask();

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("EXIT", true);
        startActivity(intent);
        super.onStop();
    }

    public void onDestroy() {
//        finishAndRemoveTask();

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("EXIT", true);
        startActivity(intent);

        super.onDestroy();
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
