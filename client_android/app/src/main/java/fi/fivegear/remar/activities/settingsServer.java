package fi.fivegear.remar.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import fi.fivegear.remar.R;
import fi.fivegear.remar.helpers.DatabaseHelper;
import fi.fivegear.remar.models.ServerInfo;

public class settingsServer extends Activity {
    DatabaseHelper db;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_server);

        db = new DatabaseHelper(getApplicationContext());

        long currUnix = System.currentTimeMillis();
        ServerInfo si1 = new ServerInfo(currUnix, "0.0.0.0", 12345);
        long si1_id = db.createServerInfo(si1);

        //Log.d("Tag Count", "Tag Count: " + db.getAllServerInfo().get(0).getServerPort());

        db.closeDB();


    }
}