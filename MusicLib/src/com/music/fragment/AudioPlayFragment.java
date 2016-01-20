package com.music.fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.music.MusicClient;
import com.music.MusicCommon;
import com.music.R;
import com.music.adapter.AudioAdapter;
import com.music.database.PlayListDbHelper;
import com.music.dialog.MusicInfoDialog;
import com.music.entity.Song;
import com.music.utils.AutoLoadListener;
import com.music.utils.AutoLoadListener.AutoLoadCallBack;
import com.music.utils.PlayControl;
import com.music.utils.SongAPI;

public class AudioPlayFragment extends Fragment {

	private static final int TYPE_SEQUENCE = 0;// 顺序播放
	private static final int TYPE_REPEAT_ALL = 1;// 列表循环
	private static final int TYPE_SINGLE = 2;// 单曲播放
	private static final int TYPE_RANDOM = 3;// 随机播放

	private int modeType = 0;// 播放模式

	private String owner;
	public View rootView;
	private Context context;
	private ListView listView;
	private AudioAdapter audioAdapter;
	private List<Song> songList;
	private List<Song> tempSongList = new ArrayList<Song>();
	private LinearLayout llPlayMode, llEditMode, llListBar;
	private ImageView ivPlayMode;
	private TextView tvPlayMode;
	private RelativeLayout rlTitleBar;
	private TextView tvTitle;
	private ImageButton btnBack;
	private boolean hasTitle;
	private ProgressBar progressBar;
	private TextView tvEmptyDesc;

	private String title;
	private int titleId;

	private View llWaiting;

	/**
	 * @param context
	 *            上下文
	 * @param titleId
	 *            标题Id
	 * @param hasTitle
	 *            是否显示标题栏
	 */
	public AudioPlayFragment(Context context, int titleId, boolean hasTitle) {
		this.context = context;
		this.titleId = titleId;
		this.title = context.getString(titleId);
		this.hasTitle = hasTitle;
		owner = MusicClient.getInstance().getUser();
	}

