package fi.fivegear.remar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.multidex.MultiDex;
import androidx.core.app.ActivityCompat;

import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;

import fi.fivegear.remar.activities.SettingsActivity;
import fi.fivegear.remar.activities.StatsActivity;
import fi.fivegear.remar.helpers.DatabaseHelper;
import fi.fivegear.remar.models.RequestEntry;
import fi.fivegear.remar.models.SessionInfo;

import static fi.fivegear.remar.Constants.previewHeight;
import static fi.fivegear.remar.Constants.previewWidth;

public class MainActivity extends Activity implements LocationListener, SensorEventListener,
        View.OnTouchListener {

    private SurfaceView mPreview;
    private SurfaceHolder mPreviewHolder;

    private Camera mCamera;
    private boolean mInPreview = false;
    private boolean mCameraConfigured = false;
    private DrawOnTop mDraw;
    private byte[] callbackBuffer;
    private int time_o, time_n, fps;
    private boolean recoFlag = false;
    private int frameID;
    public byte[] locationbyte;
    //-----------------------------------pengzhou: location service---------------------------------
    protected LocationManager locationManager;
    protected LocationListener locationListener;
    Location gps_loc;
    Location network_loc;
    Location final_loc;
    protected Context context;
    protected double latitude;
    protected double longitude;
    protected double altitude;
    protected double timeCaptured;
    protected double timeSend;
    protected float bearing;
    TextView txtLat;

    private SensorManager mSensorManager;
    private Sensor mProximity;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];
    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];
    private double angle;

    public static ImageView uploadStatus;
    public static ImageView downloadStatus;
    public static TextView currFrame;

    public static int screenWidth;
    public static int screenHeight;

    FrameLayout settingsButton, statsButton;

    public SharedPreferences sharedPreferencesSession, sharedPreferencesLocation;

    public TextView sessionController;
    String currSessionNumber;
    TextView sessionGlanceString, modalCurrSessionNumber;

    private double sessionTimeInitiated;
    private DatabaseHelper db;

    String TAG = "DBG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermission(); // Check for user permissions before loading main activity
        getScreenResolution(this); // required to draw annotations on screen
        setContentView(R.layout.activity_main);

        // obtain UID of application
        int uid;
        try {
            ApplicationInfo info = this.getPackageManager().getApplicationInfo(this.getPackageName(), 0);
            uid = info.uid;
        } catch (PackageManager.NameNotFoundException e) {
            uid = -1;
        }

//        NetworkStatsManager networkStatsManager = (NetworkStatsManager) getApplicationContext().getSystemService(Context.NETWORK_STATS_SERVICE);
//
//        NetworkStats networkStats = null;
//
//        TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
//        String subscriberID = tm.getSubscriberId();
//        networkStats = networkStatsManager.queryDetailsForUid(
//                ConnectivityManager.TYPE_MOBILE,
//                subscriberID,
//                System.currentTimeMillis(),
//                System.currentTimeMillis()+2,
//                uid);
//        NetworkStats.Bucket bucket = new NetworkStats.Bucket();
//        System.out.print(uid+"\n");
//        System.out.print(bucket.getTxBytes());
//        networkStats.getNextBucket(bucket);

        // Status indicators for sending and receiving from server
        uploadStatus = (ImageView) findViewById(R.id.statusUpload);
        downloadStatus = (ImageView) findViewById(R.id.statusDownload);

        currFrame = (TextView)findViewById(R.id.frameGlance);

        sharedPreferencesSession = getSharedPreferences("currSessionSetting", Context.MODE_PRIVATE);
        currSessionNumber = sharedPreferencesSession.getString("currSessionNumber", "0");
        sessionGlanceString = (TextView) findViewById(R.id.sessionGlance);

        SharedPreferences.Editor sessionEditor = sharedPreferencesSession.edit();
        String newSessionNumber = String.valueOf(Integer.parseInt(currSessionNumber) + 1);
        sessionEditor.putString("currSessionNumber", newSessionNumber);
        sessionEditor.apply();

        sessionTimeInitiated = System.currentTimeMillis();

        // changing session number to new increment
        sessionGlanceString.setText("S" + newSessionNumber);

        Toast.makeText(MainActivity.this, "Session number increased from " + currSessionNumber
                + " to " + newSessionNumber, Toast.LENGTH_LONG).show();

        // preparing new entry into sessions database
        db = new DatabaseHelper(this);
        SessionInfo newSessionInfo = new SessionInfo(Integer.parseInt(newSessionNumber),
                String.valueOf(sessionTimeInitiated), "",
                0, 0, 0, 0, 0, 0, 0);
        long newSessionInfo_id = db.createSessionsEntry(newSessionInfo);

