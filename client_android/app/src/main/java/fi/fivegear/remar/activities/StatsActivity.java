package fi.fivegear.remar.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.icu.text.SimpleDateFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import fi.fivegear.remar.R;
import fi.fivegear.remar.helpers.CSVWriter;
import fi.fivegear.remar.helpers.DatabaseHelper;

import static fi.fivegear.remar.Constants.TAG;

public class StatsActivity extends Activity {
    EditText statsSessionNumber;
    SharedPreferences sharedPreferencesSession;
    String currSessionNumber;

    private static SQLiteDatabase db;

    private TableLayout logTableLayout;

    private TextView statsRTT, statsRequests, statsResults;
    private TextView statsTotalPeriod, statsMeasurementBegin, statsMeasurementEnd;
    private int numRequests;

    private String currTimeString;
    private String currType;
    private Integer currFrameID;

    private Button setSession, statsExport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        sharedPreferencesSession = getSharedPreferences("currSetupSettings", Context.MODE_PRIVATE);
        currSessionNumber = sharedPreferencesSession.getString("currSessionNumber", "0");

        db = this.openOrCreateDatabase("remarManager", MODE_PRIVATE, null);
        logTableLayout = findViewById(R.id.statsLog);

        statsSessionNumber = findViewById(R.id.statsSessionNumber);
        statsSessionNumber.setGravity(Gravity.CENTER_HORIZONTAL);
        statsSessionNumber.setText(currSessionNumber);

