package com.nightscoutclock.android.utils;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.ToggleButton;

public class TimePreference extends DialogPreference {

	LinearLayout l1;
	TimePicker tp;
	Set<String> days = null;

	public TimePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		days = pref.getStringSet("alarm_days", new HashSet<String>());
	}
	
	private String getWeekDay(int i){
		
		switch (i){
		case 0:
			return "Su";
		case 1:
			return "Mo";
		case 2:
			return "Tu";
		case 3:
			return "We";
		case 4:
			return "Th";
		case 5:
			return "Fr";
		case 6:
			return "Sa";
		}
		return null;
	}

	
	@Override
	protected View onCreateDialogView() {
		 
		    //all other dialog stuff (which dialog to display)

		    //this line is what you need:
		    //this.getDialog().getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);

		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
			     LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

			layoutParams.setMargins(0, -20, 0, 0);   
		l1 = new LinearLayout(getContext());
		l1.setOrientation(LinearLayout.VERTICAL);
		l1.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		tp = new TimePicker(getContext());
		tp.setLayoutParams(layoutParams);
		tp.setIs24HourView(true);
		HorizontalScrollView l2 = new HorizontalScrollView(getContext());
		l2.setLayoutParams(layoutParams);
		LinearLayout l3 = new LinearLayout(getContext());
		l3.setOrientation(LinearLayout.HORIZONTAL);
		l3.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		l1.addView(tp);
		
		for (int i = 0; i < 7; i++){
			ToggleButton tb = new ToggleButton(getContext());
			tb.setTextOn(getWeekDay(i));
			tb.setTextOff(getWeekDay(i));
			tb.setText(getWeekDay(i));
			tb.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
			if (i==0)
				tb.setTextColor(Color.RED);
			if (days.contains((String)tb.getText())){
				tb.setChecked(true);
			}else
				tb.setChecked(false);
			tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			        if (isChecked) {
			            days.add((String)buttonView.getText());
			        } else {
			        	if (days.contains((String)buttonView.getText()))
			        		days.remove((String)buttonView.getText());
			        }
			    }
			});
				l3.addView(tb);
		}
		
		l2.addView(l3);
		l1.addView(l2);
		try{
		String dateTime = null;
		if (PreferenceManager.getDefaultSharedPreferences(getContext()).contains(this.getKey()))
			dateTime = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(this.getKey(), null);
		
		if (dateTime != null){
			Integer iHour = Integer.parseInt(dateTime.split(":")[0]);
			Integer iMinute = Integer.parseInt(dateTime.split(":")[1]);
			tp.setCurrentHour(iHour);
			tp.setCurrentMinute(iMinute);
		}
		}catch(Exception e){
			
		}
		return l1;
	}
	
	@Override
	protected void onDialogClosed(boolean isOk) {
		if (isOk){
			PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(this.getKey(), tp.getCurrentHour() +":"+ tp.getCurrentMinute()).commit();
			PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putStringSet("alarm_days", days).commit();
		}
	}
	
}