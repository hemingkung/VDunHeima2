package com.heima.vdun;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.heima.vdun.util.Logger;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.heima.vdun.dao.TokenInfoDao;
import com.heima.vdun.entity.TokenInfo;
import com.heima.vdun.entity.VersionInfo;
import com.heima.vdun.global.App;
import com.heima.vdun.global.GlobalConstants;
import com.heima.vdun.service.NetService;
import com.heima.vdun.service.UpdateOTPService;
import com.heima.vdun.util.LocalSeedEncrypt;
import com.heima.vdun.util.PasscodeGenerator;
import com.heima.vdun.view.CountdownIndicator;
import com.umeng.analytics.MobclickAgent;

/**
 * OTP显示页面
 * 
 * @author Kevin
 * 
 */
public class PasscodePageActivity extends Activity {

	ImageView ivBit1;
	ImageView ivBit2;
	ImageView ivBit3;
	ImageView ivBit4;
	ImageView ivBit5;
	ImageView ivBit6;

	TextView tvPasscodeTime;
	TextView tvPasscodeDate;
	TextView tvCardNumber;
	CountdownIndicator pbProgress;
	TextView tvCountdown;

	Handler handler;
	private Timer timer;
	private ProgressDialog dialog;
	private SharedPreferences sp;
	private static final int REFRESH_INTERVAL_SEC = GlobalConstants.REFRESH_INTERVAL_SEC;// 更新OTP的时间间隔
	int countdown;
	VersionInfo versionInfo;

	private static final int MORE = 2;
	private static final int HELP = 3;
	private static final int ABOUT = 4;
	private static final int CANCEL = 5;
	private static final int ACCOUNT_LIST = 6;
	private static final int UPDATE_VERSION = 7;

	private static final int UPDATE = 8;
	private static final int COUNT_DOWN = 9;
	private static final int CHECK_TIME_ERROR = 10;
	private static final int CHECK_TIME_SUCCEED = 11;

	private static final int NET_ERROR = 12;
	private static final int GET_VERSION_ERROR = 13;
	private static final int SHOW_UPDATE_DIALOG = 14;
	private static final int UPDATE_DIALOG = 15;
	private static final int ADJUST_TIME = 16;
	private static final int UPDATE_PROGRESS = 17;

	private static final int TIMER_STEP = 100;
	private static float PHASE_STEP;

	private App app;
	// 第一次创建应用的时候要更新倒计时，如果为true就更新，以后都不用更新（通过发送广播来更新）
	private boolean isCreated;
	private Mac mac;
	private Calendar calendar;

	// 时间偏移量
	private long timeOffset;

	private float phase = 0;
	// OTP图片资源
	private int[] OTPImages = { R.drawable.num_0, R.drawable.num_1,
			R.drawable.num_2, R.drawable.num_3, R.drawable.num_4,
			R.drawable.num_5, R.drawable.num_6, R.drawable.num_7,
			R.drawable.num_8, R.drawable.num_9 };

	private TokenInfoDao dao;
	private PasscodeGenerator pcg;
	private TokenInfo tokenInfo;
	private String passCode;
	private Typeface tf;

	//如果系统时间发生变化，导致时间偏移量被修改，此时收到广播，会重新初始化widget，已经废弃不用
	private BroadcastReceiver updatePasscodeTime = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Logger.i("Test", "passcodePageActivity updatePasscodeTime onReceive");
			PasscodePageActivity.this.tokenInfo = App.dao.getTokenInfo();
			if (PasscodePageActivity.this.tokenInfo != null) {
				PasscodePageActivity.this.timeOffset = PasscodePageActivity.this.tokenInfo.tokenTime;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 去掉标题栏
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.passcode_page);
		
		sp = this.getSharedPreferences("vdun", MODE_PRIVATE);
		
		boolean fromWidget = getIntent().getBooleanExtra("fromWidget", false);
		Logger.i("Test", "fromewidget---->"+fromWidget);
		if(fromWidget) {
			this.startService(new Intent(this,UpdateOTPService.class));
		}
		
		IntentFilter filter = new IntentFilter(
				GlobalConstants.UPDATE_PASSCODE_TIME);
		this.registerReceiver(updatePasscodeTime, filter);
		
		if(App.dao.getTokenInfo()==null) {
			startActivity(new Intent(this,InitActivity.class));
			this.finish();
			return;
		}

