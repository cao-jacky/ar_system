package fi.fivegear.remar.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.net.InetAddresses;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.util.Date;
import java.util.List;

import fi.fivegear.remar.R;
import fi.fivegear.remar.helpers.DatabaseHelper;
import fi.fivegear.remar.models.ServerInfo;

public class settingsServer extends Activity {
    SharedPreferences sharedPreferences;
    DatabaseHelper serverDatabase;

    Button editServerDetails;

    String serverIP;
    int serverPort;

    private TableLayout serverTableLayout;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_server);

        // using SharedPreferences to set current server IP and port
        sharedPreferences = getSharedPreferences("currServerSettings", Context.MODE_PRIVATE);

        serverIP = sharedPreferences.getString("currServerIP", "0.0.0.0");
        serverPort = sharedPreferences.getInt("currServerPort", 0);

        // setting displayed current values to that of which is found in shared values
        TextView currServerIPTV = (TextView)findViewById(R.id.serverIPText);
        currServerIPTV.setText(serverIP);

        TextView currServerPortTV = (TextView)findViewById(R.id.serverPortText);
        currServerPortTV.setText(Integer.toString(serverPort));

        editServerDetails = (Button)findViewById(R.id.editServerDetails);
        editServerDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editServerDetailsModal();
            }
        });

        serverTableLayout = (TableLayout)findViewById(R.id.previousServersTable);
        populateServerTable(serverTableLayout);

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

    public static String getUTCstring(String dateString) {
        long dv = Long.valueOf(dateString);
        Date df = new java.util.Date(dv);
        String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(df);
        return date;
    }

    public void editServerDetailsModal() {
        final EditText serverIPET, serverPortET;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        builder.setTitle("Set server details");
//        builder.setMessage("AlertDialog");
//        builder.setView(R.layout.popup_server_details);

        View content =  inflater.inflate(R.layout.popup_server_details, null);
        builder.setView(content);

        sharedPreferences = getSharedPreferences("currServerSettings", Context.MODE_PRIVATE);

        serverIPET = (EditText)content.findViewById(R.id.setServerIP);
        serverPortET = (EditText)content.findViewById(R.id.setServerPort);

        serverIPET.setText(serverIP);
        serverPortET.setText(String.valueOf(serverPort));

//        builder.setView(inflater.inflate(R.layout.popup_server_details, null));
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Dialog d = (Dialog) dialog;

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

                    // adding entry into the Database
                    serverDatabase = new DatabaseHelper(getApplicationContext());
                    String currUnix = String.valueOf(System.currentTimeMillis());
                    ServerInfo newServerEntry = new ServerInfo(currUnix, setServerIP, setServerPort);
                    long newServerEntry_id = serverDatabase.createServerInfo(newServerEntry);
                    serverDatabase.closeDB();

                    // add row to the table
                    serverTableLayout = (TableLayout)findViewById(R.id.previousServersTable);
                    TableRow tableRow = new TableRow(settingsServer.this);

                    TextView dateAdded = new TextView(settingsServer.this);
                    dateAdded.setText(getUTCstring(currUnix));
                    dateAdded.setPadding(5, 5, 5, 5);
                    tableRow.addView(dateAdded);// add the column to the table row here

                    TextView serverIP = new TextView(settingsServer.this);
                    serverIP.setText(setServerIP);
                    serverIP.setPadding(5, 5, 5, 5);
                    tableRow.addView(serverIP);// add the column to the table row here

                    TextView serverPort = new TextView(settingsServer.this);
                    String currServerPort = String.valueOf(setServerPort);
                    serverPort.setText(currServerPort);
                    serverPort.setPadding(5, 5, 5, 5);
                    tableRow.addView(serverPort);// add the column to the table row here

                    serverTableLayout.addView(tableRow, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                    // Refereshing the activity to show new changes
//                finish();
//                startActivity(getIntent());
//                    finish();
//                    System.exit(2);

                }

            }
        });
        builder.create();
        builder.show();
    }

    private void populateServerTable(TableLayout tableLayout){
        serverDatabase = new DatabaseHelper(getApplicationContext());
        List<ServerInfo> serverDatabaseData = serverDatabase.getAllServerInfo();

        // Setting header row
        TableRow headerTableRow = new TableRow(this);

        TextView headerDateAdded = new TextView(this);
        headerDateAdded.setText("Time added");
        headerDateAdded.setPadding(5, 5, 5, 5);
        headerTableRow.addView(headerDateAdded);// add the column to the table row here

        TextView headerServerIP = new TextView(this);
        headerServerIP.setText("Server IP");
        headerServerIP.setPadding(5, 5, 5, 5);
        headerTableRow.addView(headerServerIP);// add the column to the table row here

        TextView headerServerPort = new TextView(this);
        headerServerPort.setText("Server Port");
        headerServerPort.setPadding(5, 5, 5, 5);
        headerTableRow.addView(headerServerPort);// add the column to the table row here

        tableLayout.addView(headerTableRow, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < serverDatabaseData.size(); i++) {
            ServerInfo currServerInfo = serverDatabaseData.get(i);

            TableRow tableRow = new TableRow(this);

            TextView dateAdded = new TextView(this);
            String unixTime = getUTCstring(currServerInfo.getUnixTimeAdded());
            dateAdded.setText(unixTime);
            dateAdded.setPadding(5, 5, 5, 5);
            tableRow.addView(dateAdded);// add the column to the table row here

            TextView serverIP = new TextView(this);
            serverIP.setText(currServerInfo.getServerIP());
            serverIP.setPadding(5, 5, 5, 5);
            tableRow.addView(serverIP);// add the column to the table row here

            TextView serverPort = new TextView(this);
            String currServerPort = String.valueOf(currServerInfo.getServerPort());
            serverPort.setText(currServerPort);
            serverPort.setPadding(5, 5, 5, 5);
            tableRow.addView(serverPort);// add the column to the table row here

            tableLayout.addView(tableRow, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        }

        serverDatabase.closeDB();
    }

}