package com.music.database;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.music.entity.Song;

public class ScanListDbHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;// 数据库版本

	public static final String TABLE_SEARCH_LIST = "table_search_list";
	private String sql_songs = "create table if not exists "
			+ TABLE_SEARCH_LIST
			+ " (owner text,sid text,displayName text,trackName text,artisName text,album text,hashValue text,m4aUrl text,filePath text,parentPath text,mine_type text,size integer,duration integer,type integer,note text);";

	public ScanListDbHelper(Context context) {
		super(context, "music.db", null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(sql_songs);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("drop table if exists " + TABLE_SEARCH_LIST);
		onCreate(db);
	}

	public void clearTable() {
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("drop table if exists " + TABLE_SEARCH_LIST);
		db.execSQL(sql_songs);
		db.close();
	}

	public void insert(List<Song> list, String owner) {
		if (list == null)
			return;
		SQLiteDatabase db = getWritableDatabase();
		for (int i = 0; i < list.size(); i++) {
			ActionInsert(list.get(i), owner, db);
		}
		db.close();
	}

	public int insert(Song song, String owner) {
		int result = 0;
		SQLiteDatabase db = getWritableDatabase();
		ActionInsert(song, owner, db);
		db.close();
		return result;
	}

	private void ActionInsert(Song song, String owner, SQLiteDatabase db) {
		ContentValues value = new ContentValues();
		value.put("owner", owner);
		value.put("sid", song.getSid());
		value.put("displayName", song.getDisplayName());
		value.put("filePath", song.getFilePath());
		value.put("album", song.getAlbum());
		value.put("artisName", song.getArtisName());
		value.put("m4aUrl", song.getM4aUrl());
		value.put("mine_type", song.getMine_type());
		value.put("size", song.getSize());
		value.put("duration", song.getDuration());
		value.put("parentPath", song.getParentPath());
		db.insert(TABLE_SEARCH_LIST, null, value);
	}

	private String sql_disc_songs = "create table if not exists "
			+ MusicDbHelper.TABLE_DISC_SONG
			+ " (owner text,sid text,displayName text,trackName text,artisName text,album text,hashValue text,m4aUrl text,filePath text,parentPath text,mine_type text,size integer,duration integer,type integer,note text);";

	private void ClearDiscTable(SQLiteDatabase db) {
		db.execSQL("drop table if exists " + MusicDbHelper.TABLE_DISC_SONG);
		db.execSQL(sql_disc_songs);
	}

	public boolean CopyToDiscDb(List<String> dirs) {
		SQLiteDatabase db = getWritableDatabase();
		ClearDiscTable(db);
		for (int i = 0; i < dirs.size(); i++) {
			String parentPath = dirs.get(i);
			String sql = "insert into " + MusicDbHelper.TABLE_DISC_SONG
					+ " select * from " + TABLE_SEARCH_LIST
					+ " where parentPath='" + parentPath + "'";
			db.execSQL(sql);
		}
		db.close();
		return true;
	}

}
