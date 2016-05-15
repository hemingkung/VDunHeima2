package com.heima.vdun;

import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.heima.vdun.entity.TokenInfo;
import com.heima.vdun.global.App;
import com.heima.vdun.global.GlobalConstants;
import com.heima.vdun.service.NetService;
import com.heima.vdun.service.UpdateOTPService;
import com.heima.vdun.util.Logger;
import com.heima.vdun.util.PasscodeGenerator;
import com.umeng.analytics.MobclickAgent;

/**
 * 绑定页面
 * 
 * @author Kevin
 * 
 */
public class InitActivity extends Activity implements OnClickListener {

	App app;
	Button btSms;
	Button btWeb;

	ProgressDialog dialog;

	Timer timer;
	Handler handler;
	TokenInfo info;

	private String sn;
	private String sms;
	private String SENT_SMS_ACTION = "SENT_SMS_ACTION";
	private String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";

	private static final int NET_ERROR = 2;
	private static final int DISSMISS_PROGRESS_DIALOG = 3;
	private static final int VERIFY_BIND_FAILED = 4;
	private static final int VERIFY_BIND_SUCCEED = 5;
	private static final int INIT_CANCELED = 6;

	private BroadcastReceiver sendMessage = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// 判断短信是否发送成功
			switch (getResultCode()) {
			case Activity.RESULT_OK:
				 Toast.makeText(context, R.string.send_sms_succeed,
				 Toast.LENGTH_SHORT).show();
				break;
			default:
				Toast.makeText(InitActivity.this, R.string.send_sms_failed,
						Toast.LENGTH_LONG).show();
				showFailDialog();
				
				if(timer!=null) {
					timer.cancel();
				}
				break;
			}
		}
	};

	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// 表示对方成功收到短信
			 Toast.makeText(context, "短信接收成功",
					 Toast.LENGTH_SHORT).show();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.init_page);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		app = (App) this.getApplication();
		app.addActivity(this);
		btSms = (Button) findViewById(R.id.bind_msm);
		btWeb = (Button) findViewById(R.id.bind_web);
		btSms.setOnClickListener(this);
		btWeb.setOnClickListener(this);

		//此句表示是否是从桌面widget过来的
		boolean fromWidget = getIntent().getBooleanExtra("fromWidget", false);
		Logger.i("Test", "fromewidget---->" + fromWidget);
		if (fromWidget) {
			this.startService(new Intent(this, UpdateOTPService.class));
		}

		// 注册广播 发送消息
		registerReceiver(sendMessage, new IntentFilter(SENT_SMS_ACTION));
		registerReceiver(receiver, new IntentFilter(DELIVERED_SMS_ACTION));

		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				Intent intent = new Intent();
				switch (msg.what) {
				case NET_ERROR:
					dialog.dismiss();
					Toast.makeText(InitActivity.this, R.string.net_error,
							Toast.LENGTH_SHORT).show();
					break;
				case VERIFY_BIND_FAILED:
					dialog.dismiss();
					/*Toast.makeText(InitActivity.this, R.string.verify_bind_failed_info,
							Toast.LENGTH_LONG).show();*/
					intent.setClass(InitActivity.this,
								AccountListActivity.class);
					intent.putExtra("hideSkip", false);
					InitActivity.this.startActivity(intent);
					InitActivity.this.sendBroadcast(new Intent(
								GlobalConstants.GET_OTP_SUCCEED));
					break;
				case DISSMISS_PROGRESS_DIALOG:
					dialog.dismiss();
					break;
				case INIT_CANCELED:
					Toast.makeText(InitActivity.this, R.string.init_canceled,
							Toast.LENGTH_SHORT).show();
					break;
				case VERIFY_BIND_SUCCEED:
					dialog.dismiss();
					intent.setClass(InitActivity.this,
							AccountListActivity.class);
					intent.putExtra("hideSkip", false);
					InitActivity.this.startActivity(intent);
					InitActivity.this.sendBroadcast(new Intent(
							GlobalConstants.GET_OTP_SUCCEED));
					break;
				}
			}
		};
	}

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showCancelDialog();
		}
		return false;
	}

	
	/**
	 * 退出对话框
	 */
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
	public void onClick(View v) {
		Intent intent = new Intent();
		switch (v.getId()) {
		case R.id.bind_msm:
			if (checkNet()) {
				showBindDialog();
			} else {
				showNetErrorDialog();
			}
			break;
		case R.id.bind_web:
			intent.setClass(this, WebBindActivity.class);
			this.startActivity(intent);
			break;
		}
	}

	/**
	 * 检测网络是否正常
	 * @return
	 */
	private boolean checkNet() {
		ConnectivityManager nw = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netinfo = nw.getActiveNetworkInfo();
		if (netinfo == null || !netinfo.isAvailable()) {
			return false;
		}
		return true;
	}

	/**
	 * 如果异常，弹出错误提示，可以进行重试
	 */
	private void showNetErrorDialog() {
		AlertDialog dialog = new AlertDialog.Builder(this).setTitle(
				R.string.info).setMessage(
				this.getResources().getString(R.string.net_set_confirm))
				.setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(
						R.string.retry, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								if (checkNet()) {
									showBindDialog();
								} else {
									showNetErrorDialog();
								}
							}
						}).setNegativeButton(R.string.cancel, null).create();
		dialog.show();
	}

	/**
	 * 发送短信失败，提示使用其他手机发送短信
	 */
	private void showFailDialog() {
		new AlertDialog.Builder(this).setTitle(R.string.info).setMessage(
				getResources().getString(R.string.send_sms_failed_info1)
						+ sms
						+ getResources().getString(
								R.string.send_sms_failed_info2)
						+ GlobalConstants.SMS_NUMBER
						+ getResources().getString(
								R.string.send_sms_failed_info3)).setIcon(
				android.R.drawable.ic_dialog_alert).setPositiveButton(
				R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						timer = new Timer();
						timer.schedule(new CheckResponseTask(), 0, 1 * 1000);
					}
				}).setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						Logger.i("Test", "dialog quxiao");
						handler.sendEmptyMessage(DISSMISS_PROGRESS_DIALOG);
					}
				}).setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				Logger.i("Test", "dialog cancel");
				handler.sendEmptyMessage(DISSMISS_PROGRESS_DIALOG);
			}
		}).show();
	}

	private boolean tag;
	private final class CheckResponseTask extends TimerTask {
		int i = 0;
		@Override
		public void run() {

			if(tag) {
				Logger.i("Test", "verify Bind " + i);
				if (i < GlobalConstants.VERIFY_BIND_TIMES) {
					
					try {
						if(app.otp==null) {
							
							if(info!=null) {
								String data = info.data;

								Mac mac = Mac.getInstance("HMACSHA1");
								mac.init(new SecretKeySpec(data.getBytes(), ""));

								PasscodeGenerator pcg = new PasscodeGenerator(mac, 6, GlobalConstants.REFRESH_INTERVAL_SEC,
										handler);
								String passCode = pcg.generateTimeoutCode(false, info.tokenTime);
								app.otp = passCode;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					if (NetService.verifyBind(sn, app.IMEI,app.otp, InitActivity.this) == 1) {
						Logger.i("Test", "verify Bind ok");
						if (info != null) {
							App.dao.add(info);
							handler.sendEmptyMessage(VERIFY_BIND_SUCCEED);
						}else {
							handler.sendEmptyMessage(NET_ERROR);
						}
						this.cancel();
					}
					i++;
				} else {
					Logger.i("Test", "verify Bind failed");
					if (info != null) {
						App.dao.add(info);
						handler.sendEmptyMessage(VERIFY_BIND_FAILED);
					}else {
						handler.sendEmptyMessage(NET_ERROR);
					}
					this.cancel();
				}
			}else {
				this.cancel();
			}
		}
	}

	/**
	 * 开始绑定
	 */
	private void startBind() {
		
		tag = true;
		
		dialog = new ProgressDialog(this);
		dialog.setTitle(R.string.info);
		dialog.setMessage(this.getResources().getString(R.string.init_info));
		dialog.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				tag = false;
				handler.sendEmptyMessage(INIT_CANCELED);
			}
		});
		dialog.show();

		new Thread() {
			public void run() {
				try {
					SmsManager smsManager = SmsManager.getDefault();
					// create the sentIntent parameter
					Intent sentIntent = new Intent(SENT_SMS_ACTION);
					PendingIntent sentPI = PendingIntent.getBroadcast(
							InitActivity.this, 0, sentIntent, 0);

					// create the deilverIntent parameter
					Intent deliverIntent = new Intent(DELIVERED_SMS_ACTION);
					PendingIntent deliverPI = PendingIntent.getBroadcast(
							InitActivity.this, 0, deliverIntent, 0);

					//获取令牌信息
					info = NetService.getTokenInfo(app.IMEI, InitActivity.this);
					
					if(tag) {
						if (info != null) {
							sn = info.SN;
							sms = "hqwd" + " " + sn;
							String targetNumber = GlobalConstants.SMS_NUMBER;
							//发送短信
							smsManager.sendTextMessage(targetNumber, null, sms,
									sentPI, deliverPI);
							Logger.i("Test", "message sent");
							
							if (app.otp == null) {

								String data = info.data;
								Mac mac = Mac.getInstance("HMACSHA1");
								mac.init(new SecretKeySpec(data.getBytes(), ""));

								PasscodeGenerator pcg = new PasscodeGenerator(
										mac, 6,
										GlobalConstants.REFRESH_INTERVAL_SEC,
										handler);
								String passCode = pcg.generateTimeoutCode(
										false, info.tokenTime);
								app.otp = passCode;
							}
						
							//开启定时器，检测是否初始化成功
							timer = new Timer();
							timer.schedule(new CheckResponseTask(), 0, 1 * 1000);

						} else {
							Logger.i("Test", "info == null, message sent failed");
							handler.sendEmptyMessage(NET_ERROR);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					handler.sendEmptyMessage(NET_ERROR);
				}
			};
		}.start();
	}

	/**
	 * 开始绑定的对话框
	 */
	private void showBindDialog() {
		new AlertDialog.Builder(this).setTitle(R.string.info).setMessage(
				getResources().getString(R.string.bind_send_sms_ensure))
				.setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(
						R.string.continues,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								startBind();
							}
						}).setNegativeButton(R.string.cancel, null).show();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(sendMessage);
		this.unregisterReceiver(receiver);
	}
}
