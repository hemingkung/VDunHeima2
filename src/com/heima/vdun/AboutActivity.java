package com.heima.vdun;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.widget.TextView;

import com.heima.vdun.global.App;
import com.umeng.analytics.MobclickAgent;

/**
 * "关于"页面
 * 
 * @author Kevin
 * 
 */
public class AboutActivity extends Activity {

	TextView tvVersion;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		
		App app = (App) this.getApplication();
		app.addActivity(this);
		
		tvVersion = (TextView) findViewById(R.id.tv_about_version);

		try {
			PackageManager pm = this.getPackageManager();
			PackageInfo packageInfo = pm.getPackageInfo(this.getPackageName(),
					PackageManager.GET_UNINSTALLED_PACKAGES);
			String version = packageInfo.versionName;
			tvVersion.setText(getResources().getString(R.string.about_version)
					+ version);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
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