        statsSessionNumber.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                changeSessionStats(db, logTableLayout);
                return true;
            }
            return false;
        });

        setStatsActivity(currSessionNumber, db, logTableLayout);

        setSession = findViewById(R.id.statsSessionConfirm);
        setSession.setOnClickListener(v -> changeSessionStats(db, logTableLayout));

        statsExport = findViewById(R.id.statsExport);
        statsExport.setOnClickListener(v -> {
            String currSessionToExport = String.valueOf(statsSessionNumber.getText());

            sharedPreferencesSession = getSharedPreferences("currSetupSettings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferencesSession.edit();
            editor.putString("sessionToExport", currSessionToExport);
            editor.apply();

            statsExporter();
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

        if (requestsFrameID.size() > 500) {
            TextView statusText = findViewById(R.id.textView13);
            statusText.setText(">500 requests, not calculating stats on device");
        }
        if (requestsFrameID.size() != 0 && requestsFrameID.size() <= 500 ) {
            while (results.moveToNext()) {
                resultsFrameID.add(results.getInt(3));
                unixTimeReceived.add(results.getString(4));
            }

            // matching results to the requests lists
            ArrayList<Integer> matchedRequestsFrameID = new ArrayList<>(requestsFrameID);
            matchedRequestsFrameID.retainAll(resultsFrameID);

            // go through requestsFrameID and resultsFrameID and use indexOf on requestsFrameID
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
            statsRTT = findViewById(R.id.statsRTT);
            statsRTT.setText(String.valueOf(avgCommTime));

            statsRequests = findViewById(R.id.statsRequests);
            numRequests = unixTimeSent.size();
            statsRequests.setText(String.valueOf(numRequests));

            statsResults = findViewById(R.id.statsResults);
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
            String periodEnd = sortedCombinedTimes.get(sortedCombinedTimes.size() - 1);

            double totalPeriodTime = Double.valueOf(periodEnd) - Double.valueOf(periodBegin);
            totalPeriodTime = totalPeriodTime / 1000;
            statsTotalPeriod = findViewById(R.id.statsTotalPeriod);
            statsTotalPeriod.setText(String.valueOf(totalPeriodTime));

            statsMeasurementBegin = findViewById(R.id.statsMeasurementBegin);
            statsMeasurementBegin.setText(String.valueOf(getUTCstring(periodBegin)));

            statsMeasurementEnd = findViewById(R.id.statsMeasurementEnd);
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
        else {
            TextView statusText = findViewById(R.id.textView13);
            statusText.setText("No requests or result entries");
        }


    }

    private void changeSessionStats(SQLiteDatabase db, TableLayout logTableLayout) {
        statsSessionNumber = findViewById(R.id.statsSessionNumber);
        String selectedSession = String.valueOf(statsSessionNumber.getText());

        sharedPreferencesSession = getSharedPreferences("currSetupSettings", Context.MODE_PRIVATE);
        currSessionNumber = sharedPreferencesSession.getString("currSessionNumber", "0");

        if (Integer.valueOf(selectedSession) <= Integer.valueOf(currSessionNumber)) {
            int count = logTableLayout.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = logTableLayout.getChildAt(i);
                if (child instanceof TableRow) ((ViewGroup) child).removeAllViews();
            }
            setStatsActivity(selectedSession, db, logTableLayout);
        } else {
            Toast.makeText(this, "Session number invalid", Toast.LENGTH_SHORT).show();
        }

    }

    public class ExportDatabaseCSVTask extends AsyncTask<String, Void, Boolean> {

        private final ProgressDialog dialog = new ProgressDialog(StatsActivity.this);
        DatabaseHelper dbhelper;
        @Override
        protected void onPreExecute() {
            this.dialog.setMessage("Exporting database tables");
            this.dialog.show();
            dbhelper = new DatabaseHelper(StatsActivity.this);
        }

        protected Boolean doInBackground(final String... args) {
            sharedPreferencesSession = getSharedPreferences("currSetupSettings", Context.MODE_PRIVATE);
            String sessionToExport = sharedPreferencesSession.getString("sessionToExport", "0");

            File exportDir = new File(Environment.getExternalStorageDirectory(), "/ReMAR/");
            if (!exportDir.exists()) { exportDir.mkdirs(); }

            File requestsFile = new File(exportDir, "remar_session_" + sessionToExport + "_requests.csv");
            File resultsFile = new File(exportDir, "remar_session_" + sessionToExport + "_results.csv");

            try {
                // writing requests file
                requestsFile.createNewFile();
                CSVWriter csvWriteRequests = new CSVWriter(new FileWriter(requestsFile));
                Cursor requests = db.rawQuery("SELECT * FROM request_items WHERE sessionID = ?; ",
                        new String[]{sessionToExport});
                csvWriteRequests.writeNext(requests.getColumnNames());
                while(requests.moveToNext()) {
                    String arrStr[]=null;
                    String[] mySecondStringArray = new String[requests.getColumnNames().length];
                    for(int i=0;i<requests.getColumnNames().length;i++)
                    {
                        mySecondStringArray[i] = requests.getString(i);
                    }
                    csvWriteRequests.writeNext(mySecondStringArray);
                }
                csvWriteRequests.close();
                requests.close();

                resultsFile.createNewFile();
                CSVWriter csvWriteResults = new CSVWriter(new FileWriter(resultsFile));
                Cursor results = db.rawQuery("SELECT * FROM result_items WHERE sessionID = ?; ",
                        new String[]{sessionToExport});
                csvWriteResults.writeNext(results.getColumnNames());
                while(results.moveToNext()) {
                    String arrStr[]=null;
                    String[] mySecondStringArray = new String[results.getColumnNames().length];
                    for(int i=0;i<results.getColumnNames().length;i++)
                    {
                        mySecondStringArray[i] = results.getString(i);
                    }
                    csvWriteResults.writeNext(mySecondStringArray);
                }
                csvWriteResults.close();
                results.close();
                return true;
            } catch (IOException e) {
                System.out.println(e);
                return false;
            }
        }

        protected void onPostExecute(final Boolean success) {
            if (this.dialog.isShowing()) { this.dialog.dismiss(); }
            if (success) {
                Toast.makeText(StatsActivity.this, "Export successful", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(StatsActivity.this, "Export failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void statsExporter() {
        // exporting requests and results data into CSV file
        ActivityCompat.requestPermissions(StatsActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        new ExportDatabaseCSVTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
