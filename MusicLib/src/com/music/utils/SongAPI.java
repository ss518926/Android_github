package com.music.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.music.entity.NetSong;
import com.music.entity.Song;

public class SongAPI {
	private static String Apikey = "2a1e0d9fdca0a3ba7f49fd26e8298640";
	static String httpUrl = "http://apis.baidu.com/geekery/music/query";
	static String httpArg = "s=keywords&limit=15&p=pageNumber";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<Song> result = Search("李白", 1);
		System.out.print(result.get(0).getDisplayName() + "-"
				+ result.get(0).getArtisName());
	}

	public static List<Song> Search(String key, int pageNumber) {
		httpArg = "s=" + key + "&limit=15&p=" + pageNumber;
		String jsonResult = request(httpUrl, httpArg);
		if (jsonResult == null) {
			return null;
		}

		List<Song> list = new ArrayList<Song>();
		try {
			JSONObject jsonObject;
			jsonObject = new JSONObject(jsonResult);
			String status = (String) jsonObject.get("status");
			if (status.equals("failed"))
				return null;
			jsonObject = (JSONObject) jsonObject.get("data");
			jsonObject = (JSONObject) jsonObject.get("data");
			JSONArray jsonArray = (JSONArray) jsonObject.get("list");

			Gson gson = new Gson();
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject temp = (JSONObject) jsonArray.get(i);
				NetSong netSong = gson.fromJson(temp.toString(), NetSong.class);
				Song song = new Song(netSong);
				list.add(song);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}

		return list;
	}

	public static String request(String httpUrl, String httpArg) {
		BufferedReader reader = null;
		String result = null;
		StringBuffer sbf = new StringBuffer();
		httpUrl = httpUrl + "?" + httpArg;

		try {
			httpUrl = zh_encode(httpUrl, "utf-8");
			URL url = new URL(httpUrl);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setRequestMethod("GET");
			// 填入apikey到HTTP header
			connection.setRequestProperty("apikey", Apikey);
			connection.connect();
			InputStream is = connection.getInputStream();
			reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
			String strRead = null;
			while ((strRead = reader.readLine()) != null) {
				sbf.append(strRead);
				sbf.append("\r\n");
			}
			reader.close();
			result = sbf.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private static String zhPattern = "[\u0391-\uFFE5]+";

	/**
	 * 只替换字符转中的中文部分
	 * 
	 * @param str
	 *            被替换的字符串
	 * @param charset
	 *            字符集
	 * @return 替换好的
	 * @throws UnsupportedEncodingException
	 *             不支持的字符集
	 */
	public static String zh_encode(String str, String charset)
			throws UnsupportedEncodingException {
		Pattern p = Pattern.compile(zhPattern);
		Matcher m = p.matcher(str);
		StringBuffer b = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(b, URLEncoder.encode(m.group(0), charset));
		}
		m.appendTail(b);
		return b.toString();
	}

}
