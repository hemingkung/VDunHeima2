package com.heima.vdun;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.heima.vdun.entity.AccountBean;
import com.heima.vdun.global.App;
import com.heima.vdun.util.ImageDownloader;
import com.umeng.analytics.MobclickAgent;

public class AccountInfoActivity extends Activity {

	ImageView ivIcon;
	TextView tvAccount;
	Button btUnbind;
	App app;
	private AccountBean info;
	ImageDownloader downloader;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account_info);
		app = (App) this.getApplication();
		app.addActivity(this);
		
		ivIcon = (ImageView) findViewById(R.id.iv_account_info_icon);
		tvAccount = (TextView) findViewById(R.id.tv_account_info_account);
		btUnbind = (Button) findViewById(R.id.bt_account_info_unbind);
		
		Intent intent = getIntent();
		info = (AccountBean) intent.getSerializableExtra("accountInfo");
		
		tvAccount.setText(info.account);
		
		downloader = new ImageDownloader();
		if(info.iconUrl!=null&&!info.iconUrl.trim().equals("")) {
			downloader.download(info.iconUrl, ivIcon);
		}else {
			ivIcon.setImageResource(R.drawable.default_icon);
		}
		
		btUnbind.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.putExtra("unBindTag", info.name);
				intent.putExtra("entry", info.entry);
				intent.setClass(AccountInfoActivity.this,UnBindActivity.class);
				AccountInfoActivity.this.startActivity(intent);
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
}
