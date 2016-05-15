package com.heima.vdun.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

public class TokenInfoDBHelper extends SQLiteOpenHelper {

	public TokenInfoDBHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
	}
	
	public TokenInfoDBHelper(Context context) {
		super(context, "vdun.db", null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "create table token_conf(_id integer primary key autoincrement,sn varchar(20),tokentime varchar(20),data varchar(50))";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}
}
