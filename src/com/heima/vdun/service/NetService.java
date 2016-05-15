package com.heima.vdun.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;

import com.heima.vdun.entity.AccountBean;
import com.heima.vdun.entity.AccountInfo;
import com.heima.vdun.entity.BindResult;
import com.heima.vdun.entity.TokenInfo;
import com.heima.vdun.entity.VersionInfo;
import com.heima.vdun.global.GlobalConstants;
import com.heima.vdun.util.Logger;

public class NetService {

	//private static HttpClient httpClient = new DefaultHttpClient();//默认的额httpclient不能访问https协议
	private static HttpClient httpClient = HttpManager.sClient;//这个经过封装的client既可访问http，也可以访问https
	
	// 从服务器获取版本信息
	public static VersionInfo getVersion() {

		VersionInfo info = new VersionInfo();

		HttpGet request = new HttpGet(GlobalConstants.CHECK_VERSION_URL);

		try {
			HttpResponse response = httpClient.execute(request);
			int code = response.getStatusLine().getStatusCode();
			Logger.i("Test", code + " 版本响应码");
			if (code == HttpStatus.SC_OK) {
				String str = EntityUtils.toString(response.getEntity());
				JSONObject jo = new JSONObject(str);
				info.versionName = jo.getString("versionName");
				info.versionCode = jo.getString("versionCode");
				info.description = jo.getString("description");
				info.downloadUrl = jo.getString("downloadUrl");
				return info;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	//获取卡号和密钥
	public static TokenInfo getTokenInfo(String IMEI, Context ctx) {

		try {
			String url = GlobalConstants.GET_TOKEN_URL;
			HttpGet request = new HttpGet(url);
			TokenInfo info = new TokenInfo();

			HttpResponse response = httpClient.execute(request);
			int code = response.getStatusLine().getStatusCode();
			Logger.i("Test", code + " 获取卡号响应码");
			if (code == HttpStatus.SC_OK) {

				String str = EntityUtils.toString(response.getEntity());
				JSONObject jo = new JSONObject(str);

				String sn = jo.optString("sn", null);
				String data = jo.optString("data", null);

				if (sn != null && data != null && !sn.equals("")
						&& !data.equals("")) {
					info.SN = sn;
					info.data = data;
				} else {
					return null;
				}
				Logger.i("Test", "start to check time...");
				Long timeOffset = checkTime();

				if (timeOffset != null) {
					info.tokenTime = timeOffset;
					return info;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	//检查时间
	public static Long checkTime() {
		// {" svr_time":" 1318406513"}
		HttpGet request = new HttpGet(GlobalConstants.CHECK_TIME_URL);
		try {
			HttpResponse response = httpClient.execute(request);
			int code = response.getStatusLine().getStatusCode();
			Logger.i("Test", code + " 校准响应码");
			if (code == HttpStatus.SC_OK) {
				String str = EntityUtils.toString(response.getEntity());
				JSONObject jo = new JSONObject(str);

				String str2 = jo.optString("svr_time", null);
				if (str2 == null) {
					return null;
				}

				long serverTime = Long.parseLong(str2);
				long timeOffset = System.currentTimeMillis() - serverTime
						* 1000;
				return timeOffset;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	//验证手机号码是否和微博绑定
	public static int verifyBind(String cardNumber, String IMEI, String otp, Context ctx) {

		try {
			HttpPost request = new HttpPost(GlobalConstants.VERIFY_BIND);
			
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("sn", cardNumber));
			params.add(new BasicNameValuePair("token", otp));
			
			request.setEntity(new UrlEncodedFormEntity(params));

			HttpResponse response = httpClient.execute(request);

			int code = response.getStatusLine().getStatusCode();
			Logger.i("Test", code + " 验证响应码");
			if (code == HttpStatus.SC_OK) {

				String str = EntityUtils.toString(response.getEntity());
				//Logger.i("Test", "verify bind result-->" + str);
				JSONObject jo = new JSONObject(str);

				String result = jo.optString("result", null);
				if (result == null) {
					return -1;
				}
				int id = Integer.parseInt(result);
				Logger.i("Test", "verify bind result-->" + id);
				return id;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	
	//绑定帐号
	public static BindResult bind(String account, String cardNumber,
			String otp, String type, String IMEI, Context ctx) {
		try {
			HttpPost request = new HttpPost(GlobalConstants.BIND);

			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("username", account));
			params.add(new BasicNameValuePair("sn", cardNumber));
			params.add(new BasicNameValuePair("token", otp));
			params.add(new BasicNameValuePair("type", type));

			request.setEntity(new UrlEncodedFormEntity(params,HTTP.UTF_8));

			HttpResponse response = httpClient.execute(request);

			int code = response.getStatusLine().getStatusCode();
			Logger.i("Test", code + " 绑定响应码");
			if (code == HttpStatus.SC_OK) {

				String str = EntityUtils.toString(response.getEntity());
				JSONObject jo = new JSONObject(str);

				String result = jo.optString("result", null);
				String msg = jo.optString("msg", null);
				if (result == null) {
					return null;
				}
				BindResult br = new BindResult();
				br.result = Integer.parseInt(result);
				br.message = msg;
				return br;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * 0 解绑失败 1 解绑成功 2 参数错误或内部错误
	 */
	public static int unBind(String account, String cardNumber, String otp,
			String type, String IMEI, Context ctx) {
		try {
			HttpPost request = new HttpPost(GlobalConstants.UNBIND);

			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("username", account));
			params.add(new BasicNameValuePair("sn", cardNumber));
			params.add(new BasicNameValuePair("token", otp));
			params.add(new BasicNameValuePair("type", type));
			
			request.setEntity(new UrlEncodedFormEntity(params,HTTP.UTF_8));

			HttpResponse response = httpClient.execute(request);

			int code = response.getStatusLine().getStatusCode();
			Logger.i("Test", code + " 解绑响应码");
			if (code == HttpStatus.SC_OK) {

				String str = EntityUtils.toString(response.getEntity());
				
				Logger.i("Test", "unbind------->"+str);
				
				JSONObject jo = new JSONObject(str);

				String result = jo.optString("result", null);
				if (result == null) {
					return -1;
				}
				int id = Integer.parseInt(result);
				return id;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	
	//获取可绑定的账户种类
	public static List<AccountInfo> getAccountList(String IMEI, String cardNumber, String otp, Context ctx) {

		List<AccountInfo> list = new ArrayList<AccountInfo>();
		try {
			String url = GlobalConstants.GET_ACCOUNT_LIST;
			HttpGet request = new HttpGet(url);

			HttpResponse response = httpClient.execute(request);
			int code = response.getStatusLine().getStatusCode();
			Logger.i("Test", code + " 账户列表响应码");
			if (code == HttpStatus.SC_OK) {

				String result = EntityUtils.toString(response.getEntity());
				if (TextUtils.isEmpty(result)) {
					String cache = readFromCache();
					if (cache != null) {
						result = cache;
					} else {
						return null;
					}
				}
				write2Cache(result);

				JSONObject js = new JSONObject(result);
				//String result = jo.optString("result", null);

				
				//NetSeedEncrypt de = new NetSeedEncrypt();
				//String dec = de.getDesString(result);
				
				//Logger.i("Test", "accountlist---------------->"+dec);
				//JSONObject js = new JSONObject(dec);

				String total = js.optString("total", null);
				JSONArray accountList = js.getJSONArray("account_list");

				if (total != null && accountList != null) {
					int count = Integer.parseInt(total);
					for (int i = 0; i < count; i++) {

						JSONObject info = (JSONObject) accountList.get(i);
						String id = info.optString("id", null);
						String name = info.optString("name", null);
						String entry = info.optString("entry", null);
						String icon_url = info.optString("icon_url", null);
						String des = info.optString("text", null);
						String createTime = info.optString("create_time", null);
						String updateTime = info.optString("update_time", null);

						if (name != null && entry != null && !name.equals("")
								&& !entry.equals("")) {
							AccountInfo accountInfo = new AccountInfo();
							accountInfo.id = id;
							accountInfo.name = name;
							accountInfo.entry = entry;
							accountInfo.iconUrl = icon_url;
							accountInfo.des = des;
							accountInfo.createTime = createTime;
							accountInfo.updateTime = updateTime;
							list.add(accountInfo);
						}
					}
					return list;
				} else {
					return null;
				}

			} else {
				String cache = readFromCache();
				return getList(list,cache);
			}

		} catch (Exception e) {
			e.printStackTrace();
			String cache = readFromCache();
			return getList(list,cache);
		}
	}
	
	private static List<AccountInfo> getList(List<AccountInfo> list,String result) {
		try {
			if (result != null) {
				//NetSeedEncrypt de = new NetSeedEncrypt();
				//String dec = de.getDesString(result);
				JSONObject js = new JSONObject(result);

				String total = js.optString("total", null);
				JSONArray accountList = js.getJSONArray("account_list");

				if (total != null && accountList != null) {
					int count = Integer.parseInt(total);
					for (int i = 0; i < count; i++) {

						JSONObject info = (JSONObject) accountList.get(i);
						String id = info.optString("id", null);
						String name = info.optString("name", null);
						String entry = info.optString("entry", null);
						String icon_url = info.optString("icon_url", null);
						String des = info.optString("text", null);
						String createTime = info.optString("create_time",
								null);
						String updateTime = info.optString("update_time",
								null);

						if (name != null && entry != null
								&& !name.equals("") && !entry.equals("")) {
							AccountInfo accountInfo = new AccountInfo();
							accountInfo.id = id;
							accountInfo.name = name;
							accountInfo.entry = entry;
							accountInfo.iconUrl = icon_url;
							accountInfo.des = des;
							accountInfo.createTime = createTime;
							accountInfo.updateTime = updateTime;
							list.add(accountInfo);
						}
					}
					return list;
				} else {
					return null;
				}
			} else {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static List<AccountBean> getVsnList(List<AccountBean> list,String result) {
		try {
			if (result != null) {
				
				//NetSeedEncrypt de = new NetSeedEncrypt();
				//String dec = de.getDesString(result);
				JSONObject js = new JSONObject(result);
				
				String total = js.optString("total", null);
				
				if (total != null) {
					int count = Integer.parseInt(total);
					if(count>0) {
						JSONArray accountList = js.getJSONArray("vsn_uid");
						for (int i = 0; i < count; i++) {
							
							JSONObject info = (JSONObject) accountList.get(i);
							String id = info.optString("id", null);
							String name = info.optString("name", null);
							String entry = info.optString("entry", null);
							String icon_url = info.optString("icon_url", null);
							String account = info.optString("account", null);
							String createTime = info.optString("create_time", null);

							if (name != null && entry != null && account!=null&&!name.equals("")
									&& !entry.equals("") && !account.equals("")) {
								AccountBean bean = new AccountBean();
								bean.id = id;
								bean.account = account;
								bean.name = name;
								bean.entry = entry;
								bean.iconUrl = icon_url;
								bean.createTime = createTime;
								list.add(bean);
							}
						}
					}
					return list;
				} else {
					return null;
				}
			} else {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void write2Cache(String str) {

		try {
			File cacheFile = new File(GlobalConstants.SAVE_DIR + "account_list");
			FileOutputStream fos = new FileOutputStream(cacheFile);
			fos.write(str.getBytes("UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	private static void write2VsnCache(String str) {
		
		try {
			File cacheFile = new File(GlobalConstants.SAVE_DIR + "vsn_list");
			FileOutputStream fos = new FileOutputStream(cacheFile);
			fos.write(str.getBytes("UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	private static String readFromCache() {

		try {
			FileInputStream fileInputStream = new FileInputStream(
					GlobalConstants.SAVE_DIR + "account_list");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[512];
			int count = 0;
			while ((count = fileInputStream.read(buffer)) != -1) {
				baos.write(buffer, 0, count);
			}
			return new String(baos.toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	private static String vsnReadFromCache() {
		
		try {
			FileInputStream fileInputStream = new FileInputStream(
					GlobalConstants.SAVE_DIR + "vsn_list");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[512];
			int count = 0;
			while ((count = fileInputStream.read(buffer)) != -1) {
				baos.write(buffer, 0, count);
			}
			return new String(baos.toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	//获取已经绑定的用户列表
	public static List<AccountBean> getVsnUid(String IMEI, String cardNumber, String otp, Context ctx) {
		
		List<AccountBean> list = new ArrayList<AccountBean>();
		try {
			String url = GlobalConstants.GET_VSN_UID_URL + "?sn=" + cardNumber;
			HttpGet request = new HttpGet(url);

			HttpResponse response = httpClient.execute(request);
			int code = response.getStatusLine().getStatusCode();
			Logger.i("Test", code + " 绑定帐号列表响应码");
			if (code == HttpStatus.SC_OK) {
				
				String result = EntityUtils.toString(response.getEntity());
				Logger.i("Test", "get vsn account----->"+result);
				
				if (TextUtils.isEmpty(result)) {
					String cache = vsnReadFromCache();
					if (cache != null) {
						result = cache;
					} else {
						return null;
					}
				}
				write2VsnCache(result);
				JSONObject js = new JSONObject(result);
			//	String result = jo.optString("result", null);

			
				//NetSeedEncrypt de = new NetSeedEncrypt();
				//String dec = de.getDesString(result);
				
				//Logger.i("Test", "----------vsnuid--->"+ dec);
				//JSONObject js = new JSONObject(dec);

				String total = js.optString("total", null);
				if (total != null) {
					int count = Integer.parseInt(total);
					if(count>0) {
						JSONArray accountList = js.getJSONArray("vsn_uid");
						for (int i = 0; i < count; i++) {

							JSONObject info = (JSONObject) accountList.get(i);
							String id = info.optString("id", null);
							String name = info.optString("name", null);
							String entry = info.optString("entry", null);
							String icon_url = info.optString("icon_url", null);
							String account = info.optString("account", null);
							String createTime = info.optString("create_time", null);

							if (name != null && entry != null && account!=null&&!name.equals("")
									&& !entry.equals("") && !account.equals("")) {
								AccountBean bean = new AccountBean();
								bean.id = id;
								bean.account = account;
								bean.name = name;
								bean.entry = entry;
								bean.iconUrl = icon_url;
								bean.createTime = createTime;
								list.add(bean);
							}
						}
					}
					return list;
				} else {
					return null;
				}

			} else {
				String cache = vsnReadFromCache();
				return getVsnList(list,cache);
			}

		} catch (Exception e) {
			e.printStackTrace();
			String cache = vsnReadFromCache();
			return getVsnList(list,cache);
		}
	}
}
