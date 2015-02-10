package com.nightscoutclock.android.clock;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Messenger;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import ch.qos.logback.classic.Logger;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.charts.ScatterChart.ScatterShape;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.interfaces.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.Legend;
import com.github.mikephil.charting.utils.Legend.LegendPosition;
import com.github.mikephil.charting.utils.LimitLine;
import com.github.mikephil.charting.utils.XLabels;
import com.github.mikephil.charting.utils.XLabels.XLabelPosition;
import com.github.mikephil.charting.utils.YLabels;
import com.github.mikephil.charting.utils.YLabels.YLabelPosition;
import com.nightscoutclock.android.R;
import com.nightscoutclock.android.eula.Eula;
import com.nightscoutclock.android.eula.Eula.OnEulaAgreedTo;
import com.nightscoutclock.android.medtronic.Constants;
import com.nightscoutclock.android.record.Record;
import com.nightscoutclock.android.settings.SettingsActivity;
/**
 * 
 * @author lmmarguenda
 *
 */
public class CGMClock extends Activity implements OnSharedPreferenceChangeListener, OnEulaAgreedTo, OnChartValueSelectedListener{
	//private Logger log = (Logger)LoggerFactory.getLogger(CGMClock.class.getName());
	private Logger log = (Logger)LoggerFactory.getLogger(CGMClock.class.getName());
	public static String TRIGGER_CONFIGURATION_ACTION = "android.nigthwidget.action.TRIGGER_CONFIGURATION_ACTION";
	//public static Handler mHandlerWatchService = new Handler();

    private static final String TAG = CGMClock.class.getSimpleName();

    private Handler mHandler = new Handler();

    private int maxRetries = 20;
    private int retryCount = 0;

 
    private Menu menu = null;
    private Intent service = null;
   
    Messenger mService = null;
   // boolean keepServiceAlive = true;
    Boolean mHandlerActive = false;
   
    ActivityManager manager = null;
    final Context ctx = this;
    SharedPreferences settings = null;
    ScatterChart mChart = null;
    LineData ld = null;
    Handler mHandlerWatchAlarmClock = new Handler();
    ImageButton alarmButton = null;
    TextView time_text = null;
    SharedPreferences prefs = null;
    MediaPlayer mMediaPlayer = null;
	AudioManager mAudioManager;
	Vibrator vibrator = null;
	int userVolume;
	boolean vibrationActive = true;
	Activity baseActivity = null;
	Button bCloseAlarm = null;
	Button bMinutesMore = null;

    //All I'm really doing here is creating a simple activity to launch and maintain the service
    private Runnable updateDataView = new Runnable() {
        public void run() {
        	synchronized (mHandlerActive) {
        		if (!mHandlerActive)
        			return;
		        if (!isMyServiceRunning()) {
		            if (retryCount < maxRetries) {
		            	stopCGMServices();
		            	startCGMServices();    		
		                Log.i(TAG, "Starting service " + retryCount + "/" + maxRetries);
		                ++retryCount;
		            } else {
		                mHandler.removeCallbacks(updateDataView);
		                Log.i(TAG, "Unable to restart service, trying to recreate the activity");
		                //recreate();
		            }
		        } else {
		        	retryCount = 0;
			           
			            Record auxRecord =  CGMClock.this.loadClassFile(new File(getBaseContext().getFilesDir(), "saveClock.bin"));
			            try{
			            	
			            	SharedPreferences prefs = PreferenceManager
			        				.getDefaultSharedPreferences(getBaseContext());
			            	
			            	processRecord(auxRecord);
			            	
			            
			            	
			            }catch (Exception e){
			            	e.printStackTrace();
			            }
			            	            
		            
		        }
		        
		        mHandler.removeCallbacks(updateDataView);
	        	mHandler.postDelayed(updateDataView, 10000);
	        }
        }
    };
    protected void processRecord(Record record){
    	TextView arrow = (TextView)findViewById(R.id.arrow_id);
    	TextView sgv = (TextView)findViewById(R.id.sgv_id);
    	TextView mbg = (TextView)findViewById(R.id.mbg_value);
    	TextView mbgTime = (TextView)findViewById(R.id.mbg_time_id);
    	TextView mbgLabel = (TextView)findViewById(R.id.mbg_label);
    	ImageButton devBattery = (ImageButton)findViewById(R.id.devBattery);
    	TextView devBatteryText = (TextView)findViewById(R.id.device_battery_text_id);
    	TextView insulinLeftText = (TextView)findViewById(R.id.insulin_data_id);
    	ImageButton insulinLeft = (ImageButton)findViewById(R.id.resIcon);
    	TextView phoneLabel = (TextView)findViewById(R.id.phone_battery_label_id);
    	TextView phoneBatteryText = (TextView)findViewById(R.id.phone_battery_text_id);
    	ImageButton phoneBattery = (ImageButton)findViewById(R.id.phoneBattery);
    	TextView difference = (TextView)findViewById(R.id.difference);
    	TextView minuteId = (TextView)findViewById(R.id.minute_id);
    	
    	if (record.mbg == null){
    		mbg.setVisibility(View.GONE);
    		mbgTime.setVisibility(View.GONE);
    		mbgLabel.setVisibility(View.GONE);
    	}else{
    		mbg.setVisibility(View.VISIBLE);
    		mbgTime.setVisibility(View.VISIBLE);
    		mbgLabel.setVisibility(View.VISIBLE);
    		
    		mbg.setText(record.mbg);
    		mbgTime.setText(record.mbgTime);
    	}
    	
    	if (record.sgv == null){
    		sgv.setText("---");
    		sgv.setTextColor(Color.WHITE);
    		sgv.setPaintFlags(0);
    	}else{
    		sgv.setText(record.sgv);
    		sgv.setTextColor(record.colorSGV);
    		sgv.setPaintFlags(record.textSGVFlags);
    	}
    	if (record.sgvTime == null){
    		minuteId.setVisibility(View.GONE);
    		processGraphData();
    	}else{
    		minuteId.setVisibility(View.VISIBLE);
    		minuteId.setText(record.sgvTime);
    		minuteId.setTextColor(record.timeColor);
    	}
    	if (record.arrow == null){
    		arrow.setText("---");
    	}else{
    		arrow.setText(record.arrow);
    	}
    	if (record.insulinIcon < 0 || record.insulinLeft == null){
    		insulinLeft.setVisibility(View.GONE);
    		insulinLeftText.setVisibility(View.GONE);
    	}else{
    		insulinLeft.setVisibility(View.VISIBLE);
    		insulinLeftText.setVisibility(View.VISIBLE);
    		insulinLeft.setImageResource(record.insulinIcon);
    		insulinLeftText.setText(record.insulinLeft);
    	}
    	if (record.remoteBattery == null || record.remoteBatteryIcon < 0){
    		phoneBattery.setVisibility(View.GONE);
    		phoneBatteryText.setVisibility(View.GONE);
    		phoneLabel.setVisibility(View.GONE);
    	}else{
    		phoneBattery.setVisibility(View.VISIBLE);
    		phoneBatteryText.setVisibility(View.VISIBLE);
    		phoneLabel.setVisibility(View.VISIBLE);
    		phoneBattery.setImageResource(record.remoteBatteryIcon);
    		phoneBatteryText.setText(record.remoteBattery);
    	}
    	if (record.difference == null){
    		difference.setVisibility(View.GONE);
    	}else{
    		difference.setVisibility(View.VISIBLE);
    		difference.setText(record.difference);
    	}
    	if (record.batteryVoltage == null || record.batteryIcon < 0){
    		devBattery.setVisibility(View.GONE);
    		devBatteryText.setVisibility(View.GONE);
    	}else{
    		devBattery.setVisibility(View.VISIBLE);
    		devBatteryText.setVisibility(View.VISIBLE);
    		devBattery.setImageResource(record.batteryIcon);
    		devBatteryText.setText(record.batteryVoltage);
    	}
    	
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();
        //Refresh the status
        try{
        	Record auxRecord =  CGMClock.this.loadClassFile(new File(getBaseContext().getFilesDir(), "saveClock.bin"));
            
        	SharedPreferences prefs = PreferenceManager
    				.getDefaultSharedPreferences(getBaseContext());
        	processRecord(auxRecord);
        	processGraphData();
        }catch (Exception e){
        	e.printStackTrace();
        }
        
    }

