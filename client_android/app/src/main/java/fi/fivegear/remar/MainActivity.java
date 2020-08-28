package fi.fivegear.remar;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.InetAddresses;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.multidex.MultiDex;

import fi.fivegear.remar.activities.AugmentedRealityActivity;
import fi.fivegear.remar.helpers.DatabaseHelper;
import fi.fivegear.remar.models.ServerInfo;

public class MainActivity extends AppCompatActivity implements OnItemSelectedListener {
    private SharedPreferences sharedPreferencesSetup;

    private String currSessionNumber;
    private String lastServerIP;
    private String currProtocol;
    private String currEncodingJPEG;
    private String currEncodingPNG;

    private EditText setLowerFPS, setUpperFPS;

    private String selectedProtocol;
    private String selectedItem;
    private String jpegCompression;
    private String pngCompression;

    private Button setARSettingsButton;

    protected Context context;

    private DatabaseHelper serverDatabase;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermission(); // Check for user permissions before loading main activity
        setContentView(R.layout.activity_main);

        // obtaining current session number
        sharedPreferencesSetup = getSharedPreferences("currSetupSettings", Context.MODE_PRIVATE);
        currSessionNumber = sharedPreferencesSetup.getString("currSessionNumber", "0");

        // get current server IP
        lastServerIP = sharedPreferencesSetup.getString("currServerIP", "0.0.0.0");

        // setting displayed current values to that of which is found in shared values
        EditText setServerIPEditText = findViewById(R.id.setServerIP);
        setServerIPEditText.setText(lastServerIP);

