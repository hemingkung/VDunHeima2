package com.heima.vdun.global;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.telephony.TelephonyManager;

import com.heima.vdun.dao.TokenInfoDao;
import com.heima.vdun.util.Logger;

public class App extends Application implements Thread.UncaughtExceptionHandler {

	public static TokenInfoDao dao;
	public String IMEI;
	public String otp;
	
	private TelephonyManager telephonyManager;
	private List<Activity> activityList = new ArrayList<Activity>();
	
	public void onCreate() {
		//Logger.i("Test", "App conCreate");
		telephonyManager=(TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		IMEI = telephonyManager.getDeviceId();
		dao = new TokenInfoDao(this, IMEI);
		
		Thread.setDefaultUncaughtExceptionHandler(this);
	}
	
	public void addActivity(Activity activity) {
		activityList.add(activity);
	}
	
	public void exit() {
		for(Activity activity : activityList) {
			activity.finish();
		}
	}
	
	public void finishInitActivity() {
		
		for(int i=0;i<activityList.size();i++) {
			if(activityList.get(i).getLocalClassName().equals("InitActivity")) {
				activityList.get(i).finish();
				activityList.remove(i);
			}
		}
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		Logger.i("Test", "system crush!!!");
		android.os.Process.killProcess(android.os.Process.myPid());
	}
}