//        sessionController.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                openSessionGlanceModal();
//            }
//        });

        // Settings button
        settingsButton = (FrameLayout) findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettingsActivity();
            }
        });

        // statistics button
        statsButton = (FrameLayout) findViewById(R.id.statsButton);
        statsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openStatsActivity();
            }
        });

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);

        // set the last location as the current variable in SharedPreferences
        network_loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        String lat_long_alt = String.valueOf(network_loc.getLatitude()) + ","
                + String.valueOf(network_loc.getLongitude()) + ","
                + String.valueOf(network_loc.getAltitude());

        sharedPreferencesLocation = getSharedPreferences("currLocationSetting", Context.MODE_PRIVATE);
        SharedPreferences.Editor locationEditor = sharedPreferencesLocation.edit();
        locationEditor.putString("currLocation", lat_long_alt);
        locationEditor.apply();

        System.out.println(getNetworkClass(this));

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mPreview = findViewById(R.id.cameraPreview);
        mPreview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        mPreviewHolder = mPreview.getHolder();
        mPreviewHolder.addCallback(surfaceCallback);
        mPreview.setZOrderMediaOverlay(false);
        mPreview.setOnTouchListener(this);

        if (Constants.Show2DView) {
            mDraw = new DrawOnTop(this);
            addContentView(mDraw, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        ARManager.getInstance().init(this, true);
        ARManager.getInstance().setCallback(new ARManager.Callback() {
            @Override
            public void onObjectsDetected(Detected[] detected) {
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

    public void checkPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE}, 1);
        }

//        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
//        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
//                android.os.Process.myUid(), getPackageName());
//        if (mode == AppOpsManager.MODE_ALLOWED) {
//            return;
//        } else {
//            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
//            startActivity(intent);
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
        } else {
            checkPermission();
        }
    }

    //Here Manifest.permission.READ_PHONE_STATS is needed
    private String getSubscriberId(Context context, int networkType) {
        if (ConnectivityManager.TYPE_MOBILE == networkType) {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            }
            return tm.getSubscriberId();
        }
        return "";
    }

    public void openSessionGlanceModal() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        builder.setTitle("Session details");
