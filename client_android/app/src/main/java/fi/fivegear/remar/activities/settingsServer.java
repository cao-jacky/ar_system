package fi.fivegear.remar.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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

    private String m_Text = "";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_server);

        // using SharedPreferences to set current server IP and port
        sharedPreferences = getSharedPreferences(currServerSettings, Context.MODE_PRIVATE);

//        // testing sharedpreferences
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putString("currServerIP", "100.68.116.246");
//        editor.putInt("currServerPort", 52727);
//        editor.apply();

        String serverIP = sharedPreferences.getString("currServerIP", "0.0.0.0");
        int serverPort = sharedPreferences.getInt("currServerPort", 0);

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

    public void editServerDetailsModal() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        builder.setTitle("Custom view with 4 EditTexts");
        builder.setMessage("AlertDialog");
        builder.setView(R.layout.popup_server_details);
        //In case it gives you an error for setView(View) try
        builder.setView(inflater.inflate(R.layout.popup_server_details, null));
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(settingsServer.this, "assdasdasd", Toast.LENGTH_SHORT).show();
                dialog.cancel();
            }
        });
        builder.show();
    }
}