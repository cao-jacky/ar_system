package fi.fivegear.remar.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import fi.fivegear.remar.R;

public class settingServer extends Fragment {
    View view;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.settings_server_fragment, container, false);
        Toast.makeText(getActivity(), "First Fragment", Toast.LENGTH_LONG).show();
        return view;
    }

}
