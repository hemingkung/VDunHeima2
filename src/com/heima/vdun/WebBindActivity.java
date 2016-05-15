package com.heima.vdun;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.heima.vdun.global.App;
import com.umeng.analytics.MobclickAgent;

/**
 * 网页绑定页面
 * 
 * @author Kevin
 * 
 */
public class WebBindActivity extends Activity implements OnClickListener {

	Button btStartCamera;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_camera);
		App app = (App) this.getApplication();
		app.addActivity(this);

		btStartCamera = (Button) findViewById(R.id.bt_start_camera);
		btStartCamera.setOnClickListener(this);
		getWindow().setFormat(PixelFormat.RGBA_8888);
	}

	@Override
	public void onClick(View v) {
		Intent i = new Intent();
		switch (v.getId()) {
		// 开启摄像头进行扫描
		case R.id.bt_start_camera:
			i.setClass(this, CaptureActivity.class);
			startActivity(i);
			this.finish();
			break;
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
