package com.heima.vdun;

import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.AdapterView.OnItemClickListener;

import com.heima.vdun.adapter.AddAccountAdapter;
import com.heima.vdun.entity.AccountInfo;
import com.heima.vdun.entity.TokenInfo;
import com.heima.vdun.global.App;
import com.heima.vdun.global.GlobalConstants;
import com.heima.vdun.service.NetService;
import com.heima.vdun.util.LocalSeedEncrypt;
import com.heima.vdun.util.PasscodeGenerator;
import com.umeng.analytics.MobclickAgent;

public class AddAccountActivity extends Activity {

	private App app;
	private ListView lvList;
	private ProgressBar pb;

	private List<AccountInfo> list;
	private Handler handler;
	private TokenInfo tokenInfo;
	
	private static final int NET_ERROR = 2;
	private static final int SUCCEED = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_account);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		app = (App) this.getApplication();
		app.addActivity(this);

		pb = (ProgressBar) findViewById(R.id.pb_add_account);
		lvList = (ListView) findViewById(R.id.lv_add_account_list);
		
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {

				switch (msg.what) {
				case SUCCEED:
					pb.setVisibility(View.GONE);
					lvList.setAdapter(new AddAccountAdapter(list, AddAccountActivity.this));
					break;
				case NET_ERROR:
					showNetErrorDialog();
					break;
				}
			}
		};

		new Thread() {

			public void run() {

				try {
					tokenInfo = App.dao.getTokenInfo();
					if(app.otp==null) {
						
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
					
					list = NetService.getAccountList(app.IMEI,tokenInfo.SN,app.otp,AddAccountActivity.this);
					if (list != null) {
						handler.sendEmptyMessage(SUCCEED);
					} else {
						handler.sendEmptyMessage(NET_ERROR);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			};
		}.start();
		
		lvList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				
				Intent intent = new Intent();
				intent.putExtra("bindTag", list.get(position).name);
				intent.putExtra("entry", list.get(position).entry);
				intent.putExtra("accountDesc", list.get(position).des);
				intent.setClass(AddAccountActivity.this, BindActivity.class);
				AddAccountActivity.this.startActivity(intent);
			}
		});
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
									new Thread() {

										public void run() {

											try {
												tokenInfo = App.dao.getTokenInfo();
												if(app.otp==null) {
													
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
												
												list = NetService.getAccountList(app.IMEI,tokenInfo.SN,app.otp,AddAccountActivity.this);
												if (list != null) {
													handler.sendEmptyMessage(SUCCEED);
												} else {
													handler.sendEmptyMessage(NET_ERROR);
												}
											} catch (Exception e) {
												e.printStackTrace();
											}

										};
									}.start();
								} else {
									showNetErrorDialog();
								}
							}
						}).setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								AddAccountActivity.this
										.startActivity(new Intent(
												AddAccountActivity.this,
												PasscodePageActivity.class));
								AddAccountActivity.this.finish();
							}
						}).create();
		dialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				AddAccountActivity.this.startActivity(new Intent(
						AddAccountActivity.this, PasscodePageActivity.class));
				AddAccountActivity.this.finish();
			}
		});

		dialog.show();
	}
}
