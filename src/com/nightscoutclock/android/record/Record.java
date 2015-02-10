package com.nightscoutclock.android.record;

import java.io.Serializable;

import android.graphics.Color;


public class Record implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1857687174963206840L;
	public String insulinLeft = "---";
    public String sgv = "---";
    public String difference = "---";
    public String mbg = "---";
    public String mbgTime = "---";
    public String batteryVoltage = "---";
    public String remoteBattery = "---";
    public String sgvTime = "---";//Not used yet, I don not see the point of uploading this info.
    public int insulinIcon = -1;
    public int batteryIcon = -1;
    public int remoteBatteryIcon = -1;
    public int colorSGV = Color.WHITE;
    public int textSGVFlags = 0;
    public int timeColor = -1;
    public String arrow = null;
}
