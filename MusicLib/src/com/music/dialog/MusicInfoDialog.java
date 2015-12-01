package com.music.dialog;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.music.R;
import com.music.entity.Song;
import com.music.utils.FileUtils;

public class MusicInfoDialog extends Dialog {

	private Context context;
	private Button btnPositive;
	private LinearLayout llMusicInfo;
	private TextView tvSongName;
	private Song song;
	private List<String> infoList = new ArrayList<String>();

	public MusicInfoDialog(Context context) {
		super(context);
	}

	public MusicInfoDialog(Context context, Song song) {
		super(context, R.style.MusicDialog);
		this.context = context;
		this.song = song;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.app_music_info_dialog);

		initView();
	}

	private void initView() {
		btnPositive = (Button) findViewById(R.id.btn_positive);
		llMusicInfo = (LinearLayout) findViewById(R.id.ll_info);
		tvSongName = (TextView) findViewById(R.id.tv_song_name);

		tvSongName.setText(song.getDisplayName());

		initList();
		for (int i = 0; i < infoList.size(); i++) {
			TextView tvItem = new TextView(context);
			tvItem.setText(infoList.get(i));
			llMusicInfo.addView(tvItem);
		}

		btnPositive.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

	}

	private void initList() {
		String artisName = song.getArtisName();
		String album = song.getAlbum();
		String mineType = song.getMine_type();
		int duration = song.getDuration();
		String filePath = song.getFilePath();

		if (artisName == null) {
			artisName = "未知歌手";
		}

		if (album == null) {
			album = "未知专辑";
		}

		if (mineType == null) {
			mineType = "audio/mpeg";
		}

		infoList.add("歌手: " + artisName);
		infoList.add("专辑: " + album);
		infoList.add("格式: " + mineType);

		if (duration != 0) {
			String time = FileUtils.getTimeStr(duration);
			infoList.add("播放时长: " + time);
		}

		if (FileUtils.FileExists(filePath)) {
			String size = FileUtils.getFileSize(filePath);
			infoList.add("文件大小: " + size);
		}

		if (filePath != null) {
			infoList.add("保存路径:\n" + filePath);
		}
	}

}
