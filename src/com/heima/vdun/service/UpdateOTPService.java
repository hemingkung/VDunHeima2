package com.heima.vdun.service;

import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import com.heima.vdun.util.Logger;
import android.widget.RemoteViews;

import com.heima.vdun.InitActivity;
import com.heima.vdun.PasscodePageActivity;
import com.heima.vdun.R;
import com.heima.vdun.entity.TokenInfo;
import com.heima.vdun.global.App;
import com.heima.vdun.global.GlobalConstants;
import com.heima.vdun.util.LocalSeedEncrypt;
import com.heima.vdun.util.PasscodeGenerator;
import com.heima.vdun.widget.PasscodeWidget;

public class UpdateOTPService extends Service {

	private Handler handler;
	private static final int REFRESH_INTERVAL_SEC = GlobalConstants.REFRESH_INTERVAL_SEC;// 更新OTP的时间间隔
	private static final int RESTART_SERVICE = 0;
	private Timer timer;
	private App app;
	private Mac mac;
	private RemoteViews remoteViews;
	private AppWidgetManager appWidgetManager;
	boolean stopService;
	private PasscodeGenerator pcg;
	private ComponentName componentName;
	//private TextView tvWidget;
	
	// 时间偏移量
	//private long timeOffset;
	private TokenInfo tokenInfo;

	private PendingIntent passcodePagePendingIntent;
	private PendingIntent bindPendingIntent;

	//如果系统时间发生变化，导致时间偏移量被修改，此时收到广播，会重新初始化widget，已经废弃不用
	private BroadcastReceiver updatePasscodeTime = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Logger.i("Test", "UpdateOTPService updatePasscodeTime onReceive");
			UpdateOTPService.this.tokenInfo = App.dao.getTokenInfo();
			
