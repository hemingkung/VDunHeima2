package com.heima.vdun;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.heima.vdun.util.Logger;

import com.heima.vdun.global.App;
import com.heima.vdun.global.GlobalConstants;
import com.heima.vdun.service.NetService;
/**
 * 系统时间被更改后的广播接收者
 * 
 * 废弃不用
 * 
 * @author Kevin
 * 
 */
public class TimeChangedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Logger.i("Test", "Time changed!");
		final Context ctx = context;
		new Thread() {
			@Override
			public void run() {
				Long timeOffset = NetService.checkTime();
				if (timeOffset != null) {
					Logger.i("Test", timeOffset+"");
					App.dao.updateTime(timeOffset);
				}
				Intent intent = new Intent();
				intent.setAction(GlobalConstants.UPDATE_PASSCODE_TIME);
				ctx.sendBroadcast(intent);
			}
		}.start();
	}
}
