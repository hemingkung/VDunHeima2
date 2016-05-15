package com.heima.vdun.view;

import com.heima.vdun.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.View;

public class CountdownIndicator extends View {
	
	private double mPhase;
	private final Paint mRemainingSectorPaint;
	private Context ctx;

	public CountdownIndicator(Context paramContext) {
		this(paramContext, null);
	}
	
	public CountdownIndicator(Context paramContext,
			AttributeSet paramAttributeSet) {
		super(paramContext, paramAttributeSet);
		this.mRemainingSectorPaint = new Paint();
		this.mRemainingSectorPaint.setAntiAlias(true);
		this.ctx = paramContext;
		
		//在此处无法直接获得控件的宽高，只能从资源文件中读取
		float x = ctx.getResources().getDimension(R.dimen.countdown_indicator_width) / 2;//返回的是像素值
		float y = ctx.getResources().getDimension(R.dimen.countdown_indicator_height) / 2;
	
		//Color.argb(255, 143, 201, 233),Color.argb(255, 255, 255, 235)
		//设置图像渐变
		RadialGradient rg = new RadialGradient( x, y, y, Color.argb(255, 143, 201, 233),Color.argb(255, 166, 212, 235), TileMode.MIRROR);
		this.mRemainingSectorPaint.setShader(rg);
	}

	protected void onDraw(Canvas paramCanvas) {
		
		float f1 = (float) (this.mPhase * 360.0D);
		float f2 = 270.0F - f1;  //实现顺时针的效果
		float f3 = getWidth() - 1;
		float f4 = getHeight() - 1;

		RectF localRectF = new RectF(1.0F, 1.0F, f3, f4);
		if (f2 < 360.0F) {
			Paint localPaint1 = this.mRemainingSectorPaint;
			paramCanvas.drawArc(localRectF, f2, f1, true, localPaint1);//顺时针效果
			//paramCanvas.drawArc(localRectF, -90, 30, true, localPaint1);//逆时针效果
		}
	}

	public void setPhase(double paramDouble) {
		if ((paramDouble < 0.0D) || (paramDouble > 1.0D)) {
			String str = "phase: " + paramDouble;
			throw new IllegalArgumentException(str);
		}
		this.mPhase = paramDouble;
		invalidate();
	}
}