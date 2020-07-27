package fi.fivegear.remar.activities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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

        // matching results to the requests lists
        ArrayList<Integer> matchedRequestsFrameID = new ArrayList<>(requestsFrameID);
        matchedRequestsFrameID.retainAll(resultsFrameID);

        // go through requestsFrameID and resultsFrameID and use indexOf on requestsFrameID
//        System.out.println(matchedRequestsFrameID.size() + " " +  resultsFrameID.size());

        ArrayList<Long> timeDifferences = new ArrayList<>();

        // finding corresponding unix time values and working out the difference
        for (int i = 0; i < matchedRequestsFrameID.size(); i++) {
            int requestIndex = requestsFrameID.indexOf(matchedRequestsFrameID.get(i));
            String timeSent = unixTimeSent.get(requestIndex);
            String timeReceived = unixTimeReceived.get(i);

            long timeDiff = Long.parseLong(timeReceived) - Long.parseLong(timeSent);
            timeDifferences.add(timeDiff);

        }

        // calculating average time for this particular session
        double sum = 0;
        for (long i : timeDifferences) {
            sum += i;
        }
        double avgCommTime = sum / timeDifferences.size();

        

    }
}
