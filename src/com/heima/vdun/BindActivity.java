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
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsManager;
import com.heima.vdun.util.Logger;

import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.heima.vdun.entity.BindResult;
import com.heima.vdun.entity.TokenInfo;
import com.heima.vdun.global.App;
import com.heima.vdun.global.GlobalConstants;
import com.heima.vdun.service.NetService;
import com.heima.vdun.util.LocalSeedEncrypt;
import com.heima.vdun.util.PasscodeGenerator;
import com.umeng.analytics.MobclickAgent;

/**
 * 短信绑定页面
 * 
 * @author Kevin
 * 
 */
public class BindActivity extends Activity implements OnClickListener {

	EditText etAccount;
	Button btBind;
	ProgressDialog dialog;
	TextView tvAccountInfo;

	Timer timer;
	Handler handler;
	TokenInfo info;
	App app;
	private BindResult response;
	
	private boolean fromInitPage = false;

	private String account;
	private String accountID;
	private String sn;
	private String sms;
	private String SENT_SMS_ACTION = "SENT_SMS_ACTION";
	private String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";

	private static final int NET_ERROR = 2;
	private static final int DISSMISS_PROGRESS_DIALOG = 3;
	private static final int BIND_FAILED = 4;
	private static final int BIND_SUCCEED = 5;
	private static final int CARD_NOT_BIND_PHONENUMBER = 6;
	private static final int ACCOUNT_NOT_BIND_PHONENUMBER = 7;
	private static final int ACCOUNT_NOT_MATCH_PHONENUMBER = 8;
	private static final int VERIFY_FAILED = 9;
	private static final int VERIFY_OK = 10;

