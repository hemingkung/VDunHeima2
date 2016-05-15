package com.heima.vdun.test;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.test.AndroidTestCase;

import com.heima.vdun.util.Logger;
import com.heima.vdun.util.NetSeedEncrypt;

public class MyTest extends AndroidTestCase {

	public void netService2Test() throws Throwable

	{

		//NetService2.getSecretInfo();
		float a = 378/34;
		Logger.i("Test", a+"");

	}

	public void testDesEncrypt2() throws Throwable {

		//String str = "{'sn':'33593674','data':'cD1xtJ9FrcRfgZg5gtkZ'}";
		NetSeedEncrypt de = new NetSeedEncrypt();
		//String enc = de.getEncString(str);
		String dec = de.getDesString("1013CCEAF030684812491152B238A146ADDEDF1FE55E5C886AEC6973612F13A86642B7F887C93D2A489C1077A1F76C42397259FBF2E7D35ECA7478314445DEA71A75591B73F71E54CA7478314445DEA7018C75D059F72B7754552DD0CCD229D04D4E73C358DF6E7788311FEC8088AB0D8CF5A53335E678B4EB9AE3C934DC8FF215D57BCE1C425117018C75D059F72B77BFD94B4CF647CA664EC813C7B69C8787B27CB0445E85067C83D13F82E3B59B835DFB058D0329077AB732993336645275F1D171984C259831542AE33E2642110019ADA222985527A87DA96264E46FEA67D458689BE4EAA050D856351F87B2B67D5CBAAEA4BBCE7A26F63C10500DF462AD6236D3B0CB300361FBE204EB73F318FF018C75D059F72B771AA52C6143231570ECB33B4D4816D213B93F40BAD6C73BDDEACF31F54E8E60AA6AEC6973612F13A8018C75D059F72B77372D8A9FBD51F4BAD56B811092DF7CFB743E58792660BAD622120963BED33D2864E531EA67518EA46AEC6973612F13A8018C75D059F72B77E87F02AAD624F978D56B811092DF7CFB743E58792660BAD64EE914B7F128720BCDC4F32A4210186F15D57BCE1C425117C398AB1D3A60E4006AEC6973612F13A8018C75D059F72B771013CCEAF0306848018C75D059F72B7700BD87D7836A3276A1527C29D08B30FFFBE204EB73F318FF018C75D059F72B7762A5B7FEE4712158A8450D7E4F23B77BCA7478314445DEA7018C75D059F72B77C1355BD7A60BE424819ACF311AC1B8966AEC6973612F13A8018C75D059F72B77482EA2286EB8B880E5413EEFAE8EBE7BEB5734580BED14355258051139838FEC9B523E018F547B53EA1DE2A20573D73B05D9847541A444EA63F7BE169283DA390BB1F34FDC6BA3CAB040987397F975E503F21BB0274A3F9ECA7478314445DEA7018C75D059F72B77A0139A42432726BE07D5FE32AC85B1D20D660603405F9B0583D13F82E3B59B83EC37D37A10887B751E7C73D72B2225A02ED5E6F0689FD6E1682BC8B3F2FC419727411F3CC32EEC69690400F3DBB2469683D13F82E3B59B836FA43F72241B8516B241082713B5BEE02ED5E6F0689FD6E1A818B488D71047EB5F3EA4520CBC60AA9B981FC48E0C6DCC88311FEC8088AB0D252A0687FADB7AE3C6B452C54EDBCD0D15623C6455CF1009FDDD94EFD27EF381");
		//Logger.i("Test", enc);
		Logger.i("Test", "decode--->"+dec);

	}
	
	public void testDipPx() {
		/*int dip = px2dip(getContext(),422);//422
		int dip2 = px2dip(getContext(),66);//422
		Logger.i("Test", "px2dip--->"+dip+","+dip2);*/
		 dip2px(getContext(), 100);
	}
	
	public void httpsTest() {
		try {
		
			//HttpGet request = new HttpGet("https://www.paypal.com/c2");
		//	HttpGet request = new HttpGet("https://api.weibo.com");
			HttpGet request = new HttpGet("https://vdun.weibo.com");
			HttpClient httpClient = new DefaultHttpClient();
			
			HttpResponse response = httpClient.execute(request);
			int code = response.getStatusLine().getStatusCode();
			Logger.i("Test", code + " 测试响应码");
			if (code == HttpStatus.SC_OK) {
				String str = EntityUtils.toString(response.getEntity());
				Logger.i("Test", str);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
	 */
	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		Logger.i("Test", "scale------------------>"+scale);
		return (int) (dpValue * scale + 0.5f);
	}
	 
	/**
	 * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
	 */
	public static int px2dip(Context context, float pxValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
	}

}