//        builder.setMessage("AlertDialog");
//        builder.setView(R.layout.popup_server_details);
//        builder.setView(inflater.inflate(R.layout.popup_session_glance, null));
        View content =  inflater.inflate(R.layout.popup_session_glance, null);
        builder.setView(content);

        currSessionNumber = sharedPreferencesSession.getString("currSessionNumber", "0");
        modalCurrSessionNumber = (TextView)content.findViewById(R.id.editSessionNumber);
        modalCurrSessionNumber.setGravity(Gravity.CENTER_HORIZONTAL);
        modalCurrSessionNumber.setText(currSessionNumber);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Dialog d = (Dialog) dialog;
                EditText sessionNumber;

                sessionNumber = (EditText)d.findViewById(R.id.editSessionNumber);

                String sessionNumberString = sessionNumber.getText().toString();

                SharedPreferences.Editor editor = sharedPreferencesSession.edit();
                editor.putString("currSessionNumber", sessionNumberString);
                editor.apply();

                // changing text of the currently set server details
                sessionGlanceString.setText("S" + sessionNumberString);

                Toast.makeText(MainActivity.this, "Successfully set new session number", Toast.LENGTH_SHORT).show();
                dialog.cancel();
            }
        });
        builder.create();
        builder.show();
    }

    public void openSettingsActivity(){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void openStatsActivity(){
        Intent intent = new Intent(this, StatsActivity.class);
        startActivity(intent);
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

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        MultiDex.install(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude =  location.getLatitude();
        longitude = location.getLongitude();
        altitude = location.getAltitude();
        String lat_long_alt = latitude + ","  + longitude + "," + altitude;
//        Toast.makeText(MainActivity.this, lat_long_alt, Toast.LENGTH_LONG).show();

        sharedPreferencesLocation = getSharedPreferences("currLocationSetting", Context.MODE_PRIVATE);
        SharedPreferences.Editor locationEditor = sharedPreferencesLocation.edit();
        locationEditor.putString("currLocation", lat_long_alt);
        locationEditor.apply();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude","disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude","enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude","status");
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }


    @Override
    public void onStart() {
        Log.i(Constants.TAG, " onStart() called.");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.i(Constants.TAG, " onResume() called.");
        super.onResume();

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            mCamera = Camera.open();
        }

//        recreate();
    }

    @Override
    public void onPause() {
        Log.i(Constants.TAG, " onPause() called.");
        super.onPause();

        if (mInPreview)
            mCamera.stopPreview();

        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.lock();
        mCamera.release();
        mCamera = null;
        mInPreview = false;
    //mSensorManager.unregisterListener(this);
//        ARManager.getInstance().stop();

    }

    @Override
    public void onStop() {
        Log.i(Constants.TAG, " onStop() called.");
        ARManager.getInstance().stop();
        finish();
//        System.exit(2);
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        Log.i(Constants.TAG, " onDestroy() called.");
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        /*if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }*/

        //angle = Math.atan2(event.values[0], event.values[1])/(Math.PI/180);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        recoFlag = true;
        return this.onTouchEvent(event);
    }

    private void initPreview(int width, int height) {
        Log.i(Constants.TAG, "initPreview() called");
        if (mCamera != null && mPreviewHolder.getSurface() != null) {
            if (!mCameraConfigured) {
                Camera.Parameters params = mCamera.getParameters();
                params.setPreviewSize(width, height);

                callbackBuffer = new byte[(height + height / 2) * width];
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                params.set("camera-id", 2);
                mCamera.setParameters(params);
                mCameraConfigured = true;
            }

            try {
                mCamera.setPreviewDisplay(mPreviewHolder);
                mCamera.addCallbackBuffer(callbackBuffer);
                mCamera.setPreviewCallbackWithBuffer(frameIMGProcCallback);
            } catch (Throwable t) {
                Log.e(Constants.TAG, "Exception in initPreview()", t);
            }
        }
    }

    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(Constants.TAG, " surfaceCreated() called.");
            initPreview(previewWidth, Constants.previewHeight);
            if (mCameraConfigured && mCamera != null) {
                mCamera.setDisplayOrientation(90);
                mCamera.startPreview();
                mCamera.autoFocus(null);
                mInPreview = true;
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.i(Constants.TAG, " surfaceChanged() called.");
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i(Constants.TAG, " surfaceDestroyed() called.");
        }
    };

    Camera.PreviewCallback frameIMGProcCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            mCamera.addCallbackBuffer(callbackBuffer);

            frameID++;

            ARManager.getInstance().recognizeTime(frameID, data);
            ARManager.getInstance().driveFrame(data);
            mDraw.invalidate();
        }
    };


    class DrawOnTop extends View {
        Paint paintWord;
        Paint paintLine;
        Paint paintAlarm;
        Paint paintIcon;
        Paint paintBackground;
        private boolean ShowFPS = true;
        private boolean ShowEdge = true;
        private boolean ShowName = true;
        private int preFrameID;
        private float dispScale = Constants.dispScale;
        private int axisShiftHorizontal = Constants.axisShiftHorizontal;
        private int axisShiftVertical = Constants.axisShiftVertical;
        private Detected[] detecteds;
        private double distance;
        //private Drawable mCustomImage_pedestrian;
        //private Drawable mCustomImage_cup;

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

        public void updateData(int frameID){
            if (ShowFPS) {
                time_n = (int) System.currentTimeMillis();
                fps = 1000 * (frameID - preFrameID) / (time_n - time_o);
                time_o = time_n;
                preFrameID = frameID;
            }
        }

        public double degreesToRadians(double degrees) {
            return degrees * Math.PI / 180;
        }

        public double distanceInKmBetweenEarthCoordinates(double lat1, double lon1, double lat2, double lon2) {
            double earthRadiusKm = 6371;

            double dLat = degreesToRadians(lat2-lat1);
            double dLon = degreesToRadians(lon2-lon1);

            lat1 = degreesToRadians(lat1);
            lat2 = degreesToRadians(lat2);

            double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                    Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            return earthRadiusKm * c;
        }

        public void updateData(Detected[] detecteds) {
            this.detecteds = detecteds;
        }

        public void updateOrientationAngles() {
            // Update rotation matrix, which is needed to update orientation angles.
            SensorManager.getRotationMatrix(mRotationMatrix, null,
                    mAccelerometerReading, mMagnetometerReading);

            // "mRotationMatrix" now has up-to-date information.
            //SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationMatrix);

            SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);
            angle = Math.atan2(mAccelerometerReading[0], mAccelerometerReading[2])/(Math.PI/180);

            // "mOrientationAngles" now has up-to-date information.
        }

        @Override
        protected void onDraw(final Canvas canvas) {
            super.onDraw(canvas);

            if(detecteds != null) {
                for (Detected detected : detecteds) {
                    Log.d("test", detected.name);
                    float scale_width = (previewWidth-screenWidth);
                    float scale_height = (previewHeight-screenHeight+screenHeight/2-screenHeight/6);
                    canvas.drawRect((detected.left)*dispScale-scale_width-5, (detected.top)*(dispScale)-scale_height-50,
                            (detected.right)*dispScale-scale_width, (detected.top)*(dispScale)-scale_height, paintBackground);
                    canvas.drawText(detected.name +  " " + detected.prob,
                            (detected.left)*dispScale-scale_width, (detected.top)*dispScale-scale_height-10, paintWord);
                    canvas.drawRect((detected.left)*dispScale-scale_width, (detected.top)*(dispScale)-scale_height,
                            (detected.right)*dispScale-scale_width, (detected.bot)*(dispScale)-scale_height, paintLine);
                }

            }
            //canvas.drawText( ", o: " + bearing, 500,500, paintWord);
        }
    }
}