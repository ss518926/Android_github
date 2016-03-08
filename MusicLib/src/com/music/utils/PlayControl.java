package com.music.utils;

import android.app.Application;
import android.database.Cursor;
import android.media.MediaPlayer;

import com.music.MusicCommon;
import com.music.entity.Song;
import com.music.services.AudioPlaybackService;

public class PlayControl {

	AudioPlaybackService service;
	Application mApp;

	public PlayControl(Application app) {
		mApp = app;
	}

	public boolean togglePlaybackState() {
		MusicCommon musicCommon = MusicCommon.getInstance(mApp);
		service = musicCommon.getService();
		if (service != null) {
			return service.togglePlaybackState();
		}
		return false;
	}

	public boolean skipToTrack(int trackIndex) {
		MusicCommon musicCommon = MusicCommon.getInstance(mApp);
		service = musicCommon.getService();
		if (service != null) {
			return service.skipToTrack(trackIndex);
		}
		return false;
	}

	public boolean skipToPreviousTrack() {
		MusicCommon musicCommon = MusicCommon.getInstance(mApp);
		service = musicCommon.getService();
		if (service != null) {
			return service.skipToPreviousTrack();
		}
		return false;
	}

	public boolean skipToNextTrack() {
		MusicCommon musicCommon = MusicCommon.getInstance(mApp);
		service = musicCommon.getService();
		if (service != null) {
			return service.skipToNextTrack();
		}
		return false;
	}

	public boolean prepareMediaPlayer(int songIndex) {
		MusicCommon musicCommon = MusicCommon.getInstance(mApp);
		service = musicCommon.getService();
		if (service != null) {
			return service.prepareMediaPlayer(songIndex);
		}
		return false;
	}

	public boolean isMediaPlayerPrepared() {
		MusicCommon musicCommon = MusicCommon.getInstance(mApp);
		service = musicCommon.getService();
		if (service != null) {
			return service.isMediaPlayerPrepared();
		}
		return false;
	}

	public boolean startPlayback() {
		MusicCommon musicCommon = MusicCommon.getInstance(mApp);
		service = musicCommon.getService();
		if (service != null) {
			return service.startPlayback();
		}
		return false;
	}

	public boolean stopPlayback() {
		MusicCommon musicCommon = MusicCommon.getInstance(mApp);
		service = musicCommon.getService();
		if (service != null) {
			return service.stopPlayback();
		}
		return false;
	}

	public boolean pausePlayback() {
		MusicCommon musicCommon = MusicCommon.getInstance(mApp);
		service = musicCommon.getService();
		if (service != null) {
			return service.pausePlayback();
		}
		return false;
	}

	public void seekTo(int msec) {

	}

	public MediaPlayer getMediaPlayer() {
		MusicCommon musicCommon = MusicCommon.getInstance(mApp);
		service = musicCommon.getService();
		if (service != null) {
			return service.getCurrentMediaPlayer();
		}
		return null;
	}

	public void setRepeatMode(int mode) {
		MusicCommon musicCommon = MusicCommon.getInstance(mApp);
		service = musicCommon.getService();
		if (service != null) {
			service.setRepeatMode(mode);
		}
	}

	public int getRepeatMode() {
		MusicCommon musicCommon = MusicCommon.getInstance(mApp);
		service = musicCommon.getService();
		if (service != null) {
			return service.getRepeatMode();
		}
		return 0;
	}

	public void setCursor(Cursor cursor) {
		MusicCommon musicCommon = MusicCommon.getInstance(mApp);
		service = musicCommon.getService();
		if (service != null) {
			service.setCursor(cursor);
		}
	}

	public Song getCurrentSong() {
		MusicCommon musicCommon = MusicCommon.getInstance(mApp);
		service = musicCommon.getService();
		if (service != null) {
			if (service.getCurrentSong() != null)
				return service.getCurrentSong().getSong();
		}
		return null;
	}

}
