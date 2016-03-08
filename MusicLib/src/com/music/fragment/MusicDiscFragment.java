package com.music.fragment;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.music.MusicClient;
import com.music.R;
import com.music.database.MusicDbHelper;
import com.music.entity.Song;
import com.music.utils.AsyncPlayFolderRecursiveTask;

/**
 * 
 * 本地音乐
 * 
 * @author shen
 * 
 */
public class MusicDiscFragment extends AudioPlayFragment {

	private static final int SET_MUSIC_LIST = 1;

	private List<Song> musicList;

	private String owner;
	private Context context;

	Handler handler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {

			switch (msg.what) {
			case SET_MUSIC_LIST:
				setSongList(musicList);
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
	public MusicDiscFragment(final Context context, final int titleId,
			boolean hasTitle) {
		super(context, titleId, hasTitle);
		this.context = context;
		owner = MusicClient.getInstance().getUser();
	}

	private void GetDiscMusic() {
		musicList = new ArrayList<Song>();
		MusicDbHelper musicDb = new MusicDbHelper(context);
		musicList = musicDb.query(owner, MusicDbHelper.TYPE_DISC);

		InitPlaylist(musicList, owner);
		handler.sendEmptyMessage(SET_MUSIC_LIST);
	}

	private Button btnScan;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.audio_list, container, false);
		this.inflater = inflater;

		initBasicView();
		initPlayingBar();

		btnScan = (Button) rootView.findViewById(R.id.common_title_btn_scan);
		btnScan.setVisibility(View.VISIBLE);

		btnScan.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				AsyncPlayFolderRecursiveTask task = new AsyncPlayFolderRecursiveTask(
						(Activity) context);
				String folderName = Environment.getExternalStorageDirectory()
						.toString();
				task.execute(folderName);
			}
		});

		new Thread(new Runnable() {
			public void run() {
				GetDiscMusic();
			}
		}).start();
		return rootView;
	}

}
