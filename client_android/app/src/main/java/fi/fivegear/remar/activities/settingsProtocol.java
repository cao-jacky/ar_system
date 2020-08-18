package fi.fivegear.remar.activities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.ArrayList;

import fi.fivegear.remar.R;

public class settingsProtocol extends Activity {

    public SharedPreferences sharedPreferencesProtocol;

    private String currProtocol;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_protocol);

        RadioGroup protocolRadioGroup = findViewById(R.id.protocolSelection);

        sharedPreferencesProtocol = getSharedPreferences("currSetupSettings", Context.MODE_PRIVATE);

        // obtain currently selected string and set the radio group button to match that
        currProtocol = sharedPreferencesProtocol.getString("currProtocol", "UDP");
        int count = protocolRadioGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View o = protocolRadioGroup.getChildAt(i);
            if (o instanceof RadioButton) {
                RadioButton currRadioButton = (RadioButton)o;
                String buttonProtocolID = (String)currRadioButton.getText();
                if (buttonProtocolID.contains(currProtocol)) {
                    currRadioButton.setChecked(true);
                } else {
                    currRadioButton.setChecked(false);
                }
            }
        }

        protocolRadioGroup.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            int radioButtonID = radioGroup.getCheckedRadioButtonId();
            RadioButton radioButton = radioGroup.findViewById(radioButtonID);
            String selectedText = (String)radioButton.getText();

            // change selected protocol variable
            SharedPreferences.Editor sessionEditor = sharedPreferencesProtocol.edit();
            sessionEditor.putString("currProtocol", selectedText);
            sessionEditor.apply();
        });


    }
}
