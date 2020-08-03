package fi.fivegear.remar.activities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import fi.fivegear.remar.R;
import fi.fivegear.remar.helpers.DatabaseHelper;
import fi.fivegear.remar.models.RequestEntry;
import fi.fivegear.remar.models.ResultsEntry;

public class StatsActivity extends Activity {
    EditText statsSessionNumber;
    SharedPreferences sharedPreferencesSession;
    String currSessionNumber;

    private static SQLiteDatabase db;
    DatabaseHelper logDatabase;

    private TableLayout logTableLayout;

    private TextView statsRTT, statsRequests, statsResults;
    private TextView statsTotalPeriod, statsMeasurementBegin, statsMeasurementEnd;
    private int numRequests;

    private String currTimeString;
    private String currType;
    private Integer currFrameID;

    private Button setSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        sharedPreferencesSession = getSharedPreferences("currSessionSetting", Context.MODE_PRIVATE);
        currSessionNumber = sharedPreferencesSession.getString("currSessionNumber", "0");

        db = this.openOrCreateDatabase("remarManager", MODE_PRIVATE, null);
        logTableLayout = (TableLayout)findViewById(R.id.statsLog);

        statsSessionNumber = (EditText)findViewById(R.id.statsSessionNumber);
        statsSessionNumber.setGravity(Gravity.CENTER_HORIZONTAL);
        statsSessionNumber.setText(currSessionNumber);

        statsSessionNumber.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    changeSessionStats(db, logTableLayout);
                    return true;
                }
                return false;
            }
        });

        setStatsActivity(currSessionNumber, db, logTableLayout);

        setSession = (Button)findViewById(R.id.statsSessionConfirm);
        setSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeSessionStats(db, logTableLayout);
            }
        });


    }

    public static String getUTCstring(String dateString) {
        long dv = Long.valueOf(dateString);
        Date df = new java.util.Date(dv);
        String date = new SimpleDateFormat("dd/MM/YYYY HH:mm:ss.SSS").format(df);
        return date;
    }

    private void setStatsActivity(String currSessionNumber, SQLiteDatabase db, TableLayout tableLayout) {
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

        // SETTING STATS INTO THE CORRESPONDING FIELDS ON ACTIVITY
        statsRTT = (TextView)findViewById(R.id.statsRTT);
        statsRTT.setText(String.valueOf(avgCommTime));

        statsRequests = (TextView)findViewById(R.id.statsRequests);
        numRequests = unixTimeSent.size();
        statsRequests.setText(String.valueOf(numRequests));

        statsResults = (TextView)findViewById(R.id.statsResults);
        statsResults.setText(String.valueOf(unixTimeReceived.size()));

        // DEALING WITH THE SIMPLE LOG TABLE

        // Setting header row
        TableRow headerTableRow = new TableRow(this);

        TextView headerTime = new TextView(this);
        headerTime.setText("Time");
        headerTime.setPadding(0, 5, 0, 5);
        headerTableRow.addView(headerTime);// add the column to the table row here

        TextView headerLogItemType = new TextView(this);
        headerLogItemType.setText("Type");
        headerLogItemType.setPadding(5, 5, 0, 5);
        headerTableRow.addView(headerLogItemType);// add the column to the table row here

        TextView headerFrameId = new TextView(this);
        headerFrameId.setText("Frame ID");
        headerFrameId.setPadding(5, 5, 0, 5);
        headerTableRow.addView(headerFrameId);// add the column to the table row here

        TextView headerInfo = new TextView(this);
        headerInfo.setText("Info");
        headerInfo.setPadding(5, 5, 0, 5);
        headerTableRow.addView(headerInfo);// add the column to the table row here

        tableLayout.addView(headerTableRow, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // join unix time lists together
        ArrayList<String> combinedTimes = new ArrayList<>();
        combinedTimes.addAll(unixTimeSent);
        combinedTimes.addAll(unixTimeReceived);

        // join frameID  lists together
        ArrayList<Integer> combinedFrameIds = new ArrayList<>();
        combinedFrameIds.addAll(requestsFrameID);
        combinedFrameIds.addAll(resultsFrameID);

        // duplicating joined unix time lists and then sorting
        ArrayList<String> sortedCombinedTimes = new ArrayList<>(combinedTimes);
        matchedRequestsFrameID.retainAll(combinedTimes);

        Collections.sort(sortedCombinedTimes); // sort combined list

        // calculating total period
        String periodBegin = sortedCombinedTimes.get(0);
        String periodEnd = sortedCombinedTimes.get(sortedCombinedTimes.size()-1);

        double totalPeriodTime = Double.valueOf(periodEnd) - Double.valueOf(periodBegin);
        totalPeriodTime = totalPeriodTime / 1000;
        statsTotalPeriod = (TextView)findViewById(R.id.statsTotalPeriod);
        statsTotalPeriod.setText(String.valueOf(totalPeriodTime));

        statsMeasurementBegin = (TextView)findViewById(R.id.statsMeasurementBegin);
        statsMeasurementBegin.setText(String.valueOf(getUTCstring(periodBegin)));

        statsMeasurementEnd = (TextView)findViewById(R.id.statsMeasurementEnd);
        statsMeasurementEnd.setText(String.valueOf(getUTCstring(periodEnd)));

        for (String timeItem : sortedCombinedTimes) {
            int indexInCombinedTimes = combinedTimes.indexOf(timeItem);

            // checking if a request of result item
            if (indexInCombinedTimes <= numRequests) {
                currType = "RQ";
            } else {
                currType = "RS";
            }

            currTimeString = getUTCstring(timeItem);

            currFrameID = combinedFrameIds.get(indexInCombinedTimes);

            TableRow tableRow = new TableRow(this);

            TextView itemTime = new TextView(this);
            itemTime.setText(currTimeString);
            itemTime.setPadding(5, 0, 0, 5);
            tableRow.addView(itemTime);// add the column to the table row here

            TextView itemType = new TextView(this);
            itemType.setText(currType);
            itemType.setPadding(5, 5, 0, 5);
            tableRow.addView(itemType);// add the column to the table row here

            TextView itemFrameId = new TextView(this);
            String currServerPort = String.valueOf(currFrameID);
            itemFrameId.setText(currServerPort);
            itemFrameId.setPadding(5, 5, 0, 5);
            tableRow.addView(itemFrameId);// add the column to the table row here

            TextView itemInfo = new TextView(this);
//            itemFrameId.setText(currServerPort);
            itemFrameId.setPadding(5, 5, 0, 5);
            tableRow.addView(itemInfo);// add the column to the table row here

            tableLayout.addView(tableRow, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

    }

    private void changeSessionStats(SQLiteDatabase db, TableLayout logTableLayout) {
        statsSessionNumber = (EditText)findViewById(R.id.statsSessionNumber);
        String selectedSession = String.valueOf(statsSessionNumber.getText());

        int count = logTableLayout.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = logTableLayout.getChildAt(i);
            if (child instanceof TableRow) ((ViewGroup) child).removeAllViews();
        }
        setStatsActivity(selectedSession, db, logTableLayout);

    }

}
