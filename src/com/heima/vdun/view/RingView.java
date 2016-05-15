package com.heima.vdun.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class RingView extends View {
	
	private final Paint paint;
	private final Context ctx;

	public RingView(Context paramContext) {
		this(paramContext, null);
	}
	
	public RingView(Context paramContext,
			AttributeSet paramAttributeSet) {
		super(paramContext, paramAttributeSet);
		this.ctx = paramContext;
		this.paint = new Paint();
		this.paint.setAntiAlias(true);//消除锯齿
		this.paint.setStyle(Paint.Style.STROKE);//绘制空心圆
	}

	protected void onDraw(Canvas paramCanvas) {
		
		int center = getWidth()/2;
		int innerCircle = dip2px(ctx,83); //设置内圆半径
		int ringWidth = dip2px(ctx,5);  //设置圆环宽度
		
		//绘制内圆
		this.paint.setARGB(155, 167, 190, 206);
		this.paint.setStrokeWidth(2);
		paramCanvas.drawCircle(center,center, innerCircle, this.paint);
		
		//绘制圆环
		this.paint.setARGB(255, 212 ,225, 233);
		this.paint.setStrokeWidth(ringWidth);
		paramCanvas.drawCircle(center,center, innerCircle+1+ringWidth/2, this.paint);
		
		//绘制外圆
		this.paint.setARGB(155, 167, 190, 206);
		this.paint.setStrokeWidth(2);
		paramCanvas.drawCircle(center,center, innerCircle+ringWidth, this.paint);
	}
	
	/**
	 * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
	 */
	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}
}