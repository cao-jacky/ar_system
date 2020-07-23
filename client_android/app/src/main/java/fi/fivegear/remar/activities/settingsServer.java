package fi.fivegear.remar.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.InetAddresses;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import fi.fivegear.remar.R;
import fi.fivegear.remar.helpers.DatabaseHelper;
import fi.fivegear.remar.models.ServerInfo;

public class settingsServer extends Activity {
    public static final String currServerSettings = "currServerSettings";

    SharedPreferences sharedPreferences;
    DatabaseHelper db;

    Button editServerDetails;

    String serverIP;
    int serverPort;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_server);

        // using SharedPreferences to set current server IP and port
        sharedPreferences = getSharedPreferences(currServerSettings, Context.MODE_PRIVATE);

        serverIP = sharedPreferences.getString("currServerIP", "0.0.0.0");
        serverPort = sharedPreferences.getInt("currServerPort", 0);

        // setting displayed current values to that of which is found in shared values
        TextView currServerIPTV = (TextView)findViewById(R.id.serverIPText);
        currServerIPTV.setText(serverIP);

        TextView currServerPortTV = (TextView)findViewById(R.id.serverPortText);
        currServerPortTV.setText(Integer.toString(serverPort));

        db = new DatabaseHelper(getApplicationContext());

//        long currUnix = System.currentTimeMillis();
//        ServerInfo si1 = new ServerInfo(currUnix, "0.0.0.0", 12345);
//        long si1_id = db.createServerInfo(si1);

        //Log.d("Tag Count", "Tag Count: " + db.getAllServerInfo().get(0).getServerPort());

        db.closeDB();

        editServerDetails = (Button)findViewById(R.id.editServerDetails);
        editServerDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editServerDetailsModal();
            }
        });


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

    public void editServerDetailsModal() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        builder.setTitle("Set server details");
//        builder.setMessage("AlertDialog");
//        builder.setView(R.layout.popup_server_details);
        builder.setView(inflater.inflate(R.layout.popup_server_details, null));
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Dialog d = (Dialog) dialog;
                EditText serverIPET, serverPortET;

                serverIPET = (EditText)d.findViewById(R.id.setServerIP);
                serverPortET = (EditText)d.findViewById(R.id.setServerPort);

                String setServerIP = serverIPET.getText().toString();
                String setServerPortString = serverPortET.getText().toString();

                // performs verification of the provided IP and port
                boolean isIPValid = InetAddresses.isNumericAddress(setServerIP);
                boolean isPortValid = isNumeric(setServerPortString);
                if (!isIPValid && !isPortValid) {
                    Toast.makeText(settingsServer.this, "Both IP and port are invalid", Toast.LENGTH_SHORT).show();
                } else if (!isIPValid) {
                    Toast.makeText(settingsServer.this, "Invalid IP", Toast.LENGTH_SHORT).show();
                } else if (!isPortValid) {
                    Toast.makeText(settingsServer.this, "Invalid port", Toast.LENGTH_SHORT).show();
                } else {
                    int setServerPort = Integer.parseInt(serverPortET.getText().toString());

                    // setting new SharedPreferences variables
                    sharedPreferences = getSharedPreferences(currServerSettings, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("currServerIP", setServerIP);
                    editor.putInt("currServerPort", setServerPort);
                    editor.apply();

                    Toast.makeText(settingsServer.this, "Successfully changed details", Toast.LENGTH_SHORT).show();
                    dialog.cancel();

                    // changing text of the currently set server details
                    TextView currServerIPTV = (TextView)findViewById(R.id.serverIPText);
                    currServerIPTV.setText(setServerIP);

                    TextView currServerPortTV = (TextView)findViewById(R.id.serverPortText);
                    currServerPortTV.setText(Integer.toString(setServerPort));

                    // Refereshing the activity to show new changes
//                finish();
//                startActivity(getIntent());

                }

            }
        });
        builder.create();
        builder.show();
    }
}