			setPasscode();
			if (timer != null) {
				timer.cancel();
			}
			timer = new Timer(true);
			timer.schedule(new UpdateTask(), 0, 2000);
		}
	};
	
	//如果用户绑定成功后，会收到广播，于是重新初始化widget
	private BroadcastReceiver bindSucceed = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Logger.i("Test", "UpdateOTPService bindSucceed onReceive");
			
			try {
				tokenInfo = App.dao.getTokenInfo();
				String data = tokenInfo.data;
				// 对数据进行解密
				LocalSeedEncrypt des = new LocalSeedEncrypt(app.IMEI);
				String desString = des.getDesString(data);

				mac = Mac.getInstance("HMACSHA1");
				mac.init(new SecretKeySpec(desString.getBytes(), ""));

				pcg = new PasscodeGenerator(mac, 6, REFRESH_INTERVAL_SEC,
						handler);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	};

	private static KeyguardManager mKeyguardManager;

	@Override
	public void onCreate() {
		super.onCreate();
		app = (App) this.getApplication();
		tokenInfo = App.dao.getTokenInfo();
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case RESTART_SERVICE:
					UpdateOTPService.this.startService(new Intent(
							UpdateOTPService.this, UpdateOTPService.class));
					break;
				}
			}
		};
		
		init();
		timer = new Timer(true);
		timer.schedule(new UpdateTask(), 0, 2000);

		IntentFilter filter = new IntentFilter(
				GlobalConstants.UPDATE_PASSCODE_TIME);
		this.registerReceiver(updatePasscodeTime, filter);
		
		IntentFilter filter2 = new IntentFilter(
				GlobalConstants.GET_OTP_SUCCEED);
		this.registerReceiver(bindSucceed, filter2);

		mKeyguardManager = (KeyguardManager) this
				.getSystemService(Context.KEYGUARD_SERVICE);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		if (intent.getBooleanExtra("stop", false)) {
			stopService = true;
			stopSelf();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Logger.i("Test", "service destroy");
		if (timer != null) {
			if (!stopService) {
				//如果发现用户并没有手动删除widget，而是其他外部因素导致的，就会在10秒钟之后重启service
				timer.cancel();
				handler.sendEmptyMessageDelayed(RESTART_SERVICE, 10000);
			} else {
				timer.cancel();
			}
		}
		this.unregisterReceiver(updatePasscodeTime);
		this.unregisterReceiver(bindSucceed);
	}

	//初始化
	private void init() {
		try {
			if (tokenInfo != null) {
				String data = tokenInfo.data;
				// 对数据进行解密
				LocalSeedEncrypt des = new LocalSeedEncrypt(app.IMEI);
				String desString = des.getDesString(data);

				mac = Mac.getInstance("HMACSHA1");
				mac.init(new SecretKeySpec(desString.getBytes(), ""));

				pcg = new PasscodeGenerator(mac, 6, REFRESH_INTERVAL_SEC,
						handler);
			}
			componentName = new ComponentName(getApplicationContext(),
					PasscodeWidget.class); 
			
			Intent pIntent = new Intent(this, PasscodePageActivity.class);
			pIntent.putExtra("fromWidget", true);
			passcodePagePendingIntent = PendingIntent.getActivity(
					getApplicationContext(), 100, pIntent, 0);
			
			Intent bIntent = new Intent(this, InitActivity.class);
			bIntent.putExtra("fromWidget", true);
			bindPendingIntent = PendingIntent.getActivity(
					getApplicationContext(), 100, bIntent, 0);

			remoteViews = new RemoteViews(getPackageName(),
					R.layout.passcode_widget);
			appWidgetManager = AppWidgetManager
					.getInstance(getApplicationContext());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setPasscode() {

		try {
			tokenInfo = App.dao.getTokenInfo();
			
			//如果tokenInfo为空，说明目前手机令牌还没有初始化，点击后应该跳到初始化页面
			if (tokenInfo == null) {
				setOTPImage(null);
				remoteViews.setOnClickPendingIntent(R.id.layout_widget,
						bindPendingIntent);
			} else {
				
				//令牌已经初始化，点击后进入令牌主页面
				if (pcg == null) {
					tokenInfo = App.dao.getTokenInfo();
					String data = tokenInfo.data;
					// 对数据进行解密
					LocalSeedEncrypt des = new LocalSeedEncrypt(app.IMEI);
					String desString = des.getDesString(data);

					mac = Mac.getInstance("HMACSHA1");
					mac.init(new SecretKeySpec(desString.getBytes(), ""));

					pcg = new PasscodeGenerator(mac, 6, REFRESH_INTERVAL_SEC,
							handler);
				}

				remoteViews.setOnClickPendingIntent(R.id.layout_widget,
						passcodePagePendingIntent);
				String passCode = pcg.generateTimeoutCode(false, tokenInfo.tokenTime);
				//Logger.i("Test", "widget service passcode--->"+passCode);
				setOTPImage(passCode);
			}
			
			//更新widget
			appWidgetManager
					.updateAppWidget(componentName, remoteViews);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	
	//定时器，每两秒计算一次动态密码
	private final class UpdateTask extends TimerTask {
		public void run() {
			//如果锁屏的话就不进行更新（省电）
			if (!isScreenLocked()) {
				setPasscode();
				//Logger.i("Test", "UpdateOTPService-->setPasscode");
			}
		}
	}

	
	//判断是否锁屏
	public final boolean isScreenLocked() {
		
		if(mKeyguardManager==null) {
			mKeyguardManager = (KeyguardManager) this
			.getSystemService(Context.KEYGUARD_SERVICE);
		}
		
		return mKeyguardManager.inKeyguardRestrictedInputMode();
	}

	//更新widget显示内容
	private void setOTPImage(String passCode) {
		
		if(passCode!=null) {
			remoteViews.setTextViewText(R.id.tv_widget, passCode);
		}else {
			//如果发现此时令牌还没有初始化，就用“------”代替6位动态数字
			for(int i=0;i<6;i++) {
				remoteViews.setTextViewText(R.id.tv_widget, "- - - - - -");
			}
		}
	}
}
