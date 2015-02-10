package com.nightscoutclock.android.clock;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import ch.qos.logback.classic.Logger;

import com.nightscoutclock.android.download.DownloadHelper;
import com.nightscoutclock.android.medtronic.Constants;
/**
 * 
 * @author lmmarguenda
 *
 */
public class CGMClockUpdater extends Service {

	public static int UPDATE_FREQUENCY_SEC = 10;
	private Logger log = (Logger)LoggerFactory.getLogger(CGMClock.class.getName());
	
	SharedPreferences prefs;
	DownloadHelper dwHelper = null;
	Handler mHandler = new Handler();
	@Override
	public void onCreate() {
		super.onCreate();
		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		log.info("CREATEEEEEEE");
		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log.info("onStartCommand");
		buildUpdate();
		
		return super.onStartCommand(intent, flags, startId);
	}
	private Runnable mUpdateData = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			buildUpdate();
		}
		
	};
	private void buildUpdate() {
		log.info("buildUpdate");
		dwHelper = new DownloadHelper(getBaseContext(), prefs);
		dwHelper.setPrefs(prefs);
		Object list[] = { null, null};
		dwHelper.execute(list);
		String type = prefs.getString("refreshPeriod", "2");
    	long time = Constants.TIME_2_MIN_IN_MS;
    	if (type.equalsIgnoreCase("1"))
    		time = Constants.TIME_1_MIN_IN_MS;
    	else if (type.equalsIgnoreCase("3"))
    		time = Constants.TIME_3_MIN_IN_MS;
    	else if (type.equalsIgnoreCase("4"))
    		time = Constants.TIME_4_MIN_IN_MS;
    	else if (type.equalsIgnoreCase("5"))
    		time = Constants.TIME_5_MIN_IN_MS;
    	else if (type.equalsIgnoreCase("6"))
    		time = Constants.TIME_10_MIN_IN_MS;
    	else if (type.equalsIgnoreCase("7"))
    		time = Constants.TIME_20_MIN_IN_MS;
    	else if (type.equalsIgnoreCase("8"))
    		time = Constants.TIME_25_MIN_IN_MS;
    	else if (type.equalsIgnoreCase("9"))
    		time = Constants.TIME_30_MIN_IN_MS;
    	else if (type.equalsIgnoreCase("10"))
    		time = Constants.TIME_60_MIN_IN_MS;
    	else
    		time = Constants.TIME_2_MIN_IN_MS;
		mHandler.postDelayed(mUpdateData, time);
		//mHandlerToggleInfo.removeCallbacks(aux);
		// Push update for this widget to the home screen

	}
	@Override
	public void onDestroy(){
		log.info("ON DESTROY UPDATER");
		mHandler.removeCallbacks(mUpdateData);
	}
	
	public JSONObject loadClassFile(File f) {

		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new FileInputStream(f));
			Object o = ois.readObject();
			ois.close();
			return (JSONObject) o;
		} catch (Exception ex) {
			try {
				if (ois != null)
					ois.close();
			} catch (Exception e) {
				log.error("Error", e);
			}
		}
		return new JSONObject();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}