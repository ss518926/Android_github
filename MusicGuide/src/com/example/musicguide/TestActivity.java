package com.example.musicguide;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class TestActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test_activity_main);
	}

	public void click1(View v) {
		Intent intent = new Intent();
		intent.setClass(TestActivity.this, MusicActivity.class);
		startActivity(intent);
	}

	public void click2(View v) {
		Intent intent = new Intent();
		intent.setClass(TestActivity.this, MusicActivity.class);
		Bundle bundle = new Bundle();
		String displayName = "测试";
		String artisName = "未知";
		String m4aUrl = "http://m2.resources.yunloo.net/qzKezJMKs3j4fOAnABejRA==/3408486046866926.mp3";
		// String sid = "-1";

		bundle.putString("displayName", displayName);
		bundle.putString("artisName", artisName);
		bundle.putString("m4aUrl", m4aUrl);
		// bundle.putString("sid", sid);
		intent.putExtras(bundle);

		startActivity(intent);
	}
}
