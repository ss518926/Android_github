package com.music.database;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.music.entity.Song;

public class MusicDbHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;// 数据库版本

	public static final int TYPE_MYLIST = 0;// 本地音乐
	public static final int TYPE_DISC = 1;// 本地音乐
	public static final int TYPE_ONLINE = 2;// 在线音乐
	public static final int TYPE_FAVORITE = 3;// 我的收藏

	public static final String TABLE_MYLIST_SONG = "mylist_song";
	public static final String TABLE_DISC_SONG = "disc_song";
	public static final String TABLE_ONLINE_SONG = "online_song";
	public static final String TABLE_FAVORITE_SONG = "favorite_song";

	private String GetOperateTable(int type) {
		String table = "";
		if (type == TYPE_MYLIST) {
			table = TABLE_MYLIST_SONG;
		} else if (type == TYPE_DISC) {
			table = TABLE_DISC_SONG;
		} else if (type == TYPE_ONLINE) {
			table = TABLE_ONLINE_SONG;
		} else if (type == TYPE_FAVORITE) {
			table = TABLE_FAVORITE_SONG;
		}
		return table;
	}

	private String sql_mylist_songs = "create table if not exists "
			+ TABLE_MYLIST_SONG
			+ " (owner text,sid text,displayName text,trackName text,artisName text,album text,hashValue text,m4aUrl text,filePath text,parentPath text,mine_type text,size integer,duration integer,type integer,note text);";
	private String sql_disc_songs = "create table if not exists "
			+ TABLE_DISC_SONG
			+ " (owner text,sid text,displayName text,trackName text,artisName text,album text,hashValue text,m4aUrl text,filePath text,parentPath text,mine_type text,size integer,duration integer,type integer,note text);";
	private String sql_online_songs = "create table if not exists "
			+ TABLE_ONLINE_SONG
			+ " (owner text,sid text,displayName text,trackName text,artisName text,album text,hashValue text,m4aUrl text,filePath text,parentPath text,mine_type text,size integer,duration integer,type integer,note text);";
	private String sql_favorite_songs = "create table if not exists "
			+ TABLE_FAVORITE_SONG
			+ " (owner text,sid text,displayName text,trackName text,artisName text,album text,hashValue text,m4aUrl text,filePath text,parentPath text,mine_type text,size integer,duration integer,type integer,note text);";

	public MusicDbHelper(Context context) {
		super(context, "music.db", null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(sql_mylist_songs);
		db.execSQL(sql_disc_songs);
		db.execSQL(sql_online_songs);
		db.execSQL(sql_favorite_songs);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("drop table if exists " + TABLE_MYLIST_SONG);
		db.execSQL("drop table if exists " + TABLE_DISC_SONG);
		db.execSQL("drop table if exists " + TABLE_ONLINE_SONG);
		db.execSQL("drop table if exists " + TABLE_FAVORITE_SONG);
		onCreate(db);
	}

	public int insert(Song song, String owner, int type) {
		int result = 0;
		SQLiteDatabase db = getWritableDatabase();

		String table = GetOperateTable(type);

		String sql = "select * from " + table + " where owner='" + owner
				+ "' and filePath='" + song.getFilePath() + "'";

		if (!"-1".equals(song.getSid())) {
			sql = "select * from " + table + " where owner='" + owner
					+ "' and sid='" + song.getSid() + "'";
		}
		Cursor cursor = db.rawQuery(sql, null);
		if (cursor.getCount() != 0) {
			result = 1;
			cursor.close();
			db.close();
			return result;
		}

		ContentValues value = new ContentValues();
		value.put("owner", owner);
		value.put("trackName", song.getTrackName());
		value.put("sid", song.getSid());
		value.put("displayName", song.getDisplayName());
		value.put("filePath", song.getFilePath());
		value.put("album", song.getAlbum());
		value.put("artisName", song.getArtisName());
		value.put("m4aUrl", song.getM4aUrl());
		value.put("mine_type", song.getMine_type());
		value.put("size", song.getSize());
		value.put("duration", song.getDuration());
		value.put("type", type);

		db.insert(table, null, value);
		db.close();
		return result;
	}

	public void delete(Song song, String owner, int type) {
		SQLiteDatabase db = getWritableDatabase();

		String table = GetOperateTable(type);

		String where = "owner=? and filePath=?";
		String[] whereArgs = new String[] { String.valueOf(owner),
				String.valueOf(song.getFilePath()) };
		if (!"-1".equals(song.getSid())) {
			where = "owner=? and sid=?";
			whereArgs = new String[] { String.valueOf(owner),
					String.valueOf(song.getSid()) };
		}
		db.delete(table, where, whereArgs);
		db.close();
	}

	public void updata(Song song, String owner, int type) {
		SQLiteDatabase db = getWritableDatabase();

		String table = GetOperateTable(type);

		ContentValues value = new ContentValues();
		value.put("type", type);
		value.put("filePath", song.getFilePath());

		String where = "owner=? and filePath=?";
		String[] whereArgs = new String[] { String.valueOf(owner),
				String.valueOf(song.getFilePath()) };
		if (!"-1".equals(song.getSid())) {
			where = "owner=? and sid=?";
			whereArgs = new String[] { String.valueOf(owner),
					String.valueOf(song.getSid()) };
		}

		db.update(table, value, where, whereArgs);
		db.close();
	}

	public ArrayList<Song> query(String owner, int type) {
		List<Song> list = new ArrayList<Song>();
		SQLiteDatabase db = getWritableDatabase();

		String table = GetOperateTable(type);
		String sql = "select * from " + table + " where owner='" + owner + "'";
		Cursor cursor = db.rawQuery(sql, null);

		while (cursor.moveToNext()) {
			Song song = new Song();
			String displayName = cursor.getString(cursor
					.getColumnIndex("displayName"));
			String filePath = cursor.getString(cursor
					.getColumnIndex("filePath"));
			String album = cursor.getString(cursor.getColumnIndex("album"));
			String artisName = cursor.getString(cursor
					.getColumnIndex("artisName"));
			String trackName = cursor.getString(cursor
					.getColumnIndex("trackName"));
			int size = cursor.getInt(cursor.getColumnIndex("size"));
			int duration = cursor.getInt(cursor.getColumnIndex("duration"));
			String sid = cursor.getString(cursor.getColumnIndex("sid"));
			String m4aUrl = cursor.getString(cursor.getColumnIndex("m4aUrl"));
			String mine_type = cursor.getString(cursor
					.getColumnIndex("mine_type"));

			song.setFilePath(filePath);
			song.setDisplayName(displayName);
			song.setTrackName(trackName);
			song.setAlbum(album);
			song.setArtisName(artisName);
			song.setSize(size);
			song.setMine_type(mine_type);
			song.setSid(sid);
			song.setM4aUrl(m4aUrl);
			song.setDuration(duration);
			list.add(song);
		}
		cursor.close();
		db.close();

		return (ArrayList<Song>) list;
	}

	public String getFilePathBySid(String owner, String sid) {
		SQLiteDatabase db = getWritableDatabase();
		String filePath = null;
		String sql = "select * from " + TABLE_MYLIST_SONG + " where owner='"
				+ owner + "' and sid='" + sid + "'";
		Cursor cursor = db.rawQuery(sql, null);
		if (cursor.getCount() == 0) {
			cursor.close();
			db.close();
			return filePath;
		}
		cursor.moveToNext();
		filePath = cursor.getString(cursor.getColumnIndex("filePath"));

		cursor.close();
		db.close();
		return filePath;
	}

	public Song query(String owner, int type, int index) {
		SQLiteDatabase db = getWritableDatabase();

		String table = GetOperateTable(type);
		String sql = "select * from " + table + " where owner='" + owner + "'";
		Cursor cursor = db.rawQuery(sql, null);

		cursor.moveToPosition(index);
		Song song = new Song();
		String displayName = cursor.getString(cursor
				.getColumnIndex("displayName"));
		String filePath = cursor.getString(cursor.getColumnIndex("filePath"));
		String album = cursor.getString(cursor.getColumnIndex("album"));
		String artisName = cursor.getString(cursor.getColumnIndex("artisName"));
		String trackName = cursor.getString(cursor.getColumnIndex("trackName"));
		int size = cursor.getInt(cursor.getColumnIndex("size"));
		int duration = cursor.getInt(cursor.getColumnIndex("duration"));
		String sid = cursor.getString(cursor.getColumnIndex("sid"));
		String m4aUrl = cursor.getString(cursor.getColumnIndex("m4aUrl"));
		String mine_type = cursor.getString(cursor.getColumnIndex("mine_type"));

		song.setFilePath(filePath);
		song.setDisplayName(displayName);
		song.setTrackName(trackName);
		song.setAlbum(album);
		song.setArtisName(artisName);
		song.setSize(size);
		song.setMine_type(mine_type);
		song.setSid(sid);
		song.setM4aUrl(m4aUrl);
		song.setDuration(duration);

		cursor.close();
		db.close();

		return song;
	}

}