		ivBit1 = (ImageView) findViewById(R.id.iv_bit_1);
		ivBit2 = (ImageView) findViewById(R.id.iv_bit_2);
		ivBit3 = (ImageView) findViewById(R.id.iv_bit_3);
		ivBit4 = (ImageView) findViewById(R.id.iv_bit_4);
		ivBit5 = (ImageView) findViewById(R.id.iv_bit_5);
		ivBit6 = (ImageView) findViewById(R.id.iv_bit_6);

		tvCardNumber = (TextView) findViewById(R.id.tv_card_number);
		tvCountdown = (TextView) findViewById(R.id.tv_countdown);
		tf = Typeface.createFromAsset(getAssets(), "digiface.ttf");
		tvCountdown.setTypeface(tf);
		tvCardNumber.setTypeface(tf);

		tvPasscodeTime = (TextView) findViewById(R.id.tv_passcode_time);
		tvPasscodeDate = (TextView) findViewById(R.id.tv_passcode_date);
		tvPasscodeTime.setTypeface(tf);
		tvPasscodeDate.setTypeface(tf);

		pbProgress = (CountdownIndicator) findViewById(R.id.pb_progress);
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		app = (App) this.getApplication();
		app.addActivity(this);
		
		app.finishInitActivity();//如果还有InitActivity页面，就销毁
		
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {

				switch (msg.what) {
				// 应用开启后会接收到此广播,用来更新倒计时
				case GlobalConstants.UPDATE_OTP:
					countdown = msg.arg1;
					phase = 1 - (float) countdown
							/ GlobalConstants.REFRESH_INTERVAL_SEC;
					//Logger.i("Test", "phase----->"+phase);
					if ((countdown + "").length() == 1) {
						tvCountdown.setText("0" + countdown);
					} else {
						tvCountdown.setText(countdown + "");
					}
					break;
				// 倒计时为0时收到此广播,更新OTP和令牌时间
				case UPDATE:
					setPasscode();
					setPasscodeTime();
					break;
				// 由定时器每隔1秒发出,用来更新倒计时
				case COUNT_DOWN:
					if ((countdown + "").length() == 1) {
						tvCountdown.setText("0" + countdown);
					} else {
						tvCountdown.setText(countdown + "");
					}
					break;
				case CHECK_TIME_ERROR:
					Toast.makeText(PasscodePageActivity.this,
							R.string.check_time_error, 1).show();
					break;
				// 校验时间成功后会重新更新倒计时
				case CHECK_TIME_SUCCEED:
					isCreated = true;
					setPasscode();
					setPasscodeTime();
					isCreated = false;
					Toast.makeText(PasscodePageActivity.this,
							R.string.check_time_ok, Toast.LENGTH_SHORT).show();
					break;
				case NET_ERROR:
					Toast.makeText(PasscodePageActivity.this,
							R.string.net_error, 1).show();
					break;
				case GET_VERSION_ERROR:
					Toast.makeText(PasscodePageActivity.this,
							R.string.get_version_error, 1).show();
					break;
				case SHOW_UPDATE_DIALOG:
					Logger.i("Test", "update dailog" + msg.arg1);
					showUpdateDialog(msg.arg1);
					break;
				case UPDATE_DIALOG:
					Logger.i("Test", "update dailog");
					createAlertDialog();
					break;
				case UPDATE_PROGRESS:
					setCountdownImage();
					break;
				}
			}
		};
		init();
		
		//开启子线程检查版本
		new Thread() {
			public void run() {
				Logger.i("Test", "mcheckversion start");
				mCheckVersion();
			};
		}.start();

		PHASE_STEP = (float) 1
				/ (float) (GlobalConstants.REFRESH_INTERVAL_SEC * 1000 / TIMER_STEP);
		//Logger.i("Test", "phase_step----->"+PHASE_STEP);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if(timer!=null) {
			timer.cancel();
		}
		this.unregisterReceiver(updatePasscodeTime);
	}

	// 初始化
	private void init() {
		try {
			// 从数据库中获取卡的密码和时间偏移量
			if (app == null) {
				app = (App) this.getApplication();
				Logger.i("Test", "app ==null");
			}

			dao = App.dao;// ***************dao是空的
			if (dao == null) {
				Logger.i("Test", "dao ==null");
				App.dao = new TokenInfoDao(this, app.IMEI);
				dao = App.dao;
			}
			tokenInfo = dao.getTokenInfo();
			if (tokenInfo != null) {
				String data = tokenInfo.data;
				timeOffset = tokenInfo.tokenTime;
				tvCardNumber.setText(" " + tokenInfo.SN);

				// 对数据进行解密
				LocalSeedEncrypt des = new LocalSeedEncrypt(app.IMEI);
				String desString = des.getDesString(data);

				mac = Mac.getInstance("HMACSHA1");
				mac.init(new SecretKeySpec(desString.getBytes(), ""));

				pcg = new PasscodeGenerator(mac, 6, REFRESH_INTERVAL_SEC,
						handler);

				calendar = Calendar.getInstance();
			} else {
				app.exit();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//更新当前密钥的日期
	private void setPasscodeTime() {

		calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis() - timeOffset);
		Date time = calendar.getTime();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String date = formatter.format(time); // 将日期时间格式化

		tvPasscodeTime.setText(date.substring(11));
		tvPasscodeDate.setText(date.substring(0, 10));
	}

	//更新倒计时动画
	private void setCountdownImage() {
		phase += PHASE_STEP;
		if (phase >= 1) {
			phase = 0;
		}
		//Logger.i("Test", "set countdown phase--->"+phase);
		pbProgress.setPhase(1 - phase);
	}

	//更新6位密码
	private void setOTPImage(String passCode) {
		
		startAnim(passCode);
	}

	//计算一次动态密码
	private void setPasscode() {

		try {
			passCode = pcg.generateTimeoutCode(isCreated, timeOffset);// pcg空的
			setOTPImage(passCode);
			app.otp = passCode;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//更新倒计时数字
	private void setCountdown() {
		countdown--;
		if (countdown <= 0) {
			countdown = REFRESH_INTERVAL_SEC;
			phase = 0;
			handler.sendEmptyMessage(UPDATE);
		}
		handler.sendEmptyMessage(COUNT_DOWN);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showCancelDialog();
			return true;
		}
		return false;
	}
	
	//程序退出的对话框
	private void showCancelDialog() {
		new AlertDialog.Builder(this).setTitle(R.string.info).setMessage(
				this.getResources().getString(R.string.exit_confirm)).setIcon(
				android.R.drawable.ic_dialog_alert).setPositiveButton(
				R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						app.exit();
					}
				}).setNegativeButton(R.string.cancel, null).show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// menu.add(Menu.NONE, CARD_INFO, Menu.NONE, "令牌信息");
		menu.add(Menu.NONE, ADJUST_TIME, Menu.NONE, R.string.check_time).setIcon(R.drawable.icon_time);
		menu.add(Menu.NONE, ACCOUNT_LIST, Menu.NONE, R.string.main_view_menu_account).setIcon(R.drawable.icon_user);
		SubMenu subMenu = menu.addSubMenu(Menu.NONE, MORE, Menu.NONE,
				R.string.more).setIcon(R.drawable.icon_more);
		subMenu.add(Menu.NONE, HELP, Menu.NONE, R.string.help);
		subMenu.add(Menu.NONE, ABOUT, Menu.NONE, R.string.about);
		subMenu.add(Menu.NONE, UPDATE_VERSION, Menu.NONE, R.string.update);
		subMenu.add(Menu.NONE, CANCEL, Menu.NONE, R.string.exit);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent intent = new Intent();
		switch (item.getItemId()) {
		case ADJUST_TIME:
			// 校准时间
			dialog = new ProgressDialog(PasscodePageActivity.this);
			dialog.setTitle(R.string.check_time);
			dialog.setMessage(PasscodePageActivity.this.getResources()
					.getString(R.string.checking_time_info));
			dialog.show();
			new Thread() {
				@Override
				public void run() {
					Long timeOffset = NetService.checkTime();
					if (timeOffset == null) {
						dialog.dismiss();
						handler.sendEmptyMessage(CHECK_TIME_ERROR);
					} else {
						dao.updateTime(timeOffset);
						PasscodePageActivity.this.timeOffset = timeOffset;
						dialog.dismiss();
						handler.sendEmptyMessage(CHECK_TIME_SUCCEED);
						
						if(timer!=null) {
							timer.cancel();
						}
						timer = new Timer(true);
						timer.schedule(new UpdateTask(), 0, TIMER_STEP);// 启动定时器
						isCreated = false;
					}
				}
			}.start();
			break;
		case HELP:
			// 帮助
			intent.setClass(this, HelpActivity.class);
			this.startActivity(intent);
			break;
		case ABOUT:
			// 关于
			intent.setClass(this, AboutActivity.class);
			this.startActivity(intent);
			break;
		case ACCOUNT_LIST:
			// 账户列表
			intent.setClass(PasscodePageActivity.this, AccountListActivity.class);
			intent.putExtra("hideSkip", true);
			this.startActivity(intent);
			break;
		case UPDATE_VERSION:
			// 升级
			dialog = new ProgressDialog(this);
			dialog.setTitle(R.string.update);
			dialog.setMessage(this.getResources().getString(
					R.string.checking_version));
			dialog.show();
			new Thread() {
				@Override
				public void run() {
					checkVersion();
					dialog.dismiss();
				}
			}.start();

			break;
		case CANCEL:
			// 退出
			showCancelDialog();
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private final class UpdateTask extends TimerTask {

		int i = 0;

		@Override
		public void run() {
			//Logger.i("Test", "passcodePage-->updateTask");
			i++;
			if (i >= 1000 / TIMER_STEP) {
				setCountdown();
				i = 0;
			}
			handler.sendEmptyMessage(UPDATE_PROGRESS);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		//Logger.i("Test", "onResume");

		isCreated = true;// 控制OTP生成器是否发送"更新倒计时的广播"的开关,true表示发送
		setPasscode();// 生成OTP,此时OTP生成器会发送"更新倒计时的消息"
		setPasscodeTime();

		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer(true);
		timer.schedule(new UpdateTask(), 0, TIMER_STEP);// 启动定时器
		isCreated = false;// 关闭开关,在程序运行期间不会再发送"更新倒计时的消息"

		MobclickAgent.onResume(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (timer != null) {
			timer.cancel();
		}
		MobclickAgent.onPause(this);
	}

	// 手动检查版本的方法代码
	public void checkVersion() {
		PackageManager pm = this.getPackageManager();
		PackageInfo packageInfo = null;
		int version = -1;
		try {
			packageInfo = pm.getPackageInfo(this.getPackageName(),
					PackageManager.GET_UNINSTALLED_PACKAGES);
			version = packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			handler.sendEmptyMessage(GET_VERSION_ERROR);
		}
		if (version != -1) {
			versionInfo = NetService.getVersion();
			if (versionInfo != null) {
				Message msg = new Message();
				msg.what = SHOW_UPDATE_DIALOG;
				if (version >= Integer.parseInt(versionInfo.versionCode)) {
					// 提示用户已经是最新版本
					msg.arg1 = 0;
					handler.sendMessage(msg);
				} else {
					// 如果不是最新版本,询问用户是否安装最新版本
					msg.arg1 = 1;
					handler.sendMessage(msg);
				}
			} else {
				// 网络连接异常
				handler.sendEmptyMessage(NET_ERROR);
			}
		}
	}

	//自动检查版本的代码
	public void mCheckVersion() {
		PackageManager pm = this.getPackageManager();
		PackageInfo packageInfo = null;
		int version = -1;
		try {
			packageInfo = pm.getPackageInfo(this.getPackageName(),
					PackageManager.GET_UNINSTALLED_PACKAGES);
			version = packageInfo.versionCode;
		} catch (Exception e) {
			handler.sendEmptyMessage(GET_VERSION_ERROR);
		}
		if (version != -1) {
			versionInfo = NetService.getVersion();
			if (versionInfo != null) {
				if (version < Integer.parseInt(versionInfo.versionCode)) {
					// 如果不是最新版本,询问用户是否安装最新版本
					Logger.i("Test", "not the latest version");
					if (!sp.getString("versionCode", "0").equals(
							versionInfo.versionCode)) {
						handler.sendEmptyMessage(UPDATE_DIALOG);
					}
				}
			}
		}
	}

	//用户手动检查版本时会弹出此对话框
	private void showUpdateDialog(int isLatestVersion) {
		if (isLatestVersion == 0) {
			//不需要更新版本
			new AlertDialog.Builder(this).setTitle(R.string.info).setMessage(
					this.getResources().getString(R.string.no_need_update))
					.setIcon(android.R.drawable.ic_dialog_info)
					.setPositiveButton(R.string.ok, null).show();
		} else {
			//需要更新版本
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(
					this.getResources().getString(R.string.find_new_version)
							+ versionInfo.versionName).setMessage(
					versionInfo.description).setIcon(
					android.R.drawable.ic_dialog_info);
			builder.setPositiveButton(R.string.download_now,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(Intent.ACTION_VIEW);
							intent.setData(Uri.parse(versionInfo.downloadUrl));
							startActivity(intent);
						}
					}).setNegativeButton(R.string.cancel, null).show();
		}
	}

	// 发现新版本后自动弹出对话框
	public void createAlertDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(this.getResources().getString(
				R.string.find_new_version)
				+ versionInfo.versionName);
		builder.setIcon(android.R.drawable.ic_dialog_info);
		builder.setMessage(versionInfo.description);

		builder.setPositiveButton(R.string.download_now,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse(versionInfo.downloadUrl));
						startActivity(intent);
					}
				});

		builder.setNegativeButton(R.string.update_later,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						Editor editor = sp.edit();
						editor
								.putString("versionCode",
										versionInfo.versionCode);
						editor.commit();
					}
				});

		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	
	//实现6位数字的动画效果
	 private void startAnim(String passcode) {
		 
		 	//Logger.i("Test", "passcode---->"+passcode);
		 	int frameNumber = 9;
		 	int frameTime = 30;
		 	
			final AnimationDrawable ad1 = new AnimationDrawable();
			final AnimationDrawable ad2 = new AnimationDrawable();
			final AnimationDrawable ad3 = new AnimationDrawable();
			final AnimationDrawable ad4 = new AnimationDrawable();
			final AnimationDrawable ad5 = new AnimationDrawable();
			final AnimationDrawable ad6 = new AnimationDrawable();
			
			ad1.setOneShot(true);
			ad2.setOneShot(true);
			ad3.setOneShot(true);
			ad4.setOneShot(true);
			ad5.setOneShot(true);
			ad6.setOneShot(true);
			
			for(int i=0;i<frameNumber;i++) {
				int random = (int) Math.round(Math.random() * 9);
				Drawable frame = getResources().getDrawable(OTPImages[random]);
				ad1.addFrame(frame, frameTime);
			}
			for(int i=0;i<frameNumber;i++) {
				int random = (int) Math.round(Math.random() * 9);
				Drawable frame = getResources().getDrawable(OTPImages[random]);
				ad2.addFrame(frame, frameTime);
			}
			for(int i=0;i<frameNumber;i++) {
				int random = (int) Math.round(Math.random() * 9);
				Drawable frame = getResources().getDrawable(OTPImages[random]);
				ad3.addFrame(frame, frameTime);
			}
			for(int i=0;i<frameNumber;i++) {
				int random = (int) Math.round(Math.random() * 9);
				Drawable frame = getResources().getDrawable(OTPImages[random]);
				ad4.addFrame(frame, frameTime);
			}
			for(int i=0;i<frameNumber;i++) {
				int random = (int) Math.round(Math.random() * 9);
				Drawable frame = getResources().getDrawable(OTPImages[random]);
				ad5.addFrame(frame, frameTime);
			}
			for(int i=0;i<frameNumber;i++) {
				int random = (int) Math.round(Math.random() * 9);
				Drawable frame = getResources().getDrawable(OTPImages[random]);
				ad6.addFrame(frame, frameTime);
			}
			
			Drawable frame1 = getResources().getDrawable(OTPImages[Integer.parseInt(passCode.charAt(0)
					+ "")]);
			Drawable frame2 = getResources().getDrawable(OTPImages[Integer.parseInt(passCode.charAt(1)
					+ "")]);
			Drawable frame3 = getResources().getDrawable(OTPImages[Integer.parseInt(passCode.charAt(2)
					+ "")]);
			Drawable frame4 = getResources().getDrawable(OTPImages[Integer.parseInt(passCode.charAt(3)
					+ "")]);
			Drawable frame5 = getResources().getDrawable(OTPImages[Integer.parseInt(passCode.charAt(4)
					+ "")]);
			Drawable frame6 = getResources().getDrawable(OTPImages[Integer.parseInt(passCode.charAt(5)
					+ "")]);
			
			ad1.addFrame(frame1, frameTime);
			ad2.addFrame(frame2, frameTime);
			ad3.addFrame(frame3, frameTime);
			ad4.addFrame(frame4, frameTime);
			ad5.addFrame(frame5, frameTime);
			ad6.addFrame(frame6, frameTime);
			
			ivBit1.setBackgroundDrawable(ad1);
			ivBit2.setBackgroundDrawable(ad2);
			ivBit3.setBackgroundDrawable(ad3);
			ivBit4.setBackgroundDrawable(ad4);
			ivBit5.setBackgroundDrawable(ad5);
			ivBit6.setBackgroundDrawable(ad6);
			
			if(ad1.isRunning()) {
				ad1.stop();
			}
			if(ad2.isRunning()) {
				ad2.stop();
			}
			if(ad3.isRunning()) {
				ad3.stop();
			}
			if(ad4.isRunning()) {
				ad4.stop();
			}
			if(ad5.isRunning()) {
				ad5.stop();
			}
			if(ad6.isRunning()) {
				ad6.stop();
			}
			
			ivBit1.post(new Runnable() {
				
				@Override
				public void run() {
					ad1.start();
					ad2.start();
					ad3.start();
					ad4.start();
					ad5.start();
					ad6.start();
				}
			});
		}

}
