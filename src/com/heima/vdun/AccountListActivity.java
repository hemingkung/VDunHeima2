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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.heima.vdun.adapter.AccountListAdapter;
import com.heima.vdun.entity.AccountBean;
import com.heima.vdun.entity.TokenInfo;
import com.heima.vdun.global.App;
import com.heima.vdun.global.GlobalConstants;
import com.heima.vdun.service.NetService;
import com.heima.vdun.util.LocalSeedEncrypt;
import com.heima.vdun.util.PasscodeGenerator;
import com.umeng.analytics.MobclickAgent;

/**
 * "关于"页面
 * 
 * @author Kevin
 * 
 */
public class AccountListActivity extends Activity implements OnClickListener {

	private App app;
	private Button btSkip;
	private ListView lvList;
	private TextView tvInfo;
	private ProgressBar pb;
	LayoutInflater inflater;
	
	private List<AccountBean> list;
	private Handler handler;

	private static final int NET_ERROR = 2;
	private static final int SUCCEED = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account_list);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		app = (App) this.getApplication();
		app.addActivity(this);
		inflater = this.getLayoutInflater();

		btSkip = (Button) findViewById(R.id.bt_account_list_skip);
		btSkip.setOnClickListener(this);
		pb = (ProgressBar) findViewById(R.id.pb_account_list);

		if (getIntent().getBooleanExtra("hideSkip", false)) {
			btSkip.setVisibility(View.GONE);
		}
		lvList = (ListView) findViewById(R.id.lv_account_list);
		tvInfo = (TextView) findViewById(R.id.tv_account_list_info);
		
		View foot = inflater.inflate(R.layout.account_list_foot, null);
		lvList.addFooterView(foot);
		
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {

				switch (msg.what) {
				case SUCCEED:
					pb.setVisibility(View.GONE);
					if(list.size()==0) {
						tvInfo.setText(R.string.account_list_info2);
					}
					lvList.setAdapter(new AccountListAdapter(list, AccountListActivity.this));
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
					
					TokenInfo info = App.dao.getTokenInfo();
					
					if(info!=null) {
						list = NetService.getVsnUid(app.IMEI,info.SN , app.otp, AccountListActivity.this);
						if (list != null) {
							handler.sendEmptyMessage(SUCCEED);
						} else {
							handler.sendEmptyMessage(NET_ERROR);
						}
					}else {
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
				Intent i = new Intent();
				if(position==list.size()) {
					i.setClass(AccountListActivity.this, AddAccountActivity.class);
				}else {
					
					i.putExtra("accountInfo", list.get(position));
					i.setClass(AccountListActivity.this, AccountInfoActivity.class);
				}
				AccountListActivity.this.startActivity(i);
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			this.startActivity(new Intent(this, PasscodePageActivity.class));
			this.finish();
			return true;
		}
		return false;
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.bt_account_list_skip:
			this.startActivity(new Intent(this, PasscodePageActivity.class));
			this.finish();
			break;
		default:
			break;
		}
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
											if (checkNet()) {
											/*	list = NetService
														.getAccountList(
																app.IMEI,
																AddAccountActivity.this);*/
												if (list != null) {
													handler
															.sendEmptyMessage(SUCCEED);
												} else {
													handler
															.sendEmptyMessage(NET_ERROR);
												}
											} else {
												showNetErrorDialog();
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
								AccountListActivity.this
										.startActivity(new Intent(
												AccountListActivity.this,
												PasscodePageActivity.class));
								AccountListActivity.this.finish();
							}
						}).create();
		dialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				AccountListActivity.this.startActivity(new Intent(
						AccountListActivity.this, PasscodePageActivity.class));
				AccountListActivity.this.finish();
			}
		});

		dialog.show();
	}
}
