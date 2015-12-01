package com.music.dialog;

import java.io.File;
import java.util.List;

import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.music.MusicClient;
import com.music.R;
import com.music.adapter.AudioAdapter;
import com.music.database.MusicDbHelper;
import com.music.database.PlayListDbHelper;
import com.music.entity.Song;
import com.music.utils.FileUtils;
import com.music.utils.PlayControl;

public class DeleteDialog extends Dialog {

	private Context context;
	private Button btnCancel, btnDelete;
	private CheckBox checkBox;
	private TextView tvDeleteInfo;
	private Song song;
	private int tableType;
	private int position;
	private List<Song> songList;
	private AudioAdapter audioAdapter;

	public void setAudioAdapter(AudioAdapter audioAdapter) {
		this.audioAdapter = audioAdapter;
	}

	public void setSongList(List<Song> songList) {
		this.songList = songList;
	}

	public void setTableType(int tableType) {
		this.tableType = tableType;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public DeleteDialog(Context context) {
		super(context, R.style.MusicDialog);
		this.context = context;

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.app_delete_dialog);

		song = songList.get(position);
		initView();
	}

	private void initView() {
		btnCancel = (Button) findViewById(R.id.btn_cancel);
		btnDelete = (Button) findViewById(R.id.btn_delete);
		tvDeleteInfo = (TextView) findViewById(R.id.tv_delete_info);
		checkBox = (CheckBox) findViewById(R.id.checkBox);

		tvDeleteInfo.setText("确定要删除\"" + song.getDisplayName() + "\"");

		btnCancel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		btnDelete.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				String owner = MusicClient.getInstance().getUser();
				MusicDbHelper musicDb = new MusicDbHelper(context);
				musicDb.delete(song, owner, tableType);
				songList.remove(position);
				audioAdapter.notifyDataSetChanged();

				PlayListDbHelper playDb = new PlayListDbHelper(context);
				playDb.delete(song, owner);
				Cursor cursor = playDb.getCursor(owner);
				PlayControl playControl = new PlayControl((Application) context
						.getApplicationContext());
				playControl.setCursor(cursor);
				Song currentSong = playControl.getCurrentSong();

				if (currentSong != null && song.equals(currentSong)) {
					playControl.skipToNextTrack();
				}

				if (checkBox.isChecked()) {
					String filePath = song.getFilePath();
					if (FileUtils.FileExists(filePath)) {
						File file = new File(filePath);
						file.delete();
					}
				}

				dismiss();
			}
		});

	}
}
