package com.heima.vdun;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.heima.vdun.global.App;
import com.umeng.analytics.MobclickAgent;

public class UnBindSucceedActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.unbind_succeed);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		App app = (App) this.getApplication();
		app.addActivity(this);
		
		Button bt = (Button) findViewById(R.id.bt_unbind_succeed);
		bt.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(UnBindSucceedActivity.this, PasscodePageActivity.class);
				UnBindSucceedActivity.this.startActivity(intent);
				UnBindSucceedActivity.this.finish();
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
			
			Intent intent = new Intent(UnBindSucceedActivity.this, PasscodePageActivity.class);
			UnBindSucceedActivity.this.startActivity(intent);
			UnBindSucceedActivity.this.finish();
			return true;
		}
		return false;
	}
}
