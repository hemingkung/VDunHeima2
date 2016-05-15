package com.heima.vdun.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.heima.vdun.entity.TokenInfo;
import com.heima.vdun.util.LocalSeedEncrypt;

public class TokenInfoDao {

	TokenInfoDBHelper helper;
	String IMEI;
	public TokenInfoDao(Context context,String IMEI) {
		helper = new TokenInfoDBHelper(context);
		this.IMEI = IMEI;
	}

	public void add(TokenInfo info) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			if (db.isOpen()) {
				String sql = "insert into token_conf(sn,tokentime,data) values(?,?,?)";
				// 需要对密钥加密后再保存到数据库中
				LocalSeedEncrypt des = new LocalSeedEncrypt(IMEI);
				String data = des.getEncString(info.data);
				db.execSQL(sql, new Object[] { info.SN, info.tokenTime, data });
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.close();
		}

	}

	public void updateTime(Long timeOffset) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			if (db.isOpen()) {
				String sql = "update token_conf set tokentime=?";
				db.execSQL(sql, new Object[] { timeOffset });
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.close();
		}
	}

	public TokenInfo getTokenInfo() {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = null;
		try {
			if (db.isOpen()) {
				String sql = "select * from token_conf";
				cursor = db.rawQuery(sql,null);
				TokenInfo info = new TokenInfo();
				if (cursor.moveToFirst()) {
					String sn = cursor.getString(cursor.getColumnIndex("sn"));
					String tokenTime = cursor.getString(cursor
							.getColumnIndex("tokentime"));
					String data = cursor.getString(cursor
							.getColumnIndex("data"));

					info.SN = sn;
					info.tokenTime = Long.parseLong(tokenTime);
					info.data = data;
				} else {
					return null;
				}
				return info;
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(cursor!=null) {
				cursor.close();
			}
			db.close();
		}
		return null;
	}

	public void delete() {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			if (db.isOpen()) {
				String sql = "delete from token_conf";
				db.execSQL(sql);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.close();
		}
	}
}
