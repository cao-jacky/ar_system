package fi.fivegear.remar.activities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;

import java.util.ArrayList;

import fi.fivegear.remar.R;

public class StatsActivity extends Activity {
    EditText statsSessionNumber;
    SharedPreferences sharedPreferencesSession;
    String currSessionNumber;

    private static SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        sharedPreferencesSession = getSharedPreferences("currSessionSetting", Context.MODE_PRIVATE);
        currSessionNumber = sharedPreferencesSession.getString("currSessionNumber", "0");

        statsSessionNumber = (EditText)findViewById(R.id.statsSessionNumber);
        statsSessionNumber.setGravity(Gravity.CENTER_HORIZONTAL);
        statsSessionNumber.setText(currSessionNumber);

        db = this.openOrCreateDatabase("remarManager", MODE_PRIVATE, null);
        setStatsActivity(currSessionNumber, db);

    }

    private void setStatsActivity(String sessionNumber, SQLiteDatabase db) {
        // query session, requests and results databases
        Cursor requests = db.rawQuery("SELECT * FROM request_items WHERE sessionID = ?; ",
                new String[]{currSessionNumber});
        Cursor results = db.rawQuery("SELECT * FROM result_items WHERE sessionID = ?; ",
                new String[]{currSessionNumber});

        // Initialising array lists
        ArrayList<String> unixTimeSent = new ArrayList<String>();
        ArrayList<Integer> requestsFrameID = new ArrayList<>();

        ArrayList<String> unixTimeReceived = new ArrayList<String>();
        ArrayList<Integer> resultsFrameID = new ArrayList<>();

        while (requests.moveToNext()) {
            requestsFrameID.add(requests.getInt(3));
            unixTimeSent.add(requests.getString(4));
        }

        while (results.moveToNext()) {
            resultsFrameID.add(results.getInt(3));
            unixTimeReceived.add(results.getString(4));
        }

        // matching results to the requests
        requestsFrameID.retainAll(resultsFrameID);
        System.out.println(requestsFrameID);


    }
}
