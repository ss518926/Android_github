package com.example.musicguide;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.Window;

import com.music.MusicClient;
import com.music.MusicCommon;
import com.music.cb.SwitchFragmentCB;
import com.music.entity.Song;
import com.music.fragment.MusicMenuFragment;
import com.music.services.AudioPlaybackService;

public class MusicActivity extends FragmentActivity implements SwitchFragmentCB {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		MusicMenuFragment menuFragment = new MusicMenuFragment(this);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.music_container, menuFragment).commit();
			
			
		}
		
		Snackbar s;

		MusicClient.getInstance().setUser("admin");//设定使用者用于数据存储和使用
		
		MusicCommon musicCommon = MusicCommon.getInstance(getApplication());
		AudioPlaybackService service = musicCommon.getService();//判断音乐服务是否存在，不存在则启动
		if (service == null) {
			Intent intent = new Intent(this, AudioPlaybackService.class);
			startService(intent);
		}
		
		Bundle bundle = this.getIntent().getExtras();
		if (bundle != null && bundle.containsKey("m4aUrl")) {
			String displayName = bundle.getString("displayName");
			String artisName = bundle.getString("artisName");
			String m4aUrl = bundle.getString("m4aUrl");
			String sid = bundle.getString("sid");

			Song song = new Song();
			song.setDisplayName(displayName);
			song.setArtisName(artisName);
			song.setM4aUrl(m4aUrl);
			song.setSid(sid);
			menuFragment.startPlaySong(song);
		}
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void SwitchToFragment(Fragment toFragment, String tag) {
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.music_container, toFragment, tag)
				.addToBackStack(null).commit();
	}

}
