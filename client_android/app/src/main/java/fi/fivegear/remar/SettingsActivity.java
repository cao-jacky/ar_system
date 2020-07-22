package fi.fivegear.remar;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import fi.fivegear.remar.R;
import fi.fivegear.remar.activities.settingsServer;

public class SettingsActivity extends Activity {
    TextView activityServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        activityServer = (TextView) findViewById(R.id.settingServer);
        activityServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettingsServer();
            }
        });

    }

    public void openSettingsServer(){
        Intent intent = new Intent(this, settingsServer.class);
        startActivity(intent);
    }
}