	private BroadcastReceiver sendMessage = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// 判断短信是否发送成功
			switch (getResultCode()) {
			case Activity.RESULT_OK:
				/*Toast.makeText(context, R.string.send_sms_succeed,
				 Toast.LENGTH_SHORT).show();*/
				break;
			default:
				/*Toast.makeText(BindActivity.this, R.string.send_sms_failed,
						Toast.LENGTH_LONG).show();*/
				showFailDialog();

				if (timer != null) {
					timer.cancel();
				}
				break;
			}
		}
	};

	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			/*Toast.makeText(context, "短信接收成功",
					 Toast.LENGTH_SHORT).show();*/
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bind);
		etAccount = (EditText) findViewById(R.id.et_sms_bind);
		tvAccountInfo = (TextView) findViewById(R.id.tv_bind_account_info);
		btBind = (Button) findViewById(R.id.bt_sms_bind);
		btBind.setOnClickListener(this);
		app = (App) this.getApplication();
		app.addActivity(this);

		getWindow().setFormat(PixelFormat.RGBA_8888);

		Intent intent = getIntent();
		String tag = intent.getStringExtra("bindTag");
		accountID = intent.getStringExtra("entry");
		fromInitPage = intent.getBooleanExtra("fromInitPage",false);
		String accountInfo = intent.getStringExtra("accountDesc");
		tvAccountInfo.setText(accountInfo);
		String hint = getResources().getString(R.string.bind_page_text1) + tag
				+ getResources().getString(R.string.bind_page_text2);
		etAccount.setHint(hint);
		
		info = App.dao.getTokenInfo();
		sn = info.SN;

		// 注册广播 发送消息
		registerReceiver(sendMessage, new IntentFilter(SENT_SMS_ACTION));
		registerReceiver(receiver, new IntentFilter(DELIVERED_SMS_ACTION));

		handler = new Handler() {
			Intent i = null;
			@Override
			public void handleMessage(Message msg) {

				switch (msg.what) {
				case NET_ERROR:
					dialog.dismiss();
					Toast.makeText(BindActivity.this, R.string.net_error,
							Toast.LENGTH_SHORT).show();
					break;
				case BIND_FAILED:
					dialog.dismiss();
					if (response != null) {
						showErrorDialog(response);
					}
					break;
				case DISSMISS_PROGRESS_DIALOG:
					dialog.dismiss();
					break;
				case CARD_NOT_BIND_PHONENUMBER:
					dialog.dismiss();
					if (response != null) {
						showSmsDialog(response);
					}
					break;
				case ACCOUNT_NOT_BIND_PHONENUMBER:
					dialog.dismiss();
					if (response != null) {
						showErrorDialog(response);
					}
					break;
				case ACCOUNT_NOT_MATCH_PHONENUMBER:
					dialog.dismiss();
					if (response != null) {
						showErrorDialog(response);
					}
					break;
				case BIND_SUCCEED:
					dialog.dismiss();
					//sp.edit().putBoolean(accountID, true).commit();
					i = new Intent(BindActivity.this, BindSucceedActivity.class);
					startActivity(i);
					BindActivity.this.finish();
					break;
				case VERIFY_FAILED:
					dialog.dismiss();
					Toast.makeText(BindActivity.this,
							R.string.bind_error_later_try, Toast.LENGTH_SHORT)
							.show();
					i = new Intent(BindActivity.this, AddAccountActivity.class);
					startActivity(i);
					BindActivity.this.finish();
					break;
				case VERIFY_OK:
					dialog.dismiss();
					startBind();
					break;
				}
			}
		};
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.bt_sms_bind:
			account = etAccount.getText().toString().trim();
			Logger.i("Test", "bind account==" + account);
			if (account != null && !account.trim().equals("")) {
				if (checkNet()) {
					startBind();
				} else {
					showNetErrorDialog();
				}
			} else {
				Toast.makeText(this, R.string.account_empty_info, 1).show();
			}
			break;
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK&&fromInitPage) {
			
			Intent intent = new Intent();
			intent.setClass(this, PasscodePageActivity.class);
			startActivity(intent);
			finish();
			return true;
			
		}
		return super.onKeyDown(keyCode, event);
	}

	private void showErrorDialog(BindResult result) {
		new AlertDialog.Builder(this).setTitle(R.string.info).setMessage(
				result.message).setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(R.string.ok, null).show();
	}

	private void showSmsDialog(BindResult result) {
		new AlertDialog.Builder(this).setTitle(R.string.info).setMessage(
				result.message).setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								startSmsBind();
							}
						}).setNegativeButton(R.string.cancel, null).show();
	}

	private void startSmsBind() {
		dialog = new ProgressDialog(this);
		dialog.setTitle(R.string.info);
		dialog.setMessage(this.getResources().getString(R.string.binding_info));
		dialog.show();

		new Thread() {
			public void run() {
				try {
					SmsManager smsManager = SmsManager.getDefault();
					// create the sentIntent parameter
					Intent sentIntent = new Intent(SENT_SMS_ACTION);
					PendingIntent sentPI = PendingIntent.getBroadcast(
							BindActivity.this, 0, sentIntent, 0);

					// create the deilverIntent parameter
					Intent deliverIntent = new Intent(DELIVERED_SMS_ACTION);
					PendingIntent deliverPI = PendingIntent.getBroadcast(
							BindActivity.this, 0, deliverIntent, 0);

					info = App.dao.getTokenInfo();
					if (info != null) {
						sn = info.SN;
						sms = "hqwd" + " " + sn;
						String targetNumber = GlobalConstants.SMS_NUMBER;
						smsManager.sendTextMessage(targetNumber, null, sms,
								sentPI, deliverPI);
						Logger.i("Test", "message sent");
						timer = new Timer();
						timer.schedule(new CheckResponseTask(), 0, 1 * 1000);

					} else {
						Logger.i("Test", "info == null, message sent failed");
						handler.sendEmptyMessage(BIND_FAILED);
					}

				} catch (Exception e) {
					e.printStackTrace();
					handler.sendEmptyMessage(BIND_FAILED);
				}
			};
		}.start();
	}

	private void startBind() {
		dialog = new ProgressDialog(this);
		dialog.setTitle(R.string.info);
		dialog.setMessage(this.getResources().getString(R.string.binding_info));
		dialog.show();

		new Thread() {

			public void run() {
				try {
					Logger
							.i("Test", "BindActivity---->" + "account=="
									+ account + " sn==" + sn + " OTP=="
									+ app.otp + " accountID==" + accountID
									+ " IMEI==" + app.IMEI);
					
					if(app.otp==null) {
						TokenInfo tokenInfo = App.dao.getTokenInfo();
						if(tokenInfo!=null) {
							String data = App.dao.getTokenInfo().data;
							// 对数据进行解密
							LocalSeedEncrypt des = new LocalSeedEncrypt(app.IMEI);
							String desString = des.getDesString(data);

							Mac mac = Mac.getInstance("HMACSHA1");
							mac.init(new SecretKeySpec(desString.getBytes(), ""));

							PasscodeGenerator pcg = new PasscodeGenerator(mac, 6, GlobalConstants.REFRESH_INTERVAL_SEC,
									handler);
							String passCode = pcg.generateTimeoutCode(false, tokenInfo.tokenTime);
							app.otp = passCode;
						}
					}
					
					response = NetService.bind(account, sn, app.otp,
							accountID, app.IMEI, BindActivity.this);

					if (response == null) {
						handler.sendEmptyMessage(NET_ERROR);
						return;
					}

					Logger.i("Test", "BindActivity---->response int-->"
							+ response.result);
					switch (response.result) {
					// 绑定成功
					case 1:
						handler.sendEmptyMessage(BIND_SUCCEED);
						break;
					// 绑定失败
					case 0:
						Logger.i("Test", "------------------------");
						handler.sendEmptyMessage(BIND_FAILED);
						break;
					// 卡号未绑定手机
					case 3:
						handler.sendEmptyMessage(CARD_NOT_BIND_PHONENUMBER);
						break;
					// 帐号未绑定手机
					case 4:
						handler.sendEmptyMessage(ACCOUNT_NOT_BIND_PHONENUMBER);
						break;
					// 帐号与手机号不匹配
					case 5:
						handler.sendEmptyMessage(ACCOUNT_NOT_MATCH_PHONENUMBER);
						break;
					// 绑定失败
					default:
						handler.sendEmptyMessage(BIND_FAILED);
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
					handler.sendEmptyMessage(BIND_FAILED);
				}
			};
		}.start();
	}

	private final class CheckResponseTask extends TimerTask {
		int i = 0;

		@Override
		public void run() {

			Logger.i("Test", "verify Bind " + i);
			if (i < GlobalConstants.VERIFY_BIND_TIMES) {

				if (NetService.verifyBind(sn, app.IMEI,app.otp,
						BindActivity.this) == 1) {
					Logger.i("Test", "verify Bind ok");
					handler.sendEmptyMessage(VERIFY_OK);
					//startBind();
					this.cancel();
				}
				i++;
			} else {
				Logger.i("Test", "verify Bind failed");
				handler.sendEmptyMessage(VERIFY_FAILED);
				this.cancel();
			}
		}
	}

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

	private boolean checkNet() {
		ConnectivityManager nw = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netinfo = nw.getActiveNetworkInfo();
		if (netinfo == null || !netinfo.isAvailable()) {
			return false;
		}
		return true;
	}

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
									startBind();
								} else {
									showNetErrorDialog();
								}
							}
						}).setNegativeButton(R.string.cancel, null).create();
		dialog.show();
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
	protected void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(sendMessage);
		this.unregisterReceiver(receiver);
	}

}
