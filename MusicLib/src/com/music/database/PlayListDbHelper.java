package com.music.database;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.music.entity.Song;

public class PlayListDbHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;// 数据库版本

	public static final String TABLE_PLAY_LIST = "table_playlist";
	private String sql_songs = "create table if not exists "
			+ TABLE_PLAY_LIST
			+ " (owner text,sid text,displayName text,trackName text,artisName text,album text,hashValue text,m4aUrl text,filePath text,parentPath text,mine_type text,size integer,duration integer,type integer,note text);";

	public PlayListDbHelper(Context context) {
		super(context, "playlist.db", null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(sql_songs);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("drop table if exists " + TABLE_PLAY_LIST);
		onCreate(db);
	}

	public void clearTable() {
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("drop table if exists " + TABLE_PLAY_LIST);
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
		db.insert(TABLE_PLAY_LIST, null, value);
	}

	public void delete(Song song, String owner) {
		SQLiteDatabase db = getWritableDatabase();
		String where = "owner=? and filePath=?";
		String[] whereArgs = new String[] { String.valueOf(owner),
				String.valueOf(song.getFilePath()) };
		if (!"-1".equals(song.getSid())) {
			where = "owner=? and sid=?";
			whereArgs = new String[] { String.valueOf(owner),
					String.valueOf(song.getSid()) };
		}
		db.delete(TABLE_PLAY_LIST, where, whereArgs);
		db.close();
	}

	public Song query(String owner, int index) {
		SQLiteDatabase db = getWritableDatabase();
		String sql = "select * from " + TABLE_PLAY_LIST + " where owner='"
				+ owner + "'";

		Cursor cursor = db.rawQuery(sql, null);
		cursor.moveToPosition(index);
		Song song = new Song();
		
		if (cursor.getCount() != 0) {
			String displayName = cursor.getString(cursor
					.getColumnIndex("displayName"));
			String filePath = cursor.getString(cursor
					.getColumnIndex("filePath"));
			String album = cursor.getString(cursor.getColumnIndex("album"));
			String artisName = cursor.getString(cursor
					.getColumnIndex("artisName"));
			int size = cursor.getInt(cursor.getColumnIndex("size"));
			int duration = cursor.getInt(cursor.getColumnIndex("duration"));
			String sid = cursor.getString(cursor.getColumnIndex("sid"));
			String m4aUrl = cursor.getString(cursor.getColumnIndex("m4aUrl"));
			String mine_type = cursor.getString(cursor
					.getColumnIndex("mine_type"));

			song.setFilePath(filePath);
			song.setDisplayName(displayName);
			song.setAlbum(album);
			song.setArtisName(artisName);
			song.setSize(size);
			song.setMine_type(mine_type);
			song.setSid(sid);
			song.setM4aUrl(m4aUrl);
			song.setDuration(duration);
		} else {
			song = null;
		}
		cursor.close();
		db.close();

		return song;
	}

	public Cursor getCursor(String owner) {
		SQLiteDatabase db = getWritableDatabase();
		String sql = "select * from " + TABLE_PLAY_LIST + " where owner='"
				+ owner + "'";
		Cursor cursor = db.rawQuery(sql, null);
		return cursor;
	}

}
