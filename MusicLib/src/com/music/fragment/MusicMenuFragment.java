package com.music.fragment;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.music.MusicClient;
import com.music.MusicCommon;
import com.music.R;
import com.music.adapter.MenuAdapter;
import com.music.cb.SwitchFragmentCB;
import com.music.database.PlayListDbHelper;
import com.music.entity.Song;
import com.music.services.AudioPlaybackService;
import com.music.utils.PlayControl;

public class MusicMenuFragment extends Fragment {

	private Context context;
	private GridView gvMenu;
	private MenuAdapter menuAdapter;

	public MusicMenuFragment(Context context) {
		this.context = context;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_music_menu,
				container, false);

		gvMenu = (GridView) rootView.findViewById(R.id.gv_menu);
		menuAdapter = new MenuAdapter(context);
		gvMenu.setAdapter(menuAdapter);

		gvMenu.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				int titleID = Integer.parseInt(String.valueOf(menuAdapter
						.getItem(position)));
				String tag = context.getString(titleID);
				Fragment musicFragment = null;
				if (titleID == R.string.title_mylist_music) {
					musicFragment = new MusicMyListFragment(context, titleID,
							true);
				} else if (titleID == R.string.title_disc_music) {
					musicFragment = new MusicDiscFragment(context, titleID,
							true);
				} else if (titleID == R.string.title_favorite_music) {
					musicFragment = new MusicFavoriteFragment(context, titleID,
							true);
				} else if (titleID == R.string.title_online_music) {
					musicFragment = new MusicOnlineFragment(context, titleID,
							true);
				}

				SwitchFragmentCB switchFragmentCB = (SwitchFragmentCB) context;
				if (switchFragmentCB != null)
					switchFragmentCB.SwitchToFragment(musicFragment, tag);
			}
		});

		return rootView;
	}
	
	private Song currentSong;

	public void startPlaySong(Song song) {
		currentSong = song;
		handler.sendEmptyMessage(1);
	}

	Handler handler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				if (currentSong != null)
					playSong(currentSong);
				break;
			case 1:
				MusicCommon musicCommon = MusicCommon
						.getInstance(((Activity) context).getApplication());
				AudioPlaybackService service = musicCommon.getService();// 判断音乐服务是否存在
				if (service != null) {
					handler.sendEmptyMessage(0);
				} else {
					handler.sendEmptyMessageDelayed(1, 500);
				}
			default:
				break;
			}
			return false;
		}
	});
	
	private void playSong(Song song) {
		PlayListDbHelper playDb = new PlayListDbHelper(context);
		playDb.clearTable();
		String owner = MusicClient.getInstance().getUser();
		playDb.insert(song, owner);
		Cursor cursor = playDb.getCursor(owner);
		PlayControl playControl = new PlayControl(
				(Application) context.getApplicationContext());
		playControl.setCursor(cursor);

		playControl.skipToTrack(0);

		int titleID = R.string.title_online_music;
		String tag = context.getString(titleID);
		Fragment musicFragment = new MusicOnlineFragment(context, titleID, true);
		SwitchFragmentCB switchFragmentCB = (SwitchFragmentCB) context;
		if (switchFragmentCB != null)
			switchFragmentCB.SwitchToFragment(musicFragment, tag);
	}

}