    //Check to see if service is running
    private boolean isMyServiceRunning() {
       
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
        	if (isServiceAlive(service.service.getClassName()))
        		return true;
        }
        return false;
    }

    //Deserialize the EGVRecord (most recent) value
    public Record loadClassFile(File f) {
    	 ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(f));
            Object o = ois.readObject();
            ois.close();
            return (Record) o;
        } catch (Exception ex) {
            Log.w(TAG, " unable to loadEGVRecord");
            try{
	            if (ois != null)
	            	ois.close();
            }catch(Exception e){
            	Log.e(TAG, " Error closing ObjectInputStream");
            }
        }
        return new Record();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        this.menu = menu;
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void startCGMServices(){
    	
	    		if (!isMyServiceRunning())
	    			startService(new Intent(CGMClock.this, CGMClockUpdater.class));
    	
    	return;
    }
    
    private void stopCGMServices(){
    	
	    		
			stopService(new Intent(CGMClock.this, CGMClockUpdater.class));

    }
    
    private boolean isServiceAlive(String name){
    	
    			return CGMClockUpdater.class.getName().equals(name);
    		
	    
    }
    
    /**
     * Stop playing the sound.
     */
    public void stopSound(){
    	// reset the volume to what it was before we changed it.
	    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, userVolume, AudioManager.FLAG_PLAY_SOUND);
	    mMediaPlayer.stop();
	    mMediaPlayer.reset();

    }
    
    /**
     * release player resource.
     */
    public void releasePlayer(){
    	mMediaPlayer.release();
    }
    
    
    
    
    @Override
    protected void onDestroy() {
    	Log.i(TAG, "onDestroy called");
    	PreferenceManager.getDefaultSharedPreferences(getBaseContext()).unregisterOnSharedPreferenceChangeListener(this);
        synchronized (mHandlerActive) {
        	mHandler.removeCallbacks(updateDataView);
        	stopService(new Intent(CGMClock.this, CGMClockUpdater.class));//remove if keep alive
        	/*if (keepServiceAlive){
        		stopService(new Intent(CGMClock.this, CGMClockUpdater.class));
        		service = new Intent(this, CGMClockUpdater.class);
	            startService(service);
        	}*/
			mHandlerActive = false;
			SharedPreferences.Editor editor = getBaseContext().getSharedPreferences(Constants.PREFS_NAME, 0).edit();
			editor.putLong("lastDestroy", System.currentTimeMillis());
			editor.commit();
        	super.onDestroy();
		}
	   	if (mMediaPlayer != null){
	   		stopSound();
	   		if (vibrationActive)
	   			vibrator.cancel();
	   		mMediaPlayer = null;
	   	}
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		 try {
			
			 
			 if (key.equalsIgnoreCase("alarm_clock_active") || key.equalsIgnoreCase("alarm_days") || key.equalsIgnoreCase("alarm_clock_time")){
 				 time_text.setText(prefs.getString("alarm_clock_time", "00:00"));
				 if (prefs.getBoolean("alarm_clock_active", false)){
				    	Configuration config = getResources().getConfiguration();
				    
				    	if (config.smallestScreenWidthDp >= 600) {
				    		alarmButton.setImageResource(R.drawable.clock_icon_97);
				    	}else{
				    		alarmButton.setImageResource(R.drawable.clock_icon_72);
				    		
				    	}
				    	time_text.setPaintFlags(0);
				    	time_text.setTextColor(Color.WHITE);
				    }else{
				    	Configuration config = getResources().getConfiguration();
					    
				    	if (config.smallestScreenWidthDp >= 600) {
				    		alarmButton.setImageResource(R.drawable.clock_strike_icon_97);
				    	}else{
				    		alarmButton.setImageResource(R.drawable.clock_strike_icon_72);
				    		
				    	}
				    	time_text.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
				    	time_text.setTextColor(Color.GRAY);
				    }
				 
				 	int hour = 0;
					int minute = 0;
					if (prefs.contains("alarm_clock_time")){
		            	String time = prefs.getString("alarm_clock_time", "00:00");
		            	hour =  Integer.parseInt(time.split(":")[0]);
		            	minute =  Integer.parseInt(time.split(":")[1]);
		            }
					Calendar TIME = Calendar.getInstance();  
					long diferencia = TIME.getTimeInMillis()-SystemClock.uptimeMillis();
		            
		            if (!(hour > TIME.get(Calendar.HOUR_OF_DAY) || ((hour == TIME.get(Calendar.HOUR_OF_DAY)) && (minute > TIME.get(Calendar.MINUTE)))))
		            	TIME.set(Calendar.DAY_OF_MONTH, TIME.get(Calendar.DAY_OF_MONTH)+1);
		            else{
		            	TIME.set(Calendar.MINUTE, 0);  
			            TIME.set(Calendar.SECOND, 0);  
			            TIME.set(Calendar.MILLISECOND, 0);
		            }
		            if (prefs.contains("alarm_clock_time")){
		            	TIME.set(Calendar.HOUR_OF_DAY, hour);
		            	TIME.set(Calendar.MINUTE, minute);
		            }
		            mHandlerWatchAlarmClock.removeCallbacks(mCheckAlarmAction);    
					if (prefs.getBoolean("alarm_clock_active", false)){
						mHandlerWatchAlarmClock.postAtTime(mCheckAlarmAction, TIME.getTimeInMillis() - diferencia);
					}
				 
			 }
			 //If i do not
			 if (key.equals("IUNDERSTAND")){
				 if (!sharedPreferences.getBoolean("IUNDERSTAND", false)){
					 synchronized (mHandlerActive) {
				        	mHandler.removeCallbacks(updateDataView);
				        	mHandlerActive = false;
					 }
					 stopCGMServices();
				 }else{
					 startCGMServices();
					 mHandler.post(updateDataView);
				     mHandlerActive = true;
				 }
		     }
		 } catch (Exception e) {
			 StringBuffer sb1 = new StringBuffer("");
    		 sb1.append("EXCEPTION!!!!!! "+ e.getMessage()+" "+e.getCause());
    		 for (StackTraceElement st : e.getStackTrace()){
    			 sb1.append(st.toString()).append("\n");
    		 }
    		 Log.e("DexcomG4Activity_onSharedPreferenceChanged", sb1.toString());
    		
    			 log.error(sb1.toString());
    		 
		}
	}

	@Override
	public void onEulaAgreedTo() {
		//keepServiceAlive = true;
	}

	@Override
	public void onEulaRefusedTo() {
		//keepServiceAlive = false;
		
	}
	 @Override
	 public void onCreate(Bundle savedInstanceState) {
    	Log.i(TAG, "onCreate called");
    	//keepServiceAlive = Eula.show(this);
    	Eula.show(this);
        super.onCreate(savedInstanceState);
        baseActivity = this;
		 if (android.os.Build.VERSION.SDK_INT > 9) 
			{
			    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			    StrictMode.setThreadPolicy(policy);
			}
		 	prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		 	prefs.registerOnSharedPreferenceChangeListener(this);
		 	settings = getBaseContext().getSharedPreferences(Constants.PREFS_NAME, 0);
		 	  setContentView(R.layout.tablet_main);
		        manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		    alarmButton = (ImageButton)findViewById(R.id.imageButton1);
		    time_text = (TextView) findViewById(R.id.time);
		    time_text.setText(prefs.getString("alarm_clock_time", "00:00"));
		    if (prefs.getBoolean("alarm_clock_active", false)){
		    	Configuration config = getResources().getConfiguration();
		    
		    	if (config.smallestScreenWidthDp >= 600) {
		    		alarmButton.setImageResource(R.drawable.clock_icon_97);
		    	}else{
		    		alarmButton.setImageResource(R.drawable.clock_icon_72);
		    		
		    	}
		    	time_text.setPaintFlags(0);
		    	time_text.setTextColor(Color.WHITE);
		    }else{
		    	Configuration config = getResources().getConfiguration();
			    
		    	if (config.smallestScreenWidthDp >= 600) {
		    		alarmButton.setImageResource(R.drawable.clock_strike_icon_97);
		    	}else{
		    		alarmButton.setImageResource(R.drawable.clock_strike_icon_72);
		    		
		    	}
		    	time_text.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
		    	time_text.setTextColor(Color.GRAY);
		    }
		    bCloseAlarm = (Button) findViewById(R.id.button_close);
		    bMinutesMore = (Button) findViewById(R.id.button_moreminutes);
		    
		    bCloseAlarm.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View arg0) {
				
					bClosePressed();
				}
		    	
		    });
		    bMinutesMore.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					bMinutesMore.setVisibility(View.GONE);
			        bCloseAlarm.setVisibility(View.GONE);
					if (mMediaPlayer != null){
				   		stopSound();
				   		if (vibrationActive)
				   			vibrator.cancel();
				   		mMediaPlayer = null;
				   	}
					
					
			        mHandlerWatchAlarmClock.removeCallbacks(mCheckAlarmAction);    
					if (prefs.getBoolean("alarm_clock_active", false)){
						mHandlerWatchAlarmClock.postDelayed(mCheckAlarmAction, 300000);
					}
				}
		    });
           
      alarmButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				silenceAlarm();
				if (!prefs.getBoolean("alarm_clock_active", false)){
			    	Configuration config = getResources().getConfiguration();
			    
			    	if (config.smallestScreenWidthDp >= 600) {
			    		alarmButton.setImageResource(R.drawable.clock_icon_97);
			    	}else{
			    		alarmButton.setImageResource(R.drawable.clock_icon_72);
			    		
			    	}
			    	time_text.setPaintFlags(0);
			    	time_text.setTextColor(Color.WHITE);
			    }else{
			    	Configuration config = getResources().getConfiguration();
				    
			    	if (config.smallestScreenWidthDp >= 600) {
			    		alarmButton.setImageResource(R.drawable.clock_strike_icon_97);
			    	}else{
			    		alarmButton.setImageResource(R.drawable.clock_strike_icon_72);
			    		
			    	}
			    	time_text.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
			    	time_text.setTextColor(Color.GRAY);
			    }
				
				prefs.edit().putBoolean("alarm_clock_active", !prefs.getBoolean("alarm_clock_active", false)).commit();
				
				int hour = 0;
				int minute = 0;
				if (prefs.contains("alarm_clock_time")){
	            	String time = prefs.getString("alarm_clock_time", "00:00");
	            	hour =  Integer.parseInt(time.split(":")[0]);
	            	minute =  Integer.parseInt(time.split(":")[1]);
	            }
				Calendar TIME = Calendar.getInstance();  
				long diferencia = TIME.getTimeInMillis()-SystemClock.uptimeMillis();
	            
	            if (!(hour < TIME.get(Calendar.HOUR_OF_DAY) || ((hour == TIME.get(Calendar.HOUR_OF_DAY)) && (minute < TIME.get(Calendar.MINUTE)))))
	            	TIME.set(Calendar.DAY_OF_MONTH, TIME.get(Calendar.DAY_OF_MONTH)+1);
	            
	            if (prefs.contains("alarm_clock_time")){
	            	TIME.set(Calendar.HOUR_OF_DAY, hour);
	            	TIME.set(Calendar.MINUTE, minute);
	            }else{
	            	TIME.set(Calendar.MINUTE, 0);  
		            TIME.set(Calendar.SECOND, 0);  
		            TIME.set(Calendar.MILLISECOND, 0);
	            }
	            mHandlerWatchAlarmClock.removeCallbacks(mCheckAlarmAction);    
				if (prefs.getBoolean("alarm_clock_active", false)){
					mHandlerWatchAlarmClock.postAtTime(mCheckAlarmAction, TIME.getTimeInMillis() - diferencia);
				}
				
			}
	    });
                mChart = (ScatterChart) findViewById(R.id.chart);
		        mChart.setDescription("");

		        Typeface tf = Typeface.createFromAsset(getAssets(), "OpenSans-Regular.ttf");
		        mChart.setValueTypeface(tf);

		        mChart.setOnChartValueSelectedListener(this);

		        mChart.setDrawGridBackground(false);
		        mChart.setDrawLegend(true);

		        mChart.setTouchEnabled(true);
		        mChart.setHighlightEnabled(true);
		        mChart.setDrawYValues(true);
		        

		        // enable scaling and dragging
		        mChart.setDragEnabled(true);
		        mChart.setScaleEnabled(true);
		        mChart.setValueTextColor(Color.WHITE);

		        mChart.setPinchZoom(true);
		        
		       
		        XLabels xlabels = mChart.getXLabels();
		        xlabels.setTextColor(getResources().getColor(android.R.color.white));
		        YLabels ylabels = mChart.getYLabels();
		        ylabels.setTextColor(getResources().getColor(android.R.color.white));
		        xlabels.setPosition(XLabelPosition.BOTTOM);
		        ylabels.setPosition(YLabelPosition.RIGHT);
		        ylabels.setTypeface(tf);
		        xlabels.setTypeface(tf);
		        
            if (!prefs.getBoolean("IUNDERSTAND", false)){
    			 stopCGMServices();
    		 }else{
    	        mHandler.post(updateDataView);
    	        mHandlerActive = true;
    		 }
	        mChart.invalidate();
	    }

	 private void silenceAlarm(){
		 bMinutesMore.setVisibility(View.GONE);
	        bCloseAlarm.setVisibility(View.GONE);
			if (mMediaPlayer != null){
		   		stopSound();
		   		if (vibrationActive)
		   			vibrator.cancel();
		   		mMediaPlayer = null;
		   	}
	 }
	 private void bClosePressed(){
		 	
		 silenceAlarm();
			Calendar TIME = Calendar.getInstance();  
			long diferencia = TIME.getTimeInMillis()-SystemClock.uptimeMillis();
	            TIME.set(Calendar.MINUTE, 0);  
	            TIME.set(Calendar.SECOND, 0);  
	            TIME.set(Calendar.MILLISECOND, 0); 
	            TIME.set(Calendar.DAY_OF_MONTH, TIME.get(Calendar.DAY_OF_MONTH)+1);
	            if (prefs.contains("alarm_clock_time")){
	            	String time = prefs.getString("alarm_clock_time", "00:00");
	            	TIME.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.split(":")[0]));
	            	TIME.set(Calendar.MINUTE, Integer.parseInt(time.split(":")[1]));
	            }
	        mHandlerWatchAlarmClock.removeCallbacks(mCheckAlarmAction);    
			if (prefs.getBoolean("alarm_clock_active", false)){
				
				mHandlerWatchAlarmClock.postAtTime(mCheckAlarmAction, TIME.getTimeInMillis() -diferencia);
			}
	 }
	private void processGraphData(){
		JSONArray mbgRecords =  new JSONArray();
		JSONArray sgvRecords =  new JSONArray();
		JSONArray mbgRecordsAux =  new JSONArray();
		JSONArray sgvRecordsAux =  new JSONArray();
		 try {
			mbgRecords = new JSONArray(settings.getString("mbgRecords", "[]"));
		} catch (JSONException e) {
			
			log.error("error",e);
		}
		 try {
			 sgvRecords = new JSONArray(settings.getString("sgvRecords", "[]"));
			} catch (JSONException e) {
				
				log.error("error",e);
			}
		 JSONObject firstMbgRecord = null;
		 JSONObject firstSgvRecord = null;
		 JSONObject lastMbgRecord = null;
		 JSONObject lastSgvRecord = null;
		 try {
			 
			 if (mbgRecords.length() > 0){
				 	for (int i = 0 ; i < mbgRecords.length(); i++){
				 		JSONObject aux = mbgRecords.getJSONObject(i);
				 		if (aux == null || !aux.has("date") || !aux.has("mbg")){
				 			continue;
				 		}
				 		mbgRecordsAux.put(aux);
				 	}
				 	if (mbgRecords.length() != mbgRecordsAux.length()){
				 		mbgRecords = mbgRecordsAux;
				 		settings.edit().putString("mbgRecords", mbgRecords.toString()).commit();
				 	}
					firstMbgRecord = mbgRecords.getJSONObject(0);
					lastMbgRecord = mbgRecords.getJSONObject(mbgRecords.length() -1);
				}	
		} catch (Exception e) {
			log.error("error",e);
		}
		 try {
			 if (sgvRecords.length() > 0){
				 for (int i = 0 ; i < sgvRecords.length(); i++){
				 		JSONObject aux = sgvRecords.getJSONObject(i);
				 		if (aux == null || !aux.has("date") || !aux.has("sgv")){
				 			continue;
				 		}
				 		sgvRecordsAux.put(aux);
				 	}
				 	if (sgvRecords.length() != sgvRecordsAux.length()){
				 		sgvRecords = sgvRecordsAux;
				 		settings.edit().putString("sgvRecords", sgvRecords.toString()).commit();
				 	}
				 firstSgvRecord = sgvRecords.getJSONObject(0);
				 lastSgvRecord = sgvRecords.getJSONObject(sgvRecords.length() -1);
				}	
		} catch (Exception e) {
			log.error("error",e);
		}
		 long firstTime = 0;
		 long lastTime = 0;
		
	     Long firstSGVTime = null;
		 Long firstMBGTime = null;
		 Long lastSGVTime = null;
		 Long lastMBGTime = null;
		 if (firstSgvRecord != null && firstMbgRecord != null){
			 
			 try {
				 firstSGVTime = firstSgvRecord.getLong("date");
				 	
			} catch (Exception e) {
				log.error("error",e);
			}
			 try {
				 firstMBGTime = firstMbgRecord.getLong("date");
			 
			 } catch (Exception e) {
					log.error("error",e);
			 }
			 if (firstMBGTime != null && firstSGVTime != null){
				 if (firstMBGTime > firstSGVTime){
					 firstTime = firstSGVTime;
				 }else{
					 firstTime = firstMBGTime;
				 }
			 }else{
				 if (firstMBGTime == null){
					 firstTime = firstSGVTime;
				 }else{
					 firstTime = firstMBGTime;
				 }
			 }
			 try {
				 lastSGVTime = lastSgvRecord.getLong("date");
				 	
			} catch (Exception e) {
				log.error("error",e);
			}
			 try {
				 lastMBGTime = lastMbgRecord.getLong("date");
			 
			 } catch (Exception e) {
					log.error("error",e);
			 }
			 if (lastMBGTime != null && lastSGVTime != null){
				 if (lastMBGTime < lastSGVTime){
					 lastTime = lastSGVTime;
				 }else{
					 lastTime = lastMBGTime;
				 }
			 }else{
				 if (lastMBGTime == null){
					 lastTime = lastSGVTime;
				 }else{
					 lastTime = lastMBGTime;
				 }
			 }
		 }else{
			 if (firstSgvRecord != null){
				 try {
					 firstTime = firstSgvRecord.getLong("date");
					 	
				} catch (Exception e) {
					log.error("error",e);
				}
			 }
			 if (lastSgvRecord != null){
				 try {
					 lastTime = lastSgvRecord.getLong("date");
					 	
				} catch (Exception e) {
					log.error("error",e);
				}
			 }
			 if (lastMbgRecord != null){
				 try {
					 lastTime = lastMbgRecord.getLong("date");
				 
				 } catch (Exception e) {
						log.error("error",e);
				 }
			 }
			 if (firstMbgRecord != null){
				 try {
					 firstTime = firstMbgRecord.getLong("date");
				 
				 } catch (Exception e) {
						log.error("error",e);
				 }
				 
			 }
			 
		 }
		 
		 if (lastTime < firstTime && (sgvRecords.length() <= 0 && mbgRecords.length() <= 0))
			 return;
		 
		  ArrayList<String> xVals = new ArrayList<String>();
		  ArrayList<Long> xValsTime = new ArrayList<Long>();
	        Calendar calendar= Calendar.getInstance();
	        calendar.setTimeInMillis(firstTime);
	       int index = 0;
	       int indexMBG = 0;
	       long cutValues = Constants.MAX_GRAPH_ITEMS * Constants.TIME_5_MIN_IN_MS;
	       boolean lastIteration = false;
	       Long cutTime = lastTime - cutValues;
	       if (firstTime < cutTime){
	    	   firstTime = cutTime;
	       }
	       while (firstTime <= lastTime && !lastIteration){
	    	   		if (firstTime == lastTime)
	    	   			lastIteration = true;
	    	   		JSONObject sgvRecord = null;
	    	   		JSONObject mbgRecord = null;
	    	   		long timeSgvRecord = 0l;
		    		 long timeMbgRecord = 0l;
	    	   	 if (index < sgvRecords.length()){
	    	   		 try {
	    	   			sgvRecord = sgvRecords.getJSONObject(index);
		    	   		 timeSgvRecord = sgvRecord.getLong("date");	
					} catch (Exception e) {
						log.error("error", e);
					}
	    	   		 
	    	   	 }
	    	   	 if (indexMBG < mbgRecords.length()){
		    		 try {
						mbgRecord = mbgRecords.getJSONObject(indexMBG);
						timeMbgRecord = mbgRecord.getLong("date");
					} catch (JSONException e) {
						log.error("error", e);
					}
		    		 
	    	   	 }
	    		 boolean firstTimeSetted = false;
	    		 if (sgvRecord != null){
		    		 if (firstTime == timeSgvRecord || Math.abs(firstTime - timeSgvRecord) <= 240000)
		    		 {
		    			 firstTimeSetted = true;
		    			 firstTime = timeSgvRecord;
		    			 if (firstTime >= lastTime)
			    	   			lastIteration = true;
		    			 calendar.setTimeInMillis(firstTime);
		    			 index++;
		    		 }
	    		 }
	    		 if (mbgRecord != null){
		    		 if (firstTime == timeSgvRecord || Math.abs(firstTime - timeSgvRecord) <= 240000)
		    		 {	
		    			 if  (!firstTimeSetted)
		    				 firstTime = timeMbgRecord;
		    			 if (firstTime >= lastTime)
			    	   			lastIteration = true;
		    			 calendar.setTimeInMillis(firstTime);
		    			 indexMBG++;
		    		 }
	    		 }
	        	Date time = calendar.getTime();
	        	 SimpleDateFormat formatter = new SimpleDateFormat("dd,kk:mm", Locale.US);
			        String formattedDate = formatter.format(time);
			        log.info(formattedDate);
			    
	            xVals.add(formattedDate);
	            xValsTime.add(time.getTime());
	            
	            firstTime += 300000;
	            if (firstTime > lastTime)
	            	firstTime = lastTime;
	            calendar.setTimeInMillis(firstTime);
	        }
	       cutTime = null;
	       List<String> auxVals = null;
	       ArrayList<String> sVals = new ArrayList<String>();
	       if (xVals.size() > Constants.MAX_GRAPH_ITEMS){
	    	   auxVals =  xVals.subList(xVals.size()-21, xVals.size());
	    	   
	    	   for (String value : auxVals)
		    	   sVals.add(value);
	    	  
	       }
	       xVals.clear();
	      xVals = sVals;
	        ArrayList<Entry> yVals1 = new ArrayList<Entry>();
	        ArrayList<Entry> yVals2 = new ArrayList<Entry>();
	    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	    	String metric = prefs.getString("metric_preference", "1");
	    	int m_type = 0;
	    	if (metric.equalsIgnoreCase("2"))
	    		m_type = 1;
	    	index =xValsTime.size() - 1;
	        Long current_xValueTime = (long) 0;
	        for (int i = mbgRecords.length() -1; i >= 0 && index >= 0; i--) {
	        	try {
	        		current_xValueTime = xValsTime.get(index);
	        		JSONObject mbgRecord = mbgRecords.getJSONObject(i);
	        		Long currentTime = mbgRecord.getLong("date");
	        		boolean bFound = false;
		        	if (cutTime == null){
		        		 while(!bFound && index >= 0){
		        			  current_xValueTime = xValsTime.get(index);
				        	  if (current_xValueTime == currentTime || Math.abs(current_xValueTime - currentTime) <= 150000 || i == 0 ){
				        		bFound = true;  
				        		if (m_type == 0)
				        			yVals1.add(new Entry(mbgRecord.getInt("mbg"), index));
				        		else
				        			yVals1.add(new Entry((float)mbgRecord.getDouble("mbg"), index));
				        	  }
				        	  index--;
	        			 }
		        	}else{
		        		if (mbgRecord.getLong("date") >= cutTime){
		        			 while(!bFound && index >= 0){
				        		  current_xValueTime = xValsTime.get(index);
					        	  if (current_xValueTime == currentTime || Math.abs(current_xValueTime - currentTime) <= 150000 || i == 0 ){
					        		bFound = true;  
					        		if (m_type == 0)
					        			yVals1.add(new Entry(mbgRecord.getInt("mbg"), index));
					        		else
					        			yVals1.add(new Entry((float)mbgRecord.getDouble("mbg"), index));
					        	  }
					        	  index--;
		        			 }
		        		}
		        	}
				} catch (Exception e) {
					log.error("error",e);
				}
	        	
	        }
	        index = xValsTime.size() - 1;
	        current_xValueTime = (long) 0;
	        for (int i = sgvRecords.length() - 1; i >=0 && index >= 0; i--) {
	        	try {
	        		current_xValueTime = xValsTime.get(index);
	        		JSONObject sgvRecord = sgvRecords.getJSONObject(i);
	        		Long currentTime = sgvRecord.getLong("date");
	        		boolean bFound = false;
		        	if (cutTime == null){
		        	  while(!bFound && index >= 0){
		        		  current_xValueTime = xValsTime.get(index);
			        	  if (current_xValueTime == currentTime || Math.abs(current_xValueTime - currentTime) <= 150000 || i == 0 ){
			        		bFound = true;  
			        		if (m_type == 0){
			        			yVals2.add(0,new Entry(sgvRecord.getInt("sgv"), index));
			        		}else
			        			yVals2.add(0,new Entry((float)sgvRecord.getDouble("sgv"),index));
			        	  }
			        	  index--;
		        	  }
		        		
		        	}else{
		        		if (sgvRecord.getLong("date") >= cutTime){
		        			while(!bFound && index >= 0){
		        				current_xValueTime = xValsTime.get(index);
					        	  if (current_xValueTime == currentTime || Math.abs(current_xValueTime - currentTime) <= 150000 || i == 0 ){
					        		bFound = true;  
					        		if (m_type == 0){
					        			yVals2.add(0,new Entry(sgvRecord.getInt("sgv"), index));
					        		}else
					        			yVals2.add(0,new Entry((float)sgvRecord.getDouble("sgv"),index));
					        	  }
					        	  index--;
				        	  }
		        		}
		        	}	
				} catch (Exception e) {
					log.error("error",e);
				}
	        	
	        }
	    	ld = new LineData(xVals);
            if (prefs.getBoolean("alarms_active", true)){
            	if (prefs.getBoolean("sound_alarm", true)){
            		String slower = prefs.getString("lower_alarm_color", "70");
        			String sUpper =	prefs.getString("upper_alarm_color", "170");
            		if (m_type == 0){
            			LimitLine ll = new LimitLine(Integer.parseInt(sUpper));
                        ll.setLineColor(Color.RED);
                        ll.setLineWidth(2f);
                        ld.addLimitLine(ll);
                        ll = new LimitLine(Integer.parseInt(slower));
                        ll.setLineColor(Color.RED);
                        ll.setLineWidth(2f);
                        ld.addLimitLine(ll);
            		}else{
            			LimitLine ll = new LimitLine(Float.parseFloat(sUpper));
                        ll.setLineColor(Color.RED);
                        ll.setLineWidth(2f);
                        ld.addLimitLine(ll);
                        ll = new LimitLine(Float.parseFloat(slower));
                        ll.setLineColor(Color.RED);
                        ll.setLineWidth(2f);
                        ld.addLimitLine(ll);
            		}                	
            	}
            	
            	if (prefs.getBoolean("sound_warning", true)){
                	String slower = prefs.getString("lower_warning_color", "80");
        			String sUpper =	prefs.getString("upper_warning_color", "140");
            		if (m_type == 0){
            			LimitLine ll = new LimitLine(Integer.parseInt(sUpper));
                        ll.setLineColor(Constants.YELLOW);
                        ll.setLineWidth(1f);
                        ld.addLimitLine(ll);
                        ll = new LimitLine(Integer.parseInt(slower));
                        ll.setLineColor(Constants.YELLOW);
                        ll.setLineWidth(1f);
                        ld.addLimitLine(ll);
            		}else{
            			LimitLine ll = new LimitLine(Float.parseFloat(sUpper));
                        ll.setLineColor(Constants.YELLOW);
                        ll.setLineWidth(1f);
                        ld.addLimitLine(ll);
                        ll = new LimitLine(Float.parseFloat(slower));
                        ll.setLineColor(Constants.YELLOW);
                        ll.setLineWidth(1f);
                        ld.addLimitLine(ll);
            		}                	
            	}
	
            }
	        
	        // create a dataset and give it a type
	        ScatterDataSet set1 = new ScatterDataSet(yVals1, "MBG");
	        set1.setScatterShape(ScatterShape.CIRCLE);
	        set1.setColor(Color.RED);
	        ScatterDataSet set2 = new ScatterDataSet(yVals2, "SVG");
	        set2.setScatterShape(ScatterShape.CIRCLE);
	        set2.setColor(Color.WHITE);

	        set1.setScatterShapeSize(8f);
	        set2.setScatterShapeSize(8f);

	        ArrayList<ScatterDataSet> dataSets = new ArrayList<ScatterDataSet>();
	        dataSets.add(set1); // add the datasets
	        dataSets.add(set2);

	        // create a data object with the datasets
	        ScatterData data = new ScatterData(xVals, dataSets);
	        data.addLimitLines(ld.getLimitLines());
	        mChart.setData(data);
	        mChart.invalidate();
	        Legend l = mChart.getLegend();
	        Typeface tf = Typeface.createFromAsset(getAssets(), "OpenSans-Regular.ttf");
	        if (l != null){
		        l.setPosition(LegendPosition.RIGHT_OF_CHART);
		        l.setTypeface(tf);
		        l.setTextColor(Color.WHITE);
	        }
	        mChart.invalidate();
	}
	 
	@Override
	public void onValueSelected(Entry e, int dataSetIndex) {
		
	}

	@Override
	public void onNothingSelected() {

		
	}
	
	/* @Override  
	    public void onDisabled(Context context)  
	    {  
		 log.info("onDisabled");
		 SharedPreferences settings = context.getSharedPreferences("widget_prefs", 0);
		 AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

		 int[] appWidgetIDs = appWidgetManager
		     .getAppWidgetIds(new ComponentName(context, CGMClock.class)); 
		 if (appWidgetIDs.length > 0)
		 {
			 log.info("DISABLE Length "+appWidgetIDs.length);
			 String key = String.format(Locale.US,"appwidget%d_configured", appWidgetIDs[0]);
			 settings.edit().remove("widget_ops_"+appWidgetIDs[0]).commit();
	         settings.edit().remove("widget_configuring_"+appWidgetIDs[0]).commit();
			 if (settings.contains(key))
				 settings.edit().remove(key).commit();
		 }
		
		 SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	        // Perform this loop procedure for each App Widget that belongs to this provider
	        SharedPreferences.Editor editor= prefs.edit();
	        editor.putBoolean("widgetEnabled", false);
	        editor.remove("widget_uuid");
	        editor.commit();
	        final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);  
	  
	        if (service == null){
				 log.info("ISNULL!!!!");
			 }else
				 m.cancel(service);
	        final Intent in = new Intent(context, CGMClockUpdater.class);  
            int i = 100;
            
            boolean alarmUp = (PendingIntent.getService(context, 27, in, 
			        PendingIntent.FLAG_NO_CREATE) != null);
            while (alarmUp && i > 0){
            	log.warn("I AM KILLING SERVICES " +i);
            	i--;
            	PendingIntent pI = PendingIntent.getService(context, 27, in, 
    			        PendingIntent.FLAG_NO_CREATE);
            	if (pI != null)
            		pI.cancel();
            	m.cancel(PendingIntent.getService(context, 27, in, 
    			        PendingIntent.FLAG_NO_CREATE));
            	alarmUp = (PendingIntent.getService(context, 27, in, 
    			        PendingIntent.FLAG_NO_CREATE) != null);
            	service = null;
            }
            log.warn("I HAVE KILLED SERVICES ");
            mHandlerWatchService.removeCallbacks(mWatchAction);
	    }  */
	private String getWeekDay(int i){
		
		switch (i){
		case Calendar.SUNDAY:
			return "Su";
		case Calendar.MONDAY:
			return "Mo";
		case Calendar.TUESDAY:
			return "Tu";
		case Calendar.WEDNESDAY:
			return "We";
		case Calendar.THURSDAY:
			return "Th";
		case Calendar.FRIDAY:
			return "Fr";
		case Calendar.SATURDAY:
			return "Sa";
		}
		return null;
	}
	private Runnable mCheckAlarmAction = new Runnable(){

		@Override
		public void run() {
		
			  if (!prefs.getBoolean("alarm_clock_active", false)){
				  
				  return;
			  }
			  
			  Set<String> days = null;
			  days = prefs.getStringSet("alarm_days", new HashSet<String>());
			  Calendar cal = Calendar.getInstance();
			  //TODO: OBTEN EL DIA DE LA SEMANA "TRANSFORMALO A STRING" Y comprueba si tienes que disparar esta alarma o si no.
			  int day = cal.get(Calendar.DAY_OF_WEEK);
			  if (!days.contains(getWeekDay(day))){
				  bClosePressed();
				  return;
			  }
			  prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		        //Set the pattern for vibration   
		        long pattern[]={1000,300};
		        
		        String ringTone = prefs.getString("alarm_clock_ringtone", "");
				vibrationActive = prefs.getBoolean("vibrationActive", true);
				int type = prefs.getInt("alarmType", Constants.CONNECTION_LOST);
				
				if (vibrationActive){
				
			        //Start the vibration
			        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
			        //start vibration with repeated count, use -1 if you don't want to repeat the vibration
			        vibrator.vibrate(pattern, 0);
				}
		        bMinutesMore.setVisibility(View.VISIBLE);
		        bCloseAlarm.setVisibility(View.VISIBLE);
				
		        /**
		         * Stop playing the sound.
		         */
				mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			    userVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			    mMediaPlayer = new MediaPlayer();
		        try {
			    	 Uri alert =  Uri.parse(ringTone);
			    	 mMediaPlayer = new MediaPlayer();
			    	 mMediaPlayer.setDataSource(baseActivity, alert);
			    	 mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			    	 mMediaPlayer.setLooping(true);
			    	 mMediaPlayer.prepare();
			    	 mMediaPlayer.start();
		        	 
		        } catch(Exception e) {
		        }   
			  
			  
			  mHandlerWatchAlarmClock.removeCallbacks(mCheckAlarmAction);
		}
		
	};
	
}
