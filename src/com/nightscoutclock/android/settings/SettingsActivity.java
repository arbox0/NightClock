package com.nightscoutclock.android.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;


public class SettingsActivity extends PreferenceActivity {
	
    @Override
    public void onCreate(Bundle icicle) {

    	
         super.onCreate(icicle);
       
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment(this)).commit();
    }
}