package fi.fivegear.remar.activities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;

import fi.fivegear.remar.R;
import fi.fivegear.remar.helpers.DatabaseHelper;

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
        Cursor requests = db.rawQuery("SELECT * FROM request_items WHERE recognitionID = ?; ",
                new String[]{currSessionNumber});
        Cursor results = db.rawQuery("SELECT * FROM result_items WHERE recognitionID = ?; ",
                new String[]{currSessionNumber});

//        ArrayList<String> results = new ArrayList<String>();
//        ArrayList<String> unixTimeSent = new
        while (results.moveToNext()) {
//            results.add(requests.getString(0)); // 0 is the first column
            Log.d("test", results.getString(0)+" "+results.getString(1));
        }
    }
}