        // get current set protocol
        RadioGroup protocolRadioGroup = findViewById(R.id.setProtocolGroup);
        currProtocol = sharedPreferencesSetup.getString("currProtocol", "UDP");
        int count = protocolRadioGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View o = protocolRadioGroup.getChildAt(i);
            if (o instanceof RadioButton) {
                RadioButton currRadioButton = (RadioButton)o;
                String buttonProtocolID = (String)currRadioButton.getText();
                if (buttonProtocolID.contains(currProtocol)) {
                    currRadioButton.setChecked(true);
                } else {
                    currRadioButton.setChecked(false);
                }
            }
        }

        // setting protocol string variable as it is selected
        protocolRadioGroup.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            int radioButtonID = radioGroup.getCheckedRadioButtonId();
            RadioButton radioButton = radioGroup.findViewById(radioButtonID);
            selectedProtocol = (String)radioButton.getText();

        });

        // creating the list of resolutions
        Spinner staticSpinnerResolutions = findViewById(R.id.setResolutionSpinner);
        staticSpinnerResolutions.setOnItemSelectedListener(this);

        ArrayAdapter<CharSequence> staticAdapterResolutions = ArrayAdapter.createFromResource(this,
                R.array.resolutions_array, android.R.layout.simple_spinner_item);
        staticAdapterResolutions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        staticSpinnerResolutions.setAdapter(staticAdapterResolutions);

        // creating the list of encodings
        Spinner staticSpinnerEncodings = findViewById(R.id.setEncodingSpinner);
        staticSpinnerEncodings.setOnItemSelectedListener(this);

        ArrayAdapter<CharSequence> staticAdapterEncodings = ArrayAdapter.createFromResource(this,
                R.array.encodings_array, android.R.layout.simple_spinner_item);
        staticAdapterEncodings.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        staticSpinnerEncodings.setAdapter(staticAdapterEncodings);

        // centering the text of the FPS setting edit fields
        setLowerFPS = findViewById(R.id.setFPSLower);
        setLowerFPS.setGravity(Gravity.CENTER_HORIZONTAL);

        setUpperFPS = findViewById(R.id.setFPSUpper);
        setUpperFPS.setGravity(Gravity.CENTER_HORIZONTAL);

        // settings confirmation button
        setARSettingsButton = findViewById(R.id.setARSettings);
        setARSettingsButton.setOnClickListener(v -> openARActivity());

        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
        }
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
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
        } else {
            checkPermission();
        }
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            int d = Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void openARActivity(){
        // set variables into sharedPreferences before launching the AR intent
        Boolean openAR = true;

        // set server IP
        EditText setServerIPEditText = findViewById(R.id.setServerIP);
        String newServerIP = String.valueOf(setServerIPEditText.getText());

        // performs verification of the provided IP and port
        boolean isIPValid = InetAddresses.isNumericAddress(newServerIP);
        if (!isIPValid) {
            Toast.makeText(this, "Invalid IP", Toast.LENGTH_SHORT).show();
            openAR = false;
        } else {
            SharedPreferences.Editor sessionEditor = sharedPreferencesSetup.edit();
            sessionEditor.putString("currServerIP", newServerIP);
            sessionEditor.apply();

            // adding entry into the Database
            serverDatabase = new DatabaseHelper(getApplicationContext());
            String currUnix = String.valueOf(System.currentTimeMillis());
            ServerInfo newServerEntry = new ServerInfo(currUnix, newServerIP, 50000);
            long newServerEntry_id = serverDatabase.createServerInfo(newServerEntry);
            serverDatabase.closeDB();
            openAR = true;
        }

        // increase session ID
        SharedPreferences.Editor sessionEditor = sharedPreferencesSetup.edit();
        String newSessionNumber = String.valueOf(Integer.parseInt(currSessionNumber) + 1);
        sessionEditor.putString("currSessionNumber", newSessionNumber);
        sessionEditor.apply();

        Toast.makeText(MainActivity.this, "Session number increased from " + currSessionNumber
                + " to " + newSessionNumber, Toast.LENGTH_LONG).show();

        // change selected protocol variable
        SharedPreferences.Editor protocolEditor = sharedPreferencesSetup.edit();
        protocolEditor.putString("currProtocol", selectedProtocol);
        protocolEditor.apply();

        Log.d("ASD", selectedItem);

        if (selectedItem.contains("x")) {
            // set selected resolution if contains an "x"
            String[] separatedResolution = selectedItem.split(" x ");
            SharedPreferences.Editor resolutionEditor = sharedPreferencesSetup.edit();
            resolutionEditor.putInt("currHeight", Integer.parseInt(separatedResolution[0]));
            resolutionEditor.putInt("currWidth", Integer.parseInt(separatedResolution[1]));
            resolutionEditor.apply();
        }
        if (selectedItem.contains("JPEG")) {
            EditText encodingEditText = findViewById(R.id.setEncodingExtraSetting);
            jpegCompression = String.valueOf(encodingEditText.getText());

            // storing the encoding type selected and the compress percentage
            SharedPreferences.Editor encodingEditor = sharedPreferencesSetup.edit();
            encodingEditor.putString("currEncoding", selectedItem);
            encodingEditor.putString("currEncodingJPEG", jpegCompression);
            encodingEditor.apply();
        }
        if (selectedItem.contains("PNG")) {
            EditText encodingEditText = findViewById(R.id.setEncodingExtraSetting);
            pngCompression = String.valueOf(encodingEditText.getText());

            SharedPreferences.Editor encodingEditor = sharedPreferencesSetup.edit();
            encodingEditor.putString("currEncoding", selectedItem);
            encodingEditor.putString("currEncodingPNG", pngCompression);
            encodingEditor.apply();
        }
        if (selectedItem.contains("MP4") || selectedItem.contains("MPEG-TS")) {
            // else, assume it is an encoding to deal with
            SharedPreferences.Editor encodingEditor = sharedPreferencesSetup.edit();
            encodingEditor.putString("currEncoding", selectedItem);
            encodingEditor.apply();
        }

        // set the sending rate
        EditText setLowerFPSEditText = findViewById(R.id.setFPSLower);
        EditText setUpperFPSEditText = findViewById(R.id.setFPSUpper);

        int setLowerFPSInt = Integer.parseInt(setLowerFPSEditText.getText().toString());
        int setUpperFPSInt = Integer.parseInt(setUpperFPSEditText.getText().toString());

        SharedPreferences.Editor sendingRateEditor = sharedPreferencesSetup.edit();
        sendingRateEditor.putInt("currLowerFPS", setLowerFPSInt);
        sendingRateEditor.putInt("currUpperFPS", setUpperFPSInt);
        sendingRateEditor.apply();

        // set the UDP payload size
        EditText setUDPPayloadEditText = findViewById(R.id.setUDPPayload);
        int setUDPPayloadInt = Integer.parseInt(setUDPPayloadEditText.getText().toString());
        SharedPreferences.Editor udpPayloadEditor = sharedPreferencesSetup.edit();
        udpPayloadEditor.putInt("currUDPPayload", setUDPPayloadInt);
        sendingRateEditor.apply();

        if (openAR) {
            Intent intent = new Intent(this, AugmentedRealityActivity.class);
            startActivity(intent);
        }
    }

    //Performing action onItemSelected and onNothing selected
    @Override
    public void onItemSelected(AdapterView<?> parent, View arg1, int position, long id) {
        selectedItem = String.valueOf(parent.getItemAtPosition(position));

        if (!selectedItem.contains("x")) {
            if (selectedItem.contains("JPEG")) {
                TextView encodingHelpText = findViewById(R.id.setEncodingExtraSettingHelpText);
                encodingHelpText.setText(R.string.encoding_jpeg_help_text);
                encodingHelpText.setVisibility(View.VISIBLE);

                EditText encodingEditText = findViewById(R.id.setEncodingExtraSetting);

                // pull last set quality from sharedPreferences
                currEncodingJPEG = sharedPreferencesSetup.getString("currEncodingJPEG", "70");
                encodingEditText.setText(currEncodingJPEG);
                encodingEditText.setFilters(new InputFilter[]{new InputFilterMinMax("0", "100")});
                encodingEditText.setVisibility(View.VISIBLE);
            }
            if (selectedItem.contains("PNG")) {
                TextView encodingHelpText = findViewById(R.id.setEncodingExtraSettingHelpText);
                encodingHelpText.setText(R.string.encoding_png_help_text);
                encodingHelpText.setVisibility(View.VISIBLE);

                EditText encodingEditText = findViewById(R.id.setEncodingExtraSetting);

                // pull last set quality from sharedPreferences
                currEncodingPNG = sharedPreferencesSetup.getString("currEncodingPNG", "3");
                encodingEditText.setText(currEncodingPNG);
                encodingEditText.setFilters(new InputFilter[]{new InputFilterMinMax("0", "9")});
                encodingEditText.setVisibility(View.VISIBLE);
            }
            if (selectedItem.contains("MPEG-TS") || selectedItem.contains("MP4")) {
                // hiding if the encoding is "standard"
                TextView encodingHelpText = findViewById(R.id.setEncodingExtraSettingHelpText);
                encodingHelpText.setVisibility(View.GONE);

                EditText encodingEditText = findViewById(R.id.setEncodingExtraSetting);
                encodingEditText.setVisibility(View.GONE);
            }


        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }


    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        MultiDex.install(this);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
    }

    public class InputFilterMinMax implements InputFilter {

        private int min, max;

        public InputFilterMinMax(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public InputFilterMinMax(String min, String max) {
            this.min = Integer.parseInt(min);
            this.max = Integer.parseInt(max);
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            try {
                // Remove the string out of destination that is to be replaced
                String newVal = dest.toString().substring(0, dstart) + dest.toString().substring(dend, dest.toString().length());
                // Add the new string in
                newVal = newVal.substring(0, dstart) + source.toString() + newVal.substring(dstart, newVal.length());
                int input = Integer.parseInt(newVal);
                if (isInRange(min, max, input))
                    return null;
            } catch (NumberFormatException nfe) { }
            return "";
        }
        private boolean isInRange(int a, int b, int c) {
            return b > a ? c >= a && c <= b : c >= b && c <= a;
        }
    }

}