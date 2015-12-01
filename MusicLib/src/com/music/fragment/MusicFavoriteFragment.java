package com.music.fragment;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.music.MusicClient;
import com.music.R;
import com.music.database.MusicDbHelper;
import com.music.entity.Song;

/**
 * 
 * 收藏夹
 * 
 * @author shen
 * 
 */
public class MusicFavoriteFragment extends AudioPlayFragment {

	private static final int SET_MUSIC_LIST = 1;

	private List<Song> musicList;

	private String owner;
	private Context context;

	Handler handler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {

			switch (msg.what) {
			case SET_MUSIC_LIST:
				setSongList(musicList, "还没有收藏任何歌曲~");
				break;
			}

			return false;
		}

	});

	/**
	 * @param context
	 * @param title
	 *            标题
	 * @param hasTitle
	 *            是否显示标题栏
	 */
	public MusicFavoriteFragment(final Context context, final int titleId,
			boolean hasTitle) {
		super(context, titleId, hasTitle);
		this.context = context;
		owner = MusicClient.getInstance().getUser();

	}

	private void GetFavoriteMusic() {
		musicList = new ArrayList<Song>();
		MusicDbHelper musicDb = new MusicDbHelper(context);
		musicList = musicDb.query(owner, MusicDbHelper.TYPE_FAVORITE);

		InitPlaylist(musicList, owner);
		handler.sendEmptyMessage(SET_MUSIC_LIST);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.audio_list, container, false);
		this.inflater = inflater;

		initBasicView();
		initPlayingBar();

		new Thread(new Runnable() {
			public void run() {
				GetFavoriteMusic();
			}
		}).start();
		return rootView;
	}

}
