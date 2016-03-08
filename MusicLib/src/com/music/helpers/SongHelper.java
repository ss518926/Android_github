package com.music.helpers;

import android.content.Context;

import com.music.MusicClient;
import com.music.database.MusicDbHelper;
import com.music.database.PlayListDbHelper;
import com.music.entity.Song;
import com.music.utils.FileUtils;

public class SongHelper {

	// Song parameters.
	private Song song;
	private long mSavedPosition;
	private Context context;
	private String owner;

	/**
	 * Moves the specified cursor to the specified index and populates this
	 * helper object with new song data.
	 * 
	 * @param context
	 *            Context used to get a new Common object.
	 * @param index
	 *            The index of the song.
	 */
	public void populateSongData(Context context, int index) {
		this.context = context;
		owner=MusicClient.getInstance().getUser();
		PlayListDbHelper playDb = new PlayListDbHelper(context);
		this.song = playDb.query(owner, index);
	}

	public Song getSong() {
		return song;
	}
	
	public String getFilePath() {
		String filePath = song.getFilePath();
		
		if (!FileUtils.FileExists(filePath) && !"-1".equals(song.getSid())) {
			MusicDbHelper musicDb = new MusicDbHelper(context);
			filePath = musicDb.getFilePathBySid(owner, song.getSid());
			if (!FileUtils.FileExists(filePath)) {
				return null;
			}
		}
		
		return filePath;
	}

	public String getM4aUrl() {
		return song.getM4aUrl();
	}

	public void setSavedPosition(long savedPosition) {
		mSavedPosition = savedPosition;
	}

	public long getSavedPosition() {
		return mSavedPosition;
	}

}