	public LayoutInflater inflater;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LocalBroadcastManager.getInstance(context).registerReceiver(
				(mReceiver), new IntentFilter(MusicCommon.UPDATE_UI_BROADCAST));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopTimer();
		LocalBroadcastManager.getInstance(context)
				.unregisterReceiver(mReceiver);
	}

	public void initBasicView() {

		rlTitleBar = (RelativeLayout) rootView
				.findViewById(R.id.common_title_bar);
		llListBar = (LinearLayout) rootView
				.findViewById(R.id.list_common_bar_header);
		llListBar.setVisibility(View.GONE);

		tvTitle = (TextView) rootView.findViewById(R.id.common_title_tv_title);
		btnBack = (ImageButton) rootView
				.findViewById(R.id.common_title_bar_btn_back);
		progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
		tvEmptyDesc = (TextView) rootView.findViewById(R.id.tv_empty_desc);
		tvEmptyDesc.setVisibility(View.GONE);

		if (hasTitle) {
			tvTitle.setText(title);
			btnBack.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					((FragmentActivity) context).getSupportFragmentManager()
							.popBackStack();
				}
			});
		} else {
			rlTitleBar.setVisibility(View.GONE);
		}

	}

	private void initListView() {

		playControl = new PlayControl(
				(Application) context.getApplicationContext());

		llListBar.setVisibility(View.VISIBLE);

		listView = (ListView) rootView.findViewById(R.id.audio_list);
		audioAdapter = new AudioAdapter(listView, context, titleId);
		audioAdapter.initDatas(songList);
		listView.setAdapter(audioAdapter);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// 歌曲点击事件
				stopTimer();
				playControl.skipToTrack(position);
			}
		});

		llPlayMode = (LinearLayout) rootView
				.findViewById(R.id.list_common_bar_header_randomplay);
		ivPlayMode = (ImageView) rootView
				.findViewById(R.id.ic_list_common_bar_header_randomplay);
		tvPlayMode = (TextView) rootView
				.findViewById(R.id.tv_list_common_bar_header_randomplay);

		llEditMode = (LinearLayout) rootView
				.findViewById(R.id.list_common_bar_header_editmode);

		modeType = playControl.getRepeatMode();
		setPlayModeUI();

		llPlayMode.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// 播放模式切换
				modeType++;
				modeType = modeType % 4;
				setPlayModeUI();
			}
		});

		llEditMode.setVisibility(View.GONE);

		llEditMode.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

			}
		});

		if (titleId == R.string.title_online_music) {
			llListBar.setVisibility(View.GONE);
			llWaiting = inflater.inflate(R.layout.wating_view, listView, false);
			AutoLoadListener autoLoadListener = new AutoLoadListener(callBack);
			listView.setOnScrollListener(autoLoadListener);
		}

	}

	private void setPlayModeUI() {
		int textId = 0, resId = 0;
		switch (modeType) {
		case TYPE_RANDOM:
			textId = R.string.play_mode_random;
			resId = R.drawable.playback_playmode_random_button;
			break;
		case TYPE_SINGLE:
			textId = R.string.play_mode_single;
			resId = R.drawable.playback_playmode_repeat_single_button;
			break;
		case TYPE_SEQUENCE:
			textId = R.string.play_mode_sequence;
			resId = R.drawable.playback_playmode_sequence_button;
			break;
		case TYPE_REPEAT_ALL:
			textId = R.string.play_mode_repeat_all;
			resId = R.drawable.playback_playmode_repeat_all_button;
		}
		playControl.setRepeatMode(modeType);
		ivPlayMode.setImageResource(resId);
		tvPlayMode.setText(textId);
	}

	public void setSongList(List<Song> songList) {
		setSongList(songList, "没有找到任何歌曲~");
	}

	public void setSongList(List<Song> songList, String reason) {
		this.songList = songList;
		progressBar.setVisibility(View.GONE);
		if (songList != null && songList.size() != 0) {
			initListView();
		} else {
			if (listView != null) {
				audioAdapter = new AudioAdapter(listView, context, titleId);
				listView.setAdapter(audioAdapter);
			}
			setEmptyReason(reason);
		}
	}

	private void setEmptyReason(String reason) {
		tvEmptyDesc.setVisibility(View.VISIBLE);
		tvEmptyDesc.setText(reason);
	}

	AutoLoadCallBack callBack = new AutoLoadCallBack() {

		public void execute() {
			refreshData();
		}
	};

	private String searchKey = "";
	private int songPage = 1;
	private boolean isRefresh = false;

	public void setSearchKey(String searchKey) {
		this.searchKey = searchKey;
	}

	private void refreshData() {
		if (songPage < 0 || isRefresh) {
			return;
		}
		if (songList != null && songList.size() < 15) {
			return;
		}
		isRefresh = true;
		listView.addFooterView(llWaiting);
		listView.setAdapter(audioAdapter);
		listView.setSelection(audioAdapter.getCount() - 1);

		new Thread(new Runnable() {

			@Override
			public void run() {
				songPage++;
				tempSongList.clear();
				tempSongList.addAll(songList);
				List<Song> list = SongAPI.Search(searchKey, songPage);
				if (list == null || list.size() == 0) {
					songPage = -1;
				} else {
					tempSongList.addAll(list);
					InitPlaylist(tempSongList, owner);
				}
				isRefresh = false;
				handler.sendEmptyMessage(TYPE_NEXT_PAGE);
			}
		}).start();
	}

	private static final int TYPE_NEXT_PAGE = 9;

	Handler handler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case TYPE_NEXT_PAGE:
				listView.removeFooterView(llWaiting);
				if (songList != null) {
					songList.clear();
					songList.addAll(tempSongList);
					tempSongList.clear();
				}
				audioAdapter.notifyDataSetChanged();
				break;
			case UPDATE_SEEKBAR:
				break;
			}
			return false;
		}
	});

	private static final int UPDATE_SEEKBAR = 10;

	ImageButton btnPlayToggle, btnPlayNext, btnMusicInfo;
	TextView tvSongName, tvSingerName;
	LinearLayout llBuffering;
	SeekBar PlaySeeker;
	MediaPlayer mediaPlayer;

	private Timer mTimer;
	private TimerTask mTimerTask;

	private boolean isChanging;
	private boolean isLoadingSong = false;

	PlayControl playControl;

	public void initPlayingBar() {
		llBuffering = (LinearLayout) rootView
				.findViewById(R.id.playing_bar_buffering_icon);
		llBuffering.setVisibility(View.INVISIBLE);

		btnPlayToggle = (ImageButton) rootView
				.findViewById(R.id.playing_bar_toggle);
		btnPlayNext = (ImageButton) rootView
				.findViewById(R.id.playing_bar_next);
		btnMusicInfo = (ImageButton) rootView
				.findViewById(R.id.playing_bar_current_list);

		tvSongName = (TextView) rootView
				.findViewById(R.id.playing_bar_song_name);
		tvSingerName = (TextView) rootView
				.findViewById(R.id.playing_bar_singer_name);
		PlaySeeker = (SeekBar) rootView.findViewById(R.id.playing_bar_seeker);

		playControl = new PlayControl(
				(Application) context.getApplicationContext());

		btnPlayToggle.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Song currentSong = playControl.getCurrentSong();
				if (currentSong == null
						&& (songList == null || songList.size() == 0))
					return;

				if (!playControl.isMediaPlayerPrepared()) {
					playControl.prepareMediaPlayer(0);
				}

				boolean isplay = playControl.togglePlaybackState();
				if (isplay) {
					settimer();
					btnPlayToggle
							.setImageResource(R.drawable.kg_ic_playing_bar_pause);
				} else {
					stopTimer();
					btnPlayToggle
							.setImageResource(R.drawable.kg_ic_playing_bar_play);
				}
			}
		});

		btnPlayNext.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Song currentSong = playControl.getCurrentSong();
				if (currentSong == null
						&& (songList == null || songList.size() == 0))
					return;

				stopTimer();
				playControl.skipToNextTrack();
			}
		});

		btnMusicInfo.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Song currentSong = playControl.getCurrentSong();
				if (currentSong == null)
					return;

				try {
					int duration = playControl.getMediaPlayer().getDuration();
					currentSong.setDuration(duration);
				} catch (Exception e) {
					e.printStackTrace();
				}

				MusicInfoDialog dialog = new MusicInfoDialog(context,
						currentSong);
				dialog.show();
			}
		});

		UpdataPlayingBarUI();

	}

	/**
	 * 播放栏UI更新
	 */
	private void UpdataPlayingBarUI() {

		if (isLoadingSong) {
			llBuffering.setVisibility(View.VISIBLE);
		} else {
			llBuffering.setVisibility(View.INVISIBLE);
		}

		mediaPlayer = playControl.getMediaPlayer();
		Song currentSong = playControl.getCurrentSong();

		if (mediaPlayer == null || !playControl.isMediaPlayerPrepared()
				|| currentSong == null) {
			PlaySeeker.setEnabled(false);
			stopTimer();
			btnPlayToggle.setImageResource(R.drawable.kg_ic_playing_bar_play);
			PlaySeeker.setProgress(0);
		} else {
			PlaySeeker.setEnabled(true);
			skblisten();
			PlaySeeker.setMax(mediaPlayer.getDuration());
			if (mediaPlayer.isPlaying()) {
				settimer();
				btnPlayToggle
						.setImageResource(R.drawable.kg_ic_playing_bar_pause);
			} else {
				stopTimer();
				btnPlayToggle
						.setImageResource(R.drawable.kg_ic_playing_bar_play);
			}
		}

		String displayName = "";
		String artisName = "";
		if (currentSong != null) {
			displayName = currentSong.getDisplayName();
			artisName = currentSong.getArtisName();
		}

		if ("".equals(displayName)) {
			displayName = "酷狗音乐";
		}

		if (artisName == null || "".equals(artisName)) {
			artisName = "";
		}

		tvSongName.setText(displayName);
		tvSingerName.setText(artisName);
	}

	BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			if (intent.hasExtra(MusicCommon.UPDATE_BUFFERING_PROGRESS)) {
				int Buffer = Integer.parseInt(intent
						.getStringExtra("UpdateBufferingProgress"));
				int currentProgress = PlaySeeker.getProgress();
				if (Buffer < currentProgress) {
					llBuffering.setVisibility(View.VISIBLE);
				} else {
					llBuffering.setVisibility(View.INVISIBLE);
				}
			}

			if (intent.hasExtra(MusicCommon.UPDATE_PAGER_POSTIION)) {
				int currentSongIndex = Integer.parseInt(intent
						.getStringExtra(MusicCommon.UPDATE_PAGER_POSTIION));
				if (currentSongIndex != -1)
					isLoadingSong = true;
				UpdataPlayingBarUI();
			}

			if (intent.hasExtra(MusicCommon.UPDATE_SEEKBAR_DURATION)) {
				isLoadingSong = false;
				UpdataPlayingBarUI();
			}

		}
	};

	/**
	 * 拖动精度条的监听
	 */
	public void skblisten() {
		OnSeekBarChangeListener sChangeListener = new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// 当拖动停止后，控制mediaPlayer播放指定位置的音乐
				if (!mediaPlayer.isPlaying()) {
					mediaPlayer.start();
					settimer();
				}
				btnPlayToggle
						.setImageResource(R.drawable.kg_ic_playing_bar_pause);
				mediaPlayer.seekTo(seekBar.getProgress());
				isChanging = false;
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				isChanging = true;
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
			}
		};
		PlaySeeker.setOnSeekBarChangeListener(sChangeListener);
	}

	public void settimer() {
		stopTimer();
		if (mTimer == null) {
			mTimer = new Timer();
		}
		if (mTimerTask == null) {
			mTimerTask = new TimerTask() {
				@Override
				public void run() {
					if (isChanging == true)// 当用户正在拖动进度进度条时不处理进度条的的进度
						return;
					PlaySeeker.setProgress(mediaPlayer.getCurrentPosition());
				}
			};
		}
		// 每隔100毫秒检测一下播放进度
		if (mTimer != null && mTimerTask != null)
			mTimer.schedule(mTimerTask, 0, 10);
	}

	private void stopTimer() {
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
		if (mTimerTask != null) {
			mTimerTask.cancel();
			mTimerTask = null;
		}
	}

	public void InitPlaylist(List<Song> songList, String owner) {
		if (songList == null || songList.size() == 0)
			return;
		PlayListDbHelper playDb = new PlayListDbHelper(context);
		playDb.clearTable();
		playDb.insert(songList, owner);
		Cursor cursor = playDb.getCursor(owner);
		PlayControl playControl = new PlayControl(
				(Application) context.getApplicationContext());
		playControl.setCursor(cursor);
	}

}
