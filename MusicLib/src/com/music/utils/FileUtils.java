package com.music.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class FileUtils {
	public static String getPath(Context context, Uri uri) {

		if ("content".equalsIgnoreCase(uri.getScheme())) {
			String[] projection = { "_data" };
			Cursor cursor = null;

			try {
				cursor = context.getContentResolver().query(uri, projection,
						null, null, null);
				int column_index = cursor.getColumnIndexOrThrow("_data");
				if (cursor.moveToFirst()) {
					return cursor.getString(column_index);
				}
			} catch (Exception e) {
				// Eat it
			}
		}

		else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}

	/**
	 * 获取文件名后缀
	 * 
	 * @param filename
	 * @return
	 */
	public static String getExtensionName(String filename) {
		if ((filename != null) && (filename.length() > 0)) {
			int dot = filename.lastIndexOf('.');
			if ((dot > -1) && (dot < (filename.length() - 1))) {
				return "." + filename.substring(dot + 1);
			}
		}
		return "";
	}

	/**
	 * 获取不带扩展名的文件名
	 * 
	 * @param filename
	 * @return
	 */
	public static String getFileNameNoEx(String filename) {
		if ((filename != null) && (filename.length() > 0)) {
			int dot = filename.lastIndexOf('.');
			if ((dot > -1) && (dot < (filename.length()))) {
				return filename.substring(0, dot);
			}
		}
		return filename;
	}

	/**
	 * 获取文件名
	 * 
	 * @param
	 * @return
	 */
	public static String getFilename(String filepath) {
		if ((filepath != null) && (filepath.length() > 0)) {
			int dot = filepath.lastIndexOf('/');
			if ((dot > -1) && (dot < (filepath.length() - 1))) {
				return filepath.substring(dot + 1);
			}
		}
		return "";
	}

	/**
	 * 获取文件路径
	 * 
	 * @param
	 * @return
	 */
	public static String getSavePath(String filepath) {
		if ((filepath != null) && (filepath.length() > 0)) {
			int dot = filepath.lastIndexOf('/');
			if ((dot > -1) && (dot < (filepath.length()))) {
				return filepath.substring(0, dot);
			}
		}
		return filepath;
	}

	public static String caculateFileSize(int fileSize) {
		String resource_size = "";
		DecimalFormat df = new DecimalFormat("#.##");
		if ((double) ((double) fileSize / 1024) > 1000) {
			resource_size = df
					.format((double) ((double) fileSize / 1024 / 1024)) + "MB";
		} else {
			resource_size = df.format((double) ((double) fileSize / 1024))
					+ "KB";
		}
		return resource_size;
	}

	public static String getTimeStr(int duration) {
		String str = "";
		int total = duration / 1000;
		int sec = total % 60;
		int min = total / 60;
		if (sec < 10) {
			str = min + ":0" + sec;
		} else {
			str = min + ":" + sec;
		}
		return str;
	}

	/**
	 * 判断文件是否存在
	 * 
	 * @param filePath
	 * @return
	 */
	public static boolean FileExists(String filePath) {

		if (filePath == null || "".equals(filePath)) {
			return false;
		}

		File file = new File(filePath);
		if (file.exists()) {
			return true;
		}

		return false;
	}
	
	@SuppressWarnings("resource")
	public static String getFileSize(String filePath) {
		if (!FileExists(filePath))
			return null;
		File file = new File(filePath);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			int size = fis.available();
			return caculateFileSize(size);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}


}