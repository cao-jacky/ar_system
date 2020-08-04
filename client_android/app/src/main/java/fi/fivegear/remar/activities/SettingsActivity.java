package fi.fivegear.remar.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import fi.fivegear.remar.R;

public class SettingsActivity extends Activity {
    TextView activityServer, activityAbout;

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

        activityAbout = (TextView) findViewById(R.id.settingAbout);
        activityAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettingsAbout();
            }
        });

    }

    public void openSettingsServer(){
        Intent intent = new Intent(this, settingsServer.class);
        startActivity(intent);
    }

    public void openSettingsAbout(){
        Intent intent = new Intent(this, settingsAbout.class);
        startActivity(intent);
    }
}

