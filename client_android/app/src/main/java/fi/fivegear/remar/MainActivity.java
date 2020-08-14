package fi.fivegear.remar;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.multidex.MultiDex;

import fi.fivegear.remar.activities.AugmentedRealityActivity;
import fi.fivegear.remar.activities.SettingsActivity;
import fi.fivegear.remar.helpers.DatabaseHelper;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ReMAR";
    private double sessionTimeInitiated;

    public SharedPreferences sharedPreferencesLocation;

    private EditText setLowerFPS, setUpperFPS;


    protected Context context;

    public TextView sessionController;

    private DatabaseHelper db;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermission(); // Check for user permissions before loading main activity
        setContentView(R.layout.activity_main);

        // setting the data for the resolutions spinner
        Spinner staticSpinner = findViewById(R.id.setResolutionSpinner);

        // Create an ArrayAdapter using the string array and a default spinner
        ArrayAdapter<CharSequence> staticAdapter = ArrayAdapter
                .createFromResource(this, R.array.resolutions_array,
                        android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        staticAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        staticSpinner.setAdapter(staticAdapter);


        // centering the text of the FPS setting edit fields
        setLowerFPS = findViewById(R.id.setFPSLower);
        setLowerFPS.setGravity(Gravity.CENTER_HORIZONTAL);

        setUpperFPS = findViewById(R.id.setFPSUpper);
        setUpperFPS.setGravity(Gravity.CENTER_HORIZONTAL);

        // Settings button
//        openAR = (Button) findViewById(R.id.openAR);
//        openAR.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                openARActivity();
//            }
//        });

//        SharedPreferences.Editor sessionEditor = sharedPreferencesSession.edit();
//        String newSessionNumber = String.valueOf(Integer.parseInt(currSessionNumber) + 1);
//        sessionEditor.putString("currSessionNumber", newSessionNumber);
//        sessionEditor.apply();

//        Toast.makeText(MainActivity.this, "Session number increased from " + currSessionNumber
//                + " to " + newSessionNumber, Toast.LENGTH_LONG).show();

//        sessionTimeInitiated = System.currentTimeMillis();

//        // preparing new entry into sessions database
//        db = new DatabaseHelper(this);
//        SessionInfo newSessionInfo = new SessionInfo(Integer.parseInt(newSessionNumber),
//                String.valueOf(sessionTimeInitiated), "",
//                0, 0, 0, 0, 0, 0, 0);
//        long newSessionInfo_id = db.createSessionsEntry(newSessionInfo);
//
//
//
//        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);


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

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
        } else {
            checkPermission();
        }
    }

    public void openARActivity(){
        Intent intent = new Intent(this, AugmentedRealityActivity.class);
        startActivity(intent);
    }

    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        MultiDex.install(this);
    }


    public void onStop() {
        Log.i(Constants.TAG, " onStop() called.");
        ARManager.getInstance().stop();
        finish();
        super.onStop();

    }

    protected void onDestroy() {
        super.onDestroy();
    }

}