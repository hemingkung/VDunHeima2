package com.heima.vdun;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.heima.vdun.util.Logger;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.heima.vdun.entity.TokenInfo;
import com.heima.vdun.global.App;
import com.heima.vdun.global.GlobalConstants;
import com.heima.vdun.service.NetService;
import com.heima.vdun.util.LocalSeedEncrypt;
import com.heima.vdun.util.PasscodeGenerator;
import com.umeng.analytics.MobclickAgent;
/**
 * 解绑页面
 * 
 * @author Kevin
 * 
 */
public class UnBindActivity extends Activity implements OnClickListener {

	EditText etAccount;
	Button btUnBind;
	ProgressDialog dialog;
	
	Handler handler;
	TokenInfo info;
	App app;
	
	private String account;
	private String sn;
	private String accountID;
	
	private static final int NET_ERROR = 2;
	private static final int UNBIND_FAILED = 4;
	private static final int UNBIND_SUCCEED = 5;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.unbind);
		etAccount = (EditText) findViewById(R.id.et_unbind_page_unbind);
		btUnBind = (Button) findViewById(R.id.bt_unbind_page_unbind);
		btUnBind.setOnClickListener(this);
		app = (App) this.getApplication();
		app.addActivity(this);
		
		getWindow().setFormat(PixelFormat.RGBA_8888);
		
		Intent intent = getIntent();
		String tag = intent.getStringExtra("unBindTag");
		accountID = intent.getStringExtra("entry");
		
		String hint = getResources().getString(R.string.bind_page_text1)+tag+getResources().getString(R.string.bind_page_text2);
		etAccount.setHint(hint);
		
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case NET_ERROR:
					dialog.dismiss();
					Toast.makeText(UnBindActivity.this, R.string.net_error,
							Toast.LENGTH_SHORT).show();
					break;
				case UNBIND_FAILED:
					dialog.dismiss();
					Toast.makeText(UnBindActivity.this, R.string.unbind_error,
							Toast.LENGTH_SHORT).show();
					break;
				case UNBIND_SUCCEED:
					dialog.dismiss();
					//sp.edit().putBoolean(accountID, false).commit();
					Toast.makeText(UnBindActivity.this, R.string.unbind_succeed,
							Toast.LENGTH_LONG).show();
					startActivity(new Intent(UnBindActivity.this, UnBindSucceedActivity.class));
					UnBindActivity.this.finish();
					break;
				}
			}
		};
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.bt_unbind_page_unbind:
			account = etAccount.getText().toString().trim();
			Logger.i("Test", "unbind account==" + account);
			if (account != null && !account.trim().equals("")) {
				if (checkNet()) {
					startUnBind();
				} else {
					showNetErrorDialog();
				}
			} else {
				Toast.makeText(this, R.string.account_empty_info, 1).show();
			}
			break;
		}
	}

	private void startUnBind() {
		dialog = new ProgressDialog(this);
		dialog.setTitle(R.string.info);
		dialog.setMessage(this.getResources().getString(R.string.unbinding_info));
		dialog.show();
		sn = App.dao.getTokenInfo().SN;
		
		new Thread() {
			public void run() {
				try {
					
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
					
					int response = NetService.unBind(account, sn, app.otp, accountID, app.IMEI, UnBindActivity.this);
					Logger.i("Test", "UnBindActivity---->response int-->"+response);
					
					switch (response) {
					//解绑成功
					case 1:
						handler.sendEmptyMessage(UNBIND_SUCCEED);
						break;
					//网络异常
					case -1:
						handler.sendEmptyMessage(NET_ERROR);
						break;
					//绑定失败
					default:
						handler.sendEmptyMessage(UNBIND_FAILED);
						break;
					}
					
				} catch (Exception e) {
					e.printStackTrace();
					handler.sendEmptyMessage(UNBIND_FAILED);
				}
			};
		}.start();
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
									startUnBind();
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

}
