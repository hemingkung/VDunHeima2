package com.heima.vdun.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

import com.heima.vdun.service.UpdateOTPService;

public class PasscodeWidget extends AppWidgetProvider {

	@Override
	public void onEnabled(Context context) {//第一次添加该部件的时候调用
		context.startService(new Intent(context, UpdateOTPService.class));
	}

	@Override
	public void onDisabled(Context context) {
		Intent intent = new Intent(context, UpdateOTPService.class);
		intent.putExtra("stop", true);//说明是用户手动删除widget的，此标记true会停掉service
		context.startService(intent);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		context.startService(new Intent(context, UpdateOTPService.class));
	}
		
}
