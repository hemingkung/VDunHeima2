package com.heima.vdun;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Window;
import android.view.WindowManager;

import com.heima.vdun.dao.TokenInfoDao;
import com.heima.vdun.entity.TokenInfo;
import com.heima.vdun.global.App;
import com.umeng.analytics.MobclickAgent;
/**
 * splash页面
 * 
 * @author Kevin
 * 
 */
public class SplashActivity extends Activity {

	private boolean isFirstUsed = true;
	private SharedPreferences sp;
	private Thread thread;
	private TokenInfo tokenInfo;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//有盟统计，如果出错的话可以将异常信息反馈到网上
		MobclickAgent.onError(this);
		
		// 设置全屏
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.splash);

		//加上这句话，可以保证屏幕的渐变效果正常显示
		getWindow().setFormat(PixelFormat.RGBA_8888);

		sp = this.getSharedPreferences("vdun", MODE_PRIVATE);

		TokenInfoDao dao = App.dao;
		tokenInfo = dao.getTokenInfo();

		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					//页面停留500毫秒
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				jump2NextPage();
			}
		});

		isFirstUsed = sp.getBoolean("isFirstUsed", true);
		if (isFirstUsed) {
			
			//第一次使用的话，提示是否创建快捷方式
			addShortcutDialog();
			Editor editor = sp.edit();
			editor.putBoolean("isFirstUsed", false);
			editor.commit();
		} else {
			thread.start();
		}
	}

	/**
	 * 询问是否创建快捷方式的对话框
	 */
	private void addShortcutDialog() {

		AlertDialog dialog = new AlertDialog.Builder(this)
				.setTitle(R.string.info)
				.setMessage(
						this.getResources().getString(
								R.string.create_shortcut_confirm))
				.setIcon(android.R.drawable.ic_dialog_info)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								addShortcut();
							}
						}).setNegativeButton(R.string.no, null).create();

		
		//如果dialog消失了，就开启线程，跳转页面
		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				thread.start();
			}
		});

		dialog.show();
	}
	
	/**
	 * 执行增加桌面快捷方式的逻辑
	 */
	private void addShortcut() {
		
		//设置快捷方式要跳转的activity
		Intent intent = new Intent();
		intent.setClass(this, SplashActivity.class);
		
		//要加上下面两句，否则应用被卸载之后，快捷方式不会消失
		intent.setAction("android.intent.action.MAIN");
		intent.addCategory("android.intent.category.LAUNCHER");

		Intent addShortcut = new Intent(
				"com.android.launcher.action.INSTALL_SHORTCUT");
		Parcelable icon = Intent.ShortcutIconResource.fromContext(this,
				R.drawable.icon);//快捷方式图标
		addShortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME,
				getString(R.string.app_name));//设置名称
		addShortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);//点击快捷方式后执行的操作
		addShortcut.putExtra("duplicate", false);//是否允许快捷方式重复
		addShortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
		sendBroadcast(addShortcut);
	}

	// 跳转到系统主页面
	private void jump2NextPage() {

		Intent intent = new Intent();
		
		//如果已经有token，就直接跳转到密码显示页面，否则跳转到初始化页面
		if (tokenInfo != null) {
			intent.setClass(this, PasscodePageActivity.class);
		} else {
			intent.setClass(this, InitActivity.class);
		}

		startActivity(intent);
		this.finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		//有盟统计
		MobclickAgent.onResume(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		//有盟统计
		MobclickAgent.onPause(this);
	}
}