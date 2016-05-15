package com.heima.vdun.global;

import android.os.Environment;

public class GlobalConstants {
	
	//检查版本接口
	public static final String CHECK_VERSION_URL = "http://1.vduntest.sinaapp.com/update_check.php";//待定
	
	//获取卡号密钥接口
	public static final String GET_TOKEN_URL = "http://1.vduntest.sinaapp.com/manage/token/get_seed.php";
	
	//校准时间接口
	public static final String CHECK_TIME_URL = "http://1.vduntest.sinaapp.com/manage/token/get_svr_time.php";
	
	//验证绑定关系接口
	public static final String VERIFY_BIND = "http://1.vduntest.sinaapp.com/manage/token/verify_bind.php";
	
	//绑定接口
	public static final String BIND = "https://1.vduntest.sinaapp.com/manage/token/bind.php";
	
	//解绑接口
	public static final String UNBIND = "https://1.vduntest.sinaapp.com/manage/token/unbind.php";
	
	//获取账户列表页面
	public static final String GET_ACCOUNT_LIST = "http://1.vduntest.sinaapp.com/manage/token/account_list.php";
	
	//帮助页面
	public static final String HELP_URL = "http://vdisk.weibo.com/wapAndroidHelp";
	
	//获取卡号所绑定的所有帐号信息
	public static final String 	GET_VSN_UID_URL = "http://1.vduntest.sinaapp.com/manage/token/get_vsn_uid.php";
	
	//缓存存储目录
	public static String SAVE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Android/data/com.heima.vdun/files/cache/";
	
	//密钥刷新时间
	public static final int REFRESH_INTERVAL_SEC = 60;
	
	//发送更新countdown的消息码
	public static final int UPDATE_OTP = 100;
	
	//短信号码
	public static final String SMS_NUMBER  = "106900901001";
	
	//校验绑定的次数，60表示60秒，每秒校验一次
	public static final int VERIFY_BIND_TIMES = 60;
	
	//更新令牌时间的广播
	public static final String UPDATE_PASSCODE_TIME = "com.heima.vdun.update_passcode_time";
	
	//成功获取密钥后发送的广播
	public static final String GET_OTP_SUCCEED = "com.heima.vdun.get_otp_succeed";
}
