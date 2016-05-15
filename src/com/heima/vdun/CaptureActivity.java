package com.heima.vdun;

import java.io.IOException;
import java.util.Vector;

import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import com.heima.vdun.util.Logger;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.heima.vdun.camera.CameraManager;
import com.heima.vdun.dao.TokenInfoDao;
import com.heima.vdun.decoding.CaptureActivityHandler;
import com.heima.vdun.decoding.InactivityTimer;
import com.heima.vdun.entity.TokenInfo;
import com.heima.vdun.global.App;
import com.heima.vdun.global.GlobalConstants;
import com.heima.vdun.service.NetService;
import com.heima.vdun.util.NetSeedEncrypt;
import com.heima.vdun.view.ViewfinderView;
import com.umeng.analytics.MobclickAgent;

/**
 * 扫描二维码的界面
 * 
 * @author Kevin
 * 
 */
public class CaptureActivity extends Activity implements Callback {

	private CaptureActivityHandler handler;
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	//private TextView txtResult;
	private InactivityTimer inactivityTimer;
	private MediaPlayer mediaPlayer;
	private boolean playBeep;
	private static final float BEEP_VOLUME = 0.10f;
	private boolean vibrate;
	private ProgressDialog dialog;
	private Handler mHandler;
	private static final int BIND_FAILED = 0;
	private static final int BIND_SUCCEED = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scan_semacode);
		App app = (App) this.getApplication();
		app.addActivity(this);
		// 初始化 CameraManager
		CameraManager.init(getApplication());
		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		//txtResult = (TextView) findViewById(R.id.txtResult);
		hasSurface = false;
		inactivityTimer = new InactivityTimer(this);

		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				
				switch (msg.what) {
				case BIND_FAILED:
					Toast.makeText(CaptureActivity.this,
							R.string.bind_error, Toast.LENGTH_SHORT).show();

					// 三秒中之后跳转到绑定页面，是为了让toast显示结束之后再进行跳转
					mHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							CaptureActivity.this.finish();
							Intent i = new Intent();
							i.setClass(CaptureActivity.this, CaptureActivity.class);
							startActivity(i);
						}
					}, 1000);
					break;
				case BIND_SUCCEED:
					/*Toast.makeText(CaptureActivity.this,
							R.string.bind_succeed, Toast.LENGTH_LONG).show();*/
					// 打开绑定成功后的页面
					Intent intent = new Intent();
					intent.setClass(CaptureActivity.this,
							PasscodePageActivity.class);
					startActivity(intent);
					CaptureActivity.this.sendBroadcast(new Intent(GlobalConstants.GET_OTP_SUCCEED));
					CaptureActivity.this.finish();
					break;
				}
			}
		};
	}

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			initCamera(surfaceHolder);
		} else {
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		decodeFormats = null;
		characterSet = null;

		playBeep = true;
		AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
		if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
			playBeep = false;
		}
		initBeepSound();
		vibrate = true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	protected void onDestroy() {
		inactivityTimer.shutdown();
		super.onDestroy();
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			return;
		} catch (RuntimeException e) {
			return;
		}
		if (handler == null) {
			handler = new CaptureActivityHandler(this, decodeFormats,
					characterSet);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;

	}

	public ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();

	}

	public void handleDecode(Result obj, Bitmap barcode) {
		inactivityTimer.onActivity();
		viewfinderView.drawResultBitmap(barcode);
		playBeepSoundAndVibrate();
		/*txtResult.setText(obj.getBarcodeFormat().toString() + ":"
				+ obj.getText());*/
		//String enc = "1DC39E6BFC8AAA3C281AAD55AF54AA94424E37492BB773588859BC6BDA1A998C4F10A3E320A1B28432A9BDF8B57C7AD3";
		//confirmInput(enc);
		confirmInput(obj.getText());
	}

	private void initBeepSound() {
		if (playBeep && mediaPlayer == null) {
			// The volume on STREAM_SYSTEM is not adjustable, and users found it
			// too loud,
			// so we now play on the music stream.
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(beepListener);

			AssetFileDescriptor file = getResources().openRawResourceFd(
					R.raw.beep);
			try {
				mediaPlayer.setDataSource(file.getFileDescriptor(), file
						.getStartOffset(), file.getLength());
				file.close();
				mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
				mediaPlayer.prepare();
			} catch (IOException e) {
				mediaPlayer = null;
			}
		}
	}

	private static final long VIBRATE_DURATION = 200L;

	private void playBeepSoundAndVibrate() {
		if (playBeep && mediaPlayer != null) {
			mediaPlayer.start();
		}
		if (vibrate) {
			Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_DURATION);
		}
	}

	/**
	 * When the beep has finished playing, rewind to queue up another one.
	 */
	private final OnCompletionListener beepListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.seekTo(0);
		}
	};
	private boolean tag;

	public void confirmInput(final String data) {
		
		tag = true;
		
		dialog = new ProgressDialog(this);
		dialog.setTitle(R.string.info);
		dialog.setMessage(this.getResources().getString(R.string.init_info));
		dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				
				tag = false;
				mHandler.sendEmptyMessage(BIND_FAILED);
			}
		});
		dialog.show();
		new Thread() {
			public void run() {

				try {
					NetSeedEncrypt de = new NetSeedEncrypt();
					String dec = de.getDesString(data);
					
					TokenInfo info = new TokenInfo();
					JSONObject jo = new JSONObject(dec);

					String sn = jo.optString("sn", null);
					String data = jo.optString("data", null);

					if (sn != null && data != null && !sn.equals("")
							&& !data.equals("")) {
						info.SN = sn;
						info.data = data;
					} else {
						mHandler.sendEmptyMessage(BIND_FAILED);
						dialog.dismiss();
						return;
					}
					Logger.i("Test", "start to check time...");
					Long timeOffset = NetService.checkTime();

					if(tag) {
						if (timeOffset != null) {
							info.tokenTime = timeOffset;
							//App app = (App) CaptureActivity.this.getApplication();
							
						} else {
							info.tokenTime = 0;
							//mHandler.sendEmptyMessage(BIND_FAILED);
						}
						TokenInfoDao dao = App.dao;
						dao.add(info);
						mHandler.sendEmptyMessage(BIND_SUCCEED);
					}

				} catch (Exception e) {
					mHandler.sendEmptyMessage(BIND_FAILED);
				}
				dialog.dismiss();
			};
		}.start();
	}
}