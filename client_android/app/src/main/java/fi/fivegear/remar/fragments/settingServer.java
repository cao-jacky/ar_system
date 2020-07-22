package fi.fivegear.remar.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import fi.fivegear.remar.R;
import fi.fivegear.remar.activities.presetsActivity;

public class settingServer extends Fragment {
    View view;
    Button editPresetsButton;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activity_settings_server, container, false);

        editPresetsButton = (Button) view.findViewById(R.id.editPresets);
        editPresetsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPresetsEdit();
            }
        });

        return view;
    }

    public void openPresetsEdit(){
        Intent intent = new Intent(getActivity(), presetsActivity.class);
        startActivity(intent);
    }

}
