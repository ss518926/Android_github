package com.music;

import com.music.services.AudioPlaybackService;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

public class MusicCommon {

	private static MusicCommon common;

	public static MusicCommon getInstance(Application app) {
		if (common == null)
			common = new MusicCommon(app);
		return common;
	}

	private MusicCommon(Application app) {

		mApp = app;
		// Application context.
		mContext = mApp;
		// SharedPreferences.
		mSharedPreferences = mApp.getSharedPreferences("com.music.player",
				Context.MODE_PRIVATE);
	}

	public static final String UPDATE_UI_BROADCAST = "com.music.player.NEW_SONG_UPDATE_UI";

	// Repeat mode constants.
	public static final int REPEAT_OFF = 0;
	public static final int REPEAT_PLAYLIST = 1;
	public static final int REPEAT_SONG = 2;
	public static final int REPEAT_RANDOM = 3;

	public static final String REPEAT_MODE = "RepeatMode";

	// SharedPreferences keys.
	public static final String CROSSFADE_ENABLED = "CrossfadeEnabled";
	public static final String CROSSFADE_DURATION = "CrossfadeDuration";
	public static final String SHUFFLE_ON = "ShuffleOn";

	// Update UI broadcast flags.
	public static final String SHOW_AUDIOBOOK_TOAST = "AudiobookToast";
	public static final String UPDATE_SEEKBAR_DURATION = "UpdateSeekbarDuration";
	public static final String UPDATE_PAGER_POSTIION = "UpdatePagerPosition";
	public static final String UPDATE_PLAYBACK_CONTROLS = "UpdatePlabackControls";
	public static final String SERVICE_STOPPING = "ServiceStopping";
	public static final String SHOW_STREAMING_BAR = "ShowStreamingBar";
	public static final String HIDE_STREAMING_BAR = "HideStreamingBar";
	public static final String UPDATE_BUFFERING_PROGRESS = "UpdateBufferingProgress";
	public static final String INIT_PAGER = "InitPager";
	public static final String NEW_QUEUE_ORDER = "NewQueueOrder";
	public static final String UPDATE_EQ_FRAGMENT = "UpdateEQFragment";

	private static SharedPreferences mSharedPreferences;
	AudioPlaybackService service;
	private LocalBroadcastManager mLocalBroadcastManager;
	private boolean mIsServiceRunning = false;
	Context mContext;
	Application mApp;

	/**
	 * Sends out a local broadcast that notifies all receivers to update their
	 * respective UI elements.
	 */
	public void broadcastUpdateUICommand(String[] updateFlags,
			String[] flagValues) {
		Intent intent = new Intent(MusicCommon.UPDATE_UI_BROADCAST);
		for (int i = 0; i < updateFlags.length; i++) {
			intent.putExtra(updateFlags[i], flagValues[i]);
		}

		mLocalBroadcastManager = LocalBroadcastManager.getInstance(mContext);
		mLocalBroadcastManager.sendBroadcast(intent);

	}

	public SharedPreferences getSharedPreferences() {
		return mSharedPreferences;
	}

	public boolean isCrossfadeEnabled() {
		return getSharedPreferences().getBoolean(MusicCommon.CROSSFADE_ENABLED,
				false);
	}

	public int getCrossfadeDuration() {
		return getSharedPreferences().getInt(MusicCommon.CROSSFADE_DURATION, 5);
	}

	public void setService(AudioPlaybackService service) {
		this.service = service;
	}

	public AudioPlaybackService getService() {
		return service;
	}

	public void setIsServiceRunning(boolean running) {
		mIsServiceRunning = running;
	}

	public boolean isServiceRunning() {
		return mIsServiceRunning;
	}

}
