/*
 * Copyright (C) 2014 Saravan Pantham
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.music.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.music.MusicCommon;
import com.music.helpers.AudioManagerHelper;
import com.music.helpers.SongHelper;

/**
 * The meat and potatoes of the entire app. Manages playback, equalizer effects,
 * and all other audio related operations.
 * 
 * @author Saravan Pantham
 */
public class AudioPlaybackService extends Service {

	// Context and Intent.
	private Context mContext;
	private Service mService;

	// Global Objects Provider.
	private MusicCommon mCommon;

	// MediaPlayer objects and flags.
	private MediaPlayer mMediaPlayer;
	private MediaPlayer mMediaPlayer2;
	private int mCurrentMediaPlayer = 1;
	private boolean mFirstRun = true;

	// AudioManager.
	private AudioManager mAudioManager;
	private AudioManagerHelper mAudioManagerHelper;

	// Flags that indicate whether the mediaPlayers have been initialized.
	private boolean mMediaPlayerPrepared = false;
	private boolean mMediaPlayer2Prepared = false;

	// Cursor object(s) that will guide the rest of this queue.
	private Cursor mCursor;

	// Holds the indeces of the current cursor, in the order that they'll be
	// played.
	private ArrayList<Integer> mPlaybackIndecesList = new ArrayList<Integer>();

	// Holds the indeces of songs that were unplayable.
	private ArrayList<Integer> mFailedIndecesList = new ArrayList<Integer>();

	// Song data helpers for each MediaPlayer object.
	private SongHelper mMediaPlayerSongHelper;
	private SongHelper mMediaPlayer2SongHelper;

	// Pointer variable.
	private int mCurrentSongIndex;

	// Custom actions for media player controls via the notification bar.
	public static final String LAUNCH_NOW_PLAYING_ACTION = "com.music.player.LAUNCH_NOW_PLAYING_ACTION";
	public static final String PREVIOUS_ACTION = "com.music.player.PREVIOUS_ACTION";
	public static final String PLAY_PAUSE_ACTION = "com.music.player.PLAY_PAUSE_ACTION";
	public static final String NEXT_ACTION = "com.music.player.NEXT_ACTION";
	public static final String STOP_SERVICE = "com.music.player.STOP_SERVICE";

	// Indicates if an enqueue/queue reordering operation was performed on the
	// original queue.
	private boolean mEnqueuePerformed = false;

	// Handler object.
	private Handler mHandler;

	// Volume variables that handle the crossfade effect.
	private float mFadeOutVolume = 1.0f;
	private float mFadeInVolume = 0.0f;

	// Crossfade.
	private int mCrossfadeDuration;

	// Indicates if the user changed the track manually.
	private boolean mTrackChangedByUser = false;

	// RemoteControlClient for use with remote controls and ICS+ lockscreen
	// controls.
	private ComponentName mMediaButtonReceiverComponent;

	// Enqueue reorder scalar.
	private int mEnqueueReorderScalar = 0;

	// Temp placeholder for GMusic Uri.
	public static final Uri URI_BEING_LOADED = Uri.parse("uri_being_loaded");

	/**
	 * Constructor that should be used whenever this service is being explictly
	 * created.
	 * 
	 * @param context
	 *            The context being passed in.
	 */
	public AudioPlaybackService(Context context) {
		mContext = context;
	}

	/**
	 * Empty constructor. Required if a custom constructor was explicitly
	 * declared (see above).
	 */
	public AudioPlaybackService() {
		super();
	}

	/**
	 * Prepares the MediaPlayer objects for first use and starts the service.
	 * The workflow of the entire service starts here.
	 * 
	 * @param intent
	 *            Calling intent.
	 * @param flags
	 *            Service flags.
	 * @param startId
	 *            Service start ID.
	 */
	@SuppressLint("NewApi")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		// Context.
		mContext = getApplicationContext();
		mService = this;
		mHandler = new Handler();

		mCommon = MusicCommon.getInstance(getApplication());
		mCommon.setService((AudioPlaybackService) this);
		mCommon.setIsServiceRunning(true);
		mAudioManager = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);

		// Initialize the MediaPlayer objects.
		initMediaPlayers();

		// Time to play nice with other music players (and audio apps) and
		// request audio focus.
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mAudioManagerHelper = new AudioManagerHelper();

		// Request audio focus for playback
		mAudioManagerHelper.setHasAudioFocus(requestAudioFocus());

		// Grab the crossfade duration for this session.
		mCrossfadeDuration = mCommon.getCrossfadeDuration();

		// Initialize audio effects (equalizer, virtualizer, bass boost) for
		// this session.

		mAudioManager
				.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);

		// The service has been successfully started.

		return START_STICKY;
	}

	/**
	 * Public interface that provides access to major events during the service
	 * startup process.
	 * 
	 * @author Saravan Pantham
	 */
	public interface PrepareServiceListener {

		/**
		 * Called when the service is up and running.
		 */
		public void onServiceRunning(AudioPlaybackService service);

		/**
		 * Called when the service failed to start. Also returns the failure
		 * reason via the exception parameter.
		 */
		public void onServiceFailed(Exception exception);

	}

	/**
	 * Initializes the MediaPlayer objects for this service session.
	 */
	private void initMediaPlayers() {

		/*
		 * Release the MediaPlayer objects if they are still valid.
		 */
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
			mMediaPlayer = null;
		}

		if (mMediaPlayer2 != null) {
			getMediaPlayer2().release();
			mMediaPlayer2 = null;
		}

		mMediaPlayer = new MediaPlayer();
		mMediaPlayer2 = new MediaPlayer();
		setCurrentMediaPlayer(1);

		getMediaPlayer().reset();
		getMediaPlayer2().reset();

		// Loop the players if the repeat mode is set to repeat the current
		// song.
		if (getRepeatMode() == MusicCommon.REPEAT_SONG) {
			getMediaPlayer().setLooping(true);
			getMediaPlayer2().setLooping(true);
		}

		try {
			mMediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
			getMediaPlayer2().setWakeMode(mContext,
					PowerManager.PARTIAL_WAKE_LOCK);
		} catch (Exception e) {
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer2 = new MediaPlayer();
			setCurrentMediaPlayer(1);
		}

		// Set the mediaPlayers' stream sources.
		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		getMediaPlayer2().setAudioStreamType(AudioManager.STREAM_MUSIC);

	}

	/**
	 * Requests AudioFocus from the OS.
	 * 
	 * @return True if AudioFocus was gained. False, otherwise.
	 */
	private boolean requestAudioFocus() {
		int result = mAudioManager.requestAudioFocus(audioFocusChangeListener,
				AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

		if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			// Stop the service.
			mService.stopSelf();
			Toast.makeText(mContext, "close_other_audio_apps",
					Toast.LENGTH_LONG).show();
			return false;
		} else {
			return true;
		}

	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	/**
	 * Listens for audio focus changes and reacts accordingly.
	 */
	private OnAudioFocusChangeListener audioFocusChangeListener = new OnAudioFocusChangeListener() {

		@Override
		public void onAudioFocusChange(int focusChange) {
			if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
				// We've temporarily lost focus, so pause the mMediaPlayer,
				// wherever it's at.
				try {
					getCurrentMediaPlayer().pause();

					mAudioManagerHelper.setHasAudioFocus(false);
				} catch (Exception e) {
					e.printStackTrace();
				}

			} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
				// Lower the current mMediaPlayer volume.
				mAudioManagerHelper.setAudioDucked(true);
				mAudioManagerHelper.setTargetVolume(5);
				mAudioManagerHelper.setStepDownIncrement(1);
				mAudioManagerHelper.setCurrentVolume(mAudioManager
						.getStreamVolume(AudioManager.STREAM_MUSIC));
				mAudioManagerHelper.setOriginalVolume(mAudioManager
						.getStreamVolume(AudioManager.STREAM_MUSIC));
				mHandler.post(duckDownVolumeRunnable);

			} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {

				if (mAudioManagerHelper.isAudioDucked()) {
					// Crank the volume back up again.
					mAudioManagerHelper.setTargetVolume(mAudioManagerHelper
							.getOriginalVolume());
					mAudioManagerHelper.setStepUpIncrement(1);
					mAudioManagerHelper.setCurrentVolume(mAudioManager
							.getStreamVolume(AudioManager.STREAM_MUSIC));

					mHandler.post(duckUpVolumeRunnable);
					mAudioManagerHelper.setAudioDucked(false);
				} else {
					// We've regained focus. Update the audioFocus tag, but
					// don't start the mMediaPlayer.
					mAudioManagerHelper.setHasAudioFocus(true);

				}

			} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
				// We've lost focus permanently so pause the service. We'll have
				// to request focus again later.
				getCurrentMediaPlayer().pause();

				mAudioManagerHelper.setHasAudioFocus(false);

			}

		}

	};

	/**
	 * Fades out volume before a duck operation.
	 */
	private Runnable duckDownVolumeRunnable = new Runnable() {

		@Override
		public void run() {
			if (mAudioManagerHelper.getCurrentVolume() > mAudioManagerHelper
					.getTargetVolume()) {
				mAudioManager
						.setStreamVolume(
								AudioManager.STREAM_MUSIC,
								(mAudioManagerHelper.getCurrentVolume() - mAudioManagerHelper
										.getStepDownIncrement()), 0);

				mAudioManagerHelper.setCurrentVolume(mAudioManager
						.getStreamVolume(AudioManager.STREAM_MUSIC));
				mHandler.postDelayed(this, 50);
			}

		}

	};

	/**
	 * Fades in volume after a duck operation.
	 */
	private Runnable duckUpVolumeRunnable = new Runnable() {

		@Override
		public void run() {
			if (mAudioManagerHelper.getCurrentVolume() < mAudioManagerHelper
					.getTargetVolume()) {
				mAudioManager
						.setStreamVolume(
								AudioManager.STREAM_MUSIC,
								(mAudioManagerHelper.getCurrentVolume() + mAudioManagerHelper
										.getStepUpIncrement()), 0);

				mAudioManagerHelper.setCurrentVolume(mAudioManager
						.getStreamVolume(AudioManager.STREAM_MUSIC));
				mHandler.postDelayed(this, 50);
			}

		}

	};

	/**
	 * Called once mMediaPlayer is prepared.
	 */
	public OnPreparedListener mediaPlayerPrepared = new OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer mediaPlayer) {

			// Update the prepared flag.
			setIsMediaPlayerPrepared(true);

			// Set the completion listener for mMediaPlayer.
			getMediaPlayer().setOnCompletionListener(onMediaPlayerCompleted);

			// Check to make sure we have AudioFocus.
			if (checkAndRequestAudioFocus() == true) {

				// Check if the the user saved the track's last playback
				// position.
				if (getMediaPlayerSongHelper().getSavedPosition() != -1) {
					// Seek to the saved track position.
					mMediaPlayer.seekTo((int) getMediaPlayerSongHelper()
							.getSavedPosition());
					mCommon.broadcastUpdateUICommand(
							new String[] { MusicCommon.SHOW_AUDIOBOOK_TOAST },
							new String[] { ""
									+ getMediaPlayerSongHelper()
											.getSavedPosition() });

				}

				// This is the first time mMediaPlayer has been prepared, so
				// start it immediately.
				if (mFirstRun) {
					startMediaPlayer();
					mFirstRun = false;
				}

			} else {
				return;
			}

		}

	};

	/**
	 * Called once mMediaPlayer2 is prepared.
	 */
	public OnPreparedListener mediaPlayer2Prepared = new OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer mediaPlayer) {

			// Update the prepared flag.
			setIsMediaPlayer2Prepared(true);

			// Set the completion listener for mMediaPlayer2.
			getMediaPlayer2().setOnCompletionListener(onMediaPlayer2Completed);

			// Check to make sure we have AudioFocus.
			if (checkAndRequestAudioFocus() == true) {

				// Check if the the user saved the track's last playback
				// position.
				if (getMediaPlayer2SongHelper().getSavedPosition() != -1) {
					// Seek to the saved track position.
					mMediaPlayer2.seekTo((int) getMediaPlayer2SongHelper()
							.getSavedPosition());
					mCommon.broadcastUpdateUICommand(
							new String[] { MusicCommon.SHOW_AUDIOBOOK_TOAST },
							new String[] { ""
									+ getMediaPlayer2SongHelper()
											.getSavedPosition() });

				}

			} else {
				return;
			}

		}

	};

	/**
	 * Completion listener for mMediaPlayer.
	 */
	private OnCompletionListener onMediaPlayerCompleted = new OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {

			// Remove the crossfade playback.
			mHandler.removeCallbacks(startCrossFadeRunnable);
			mHandler.removeCallbacks(crossFadeRunnable);

			// Set the track position handler (notifies the handler when the
			// track should start being faded).
			if (mHandler != null && mCommon.isCrossfadeEnabled()) {
				mHandler.post(startCrossFadeRunnable);
			}

			// Reset the fadeVolume variables.
			mFadeInVolume = 0.0f;
			mFadeOutVolume = 1.0f;

			// Reset the volumes for both mediaPlayers.
			getMediaPlayer().setVolume(1.0f, 1.0f);
			getMediaPlayer2().setVolume(1.0f, 1.0f);

			try {
				if (isAtEndOfQueue()
						&& getRepeatMode() != MusicCommon.REPEAT_PLAYLIST
						&& getRepeatMode() != MusicCommon.REPEAT_RANDOM) {
					finish = true;
					skipToNextTrack();
				} else if (isMediaPlayer2Prepared()) {
					startMediaPlayer2();
				} else {
					// Check every 100ms if mMediaPlayer2 is prepared.
					mHandler.post(startMediaPlayer2IfPrepared);
				}

			} catch (IllegalStateException e) {
				// mMediaPlayer2 isn't prepared yet.
				mHandler.post(startMediaPlayer2IfPrepared);
			}

		}

	};

	/**
	 * Completion listener for mMediaPlayer2.
	 */
	private OnCompletionListener onMediaPlayer2Completed = new OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {

			// Remove the crossfade playback.
			mHandler.removeCallbacks(startCrossFadeRunnable);
			mHandler.removeCallbacks(crossFadeRunnable);

			// Set the track position handler (notifies the handler when the
			// track should start being faded).
			if (mHandler != null && mCommon.isCrossfadeEnabled()) {
				mHandler.post(startCrossFadeRunnable);
			}

			// Reset the fadeVolume variables.
			mFadeInVolume = 0.0f;
			mFadeOutVolume = 1.0f;

			// Reset the volumes for both mediaPlayers.
			getMediaPlayer().setVolume(1.0f, 1.0f);
			getMediaPlayer2().setVolume(1.0f, 1.0f);

			try {
				if (isAtEndOfQueue()
						&& getRepeatMode() != MusicCommon.REPEAT_PLAYLIST
						&& getRepeatMode() != MusicCommon.REPEAT_RANDOM) {
					finish = true;
					skipToNextTrack();
				} else if (isMediaPlayerPrepared()) {
					startMediaPlayer();
				} else {
					// Check every 100ms if mMediaPlayer is prepared.
					mHandler.post(startMediaPlayerIfPrepared);
				}

			} catch (IllegalStateException e) {
				// mMediaPlayer isn't prepared yet.
				mHandler.post(startMediaPlayerIfPrepared);
			}

		}

	};

	/**
	 * Buffering listener.
	 */
	public OnBufferingUpdateListener bufferingListener = new OnBufferingUpdateListener() {

		@Override
		public void onBufferingUpdate(MediaPlayer mp, int percent) {

			if (mCommon.getSharedPreferences().getBoolean("NOW_PLAYING_ACTIVE",
					false) == true) {

				if (mp == getCurrentMediaPlayer()) {
					float max = mp.getDuration();
					int bufferdDuration = (int) (max * percent / 100);
					mCommon.broadcastUpdateUICommand(
							new String[] { MusicCommon.UPDATE_BUFFERING_PROGRESS },
							new String[] { "" + bufferdDuration });
				}

			}

		}

	};

	/**
	 * Error listener for mMediaPlayer.
	 */
	public OnErrorListener onErrorListener = new OnErrorListener() {

		@Override
		public boolean onError(MediaPlayer mMediaPlayer, int what, int extra) {
			/*
			 * This error listener might seem like it's not doing anything.
			 * However, removing this will cause the mMediaPlayer object to go
			 * crazy and skip around. The key here is to make this method return
			 * true. This notifies the mMediaPlayer object that we've handled
			 * all errors and that it shouldn't do anything else to try and
			 * remedy the situation.
			 * 
			 * TL;DR: Don't touch this interface. Ever.
			 */
			return true;
		}

	};

	/**
	 * Starts mMediaPlayer if it is prepared and ready for playback. Otherwise,
	 * continues checking every 100ms if mMediaPlayer is prepared.
	 */
	private Runnable startMediaPlayerIfPrepared = new Runnable() {

		@Override
		public void run() {
			if (isMediaPlayerPrepared())
				startMediaPlayer();
			else
				mHandler.postDelayed(this, 100);

		}

	};

	/**
	 * Starts mMediaPlayer if it is prepared and ready for playback. Otherwise,
	 * continues checking every 100ms if mMediaPlayer2 is prepared.
	 */
	private Runnable startMediaPlayer2IfPrepared = new Runnable() {

		@Override
		public void run() {
			if (isMediaPlayer2Prepared())
				startMediaPlayer2();
			else
				mHandler.postDelayed(this, 100);

		}

	};

	/**
	 * First runnable that handles the cross fade operation between two tracks.
	 */
	public Runnable startCrossFadeRunnable = new Runnable() {

		@Override
		public void run() {

			// Check if we're in the last part of the current song.
			try {
				if (getCurrentMediaPlayer().isPlaying()) {

					int currentTrackDuration = getCurrentMediaPlayer()
							.getDuration();
					int currentTrackFadePosition = currentTrackDuration
							- (mCrossfadeDuration * 1000);
					if (getCurrentMediaPlayer().getCurrentPosition() >= currentTrackFadePosition) {
						// Launch the next runnable that will handle the cross
						// fade effect.
						mHandler.postDelayed(crossFadeRunnable, 100);

					} else {
						mHandler.postDelayed(startCrossFadeRunnable, 1000);
					}

				} else {
					mHandler.postDelayed(startCrossFadeRunnable, 1000);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	};

	/**
	 * Crossfade runnable.
	 */
	public Runnable crossFadeRunnable = new Runnable() {

		@Override
		public void run() {
			try {

				// Do not crossfade if the current song is set to repeat itself.
				if (getRepeatMode() != MusicCommon.REPEAT_SONG) {

					// Do not crossfade if this is the last track in the queue.
					if (getCursor().getCount() > (mCurrentSongIndex + 1)) {

						// Set the next mMediaPlayer's volume and raise it
						// incrementally.
						if (getCurrentMediaPlayer() == getMediaPlayer()) {

							getMediaPlayer2().setVolume(mFadeInVolume,
									mFadeInVolume);
							getMediaPlayer().setVolume(mFadeOutVolume,
									mFadeOutVolume);

							// If the mMediaPlayer is already playing or it
							// hasn't been prepared yet, we can't use crossfade.
							if (!getMediaPlayer2().isPlaying()) {

								if (mMediaPlayer2Prepared == true) {

									if (checkAndRequestAudioFocus() == true) {

										// Check if the the user requested to
										// save the track's last playback
										// position.
										if (getMediaPlayer2SongHelper()
												.getSavedPosition() != -1) {
											// Seek to the saved track position.
											getMediaPlayer2()
													.seekTo((int) getMediaPlayer2SongHelper()
															.getSavedPosition());
											mCommon.broadcastUpdateUICommand(
													new String[] { MusicCommon.SHOW_AUDIOBOOK_TOAST },
													new String[] { ""
															+ getMediaPlayer2SongHelper()
																	.getSavedPosition() });

										}

										getMediaPlayer2().start();
									} else {
										return;
									}

								}

							}

						} else {

							getMediaPlayer().setVolume(mFadeInVolume,
									mFadeInVolume);
							getMediaPlayer2().setVolume(mFadeOutVolume,
									mFadeOutVolume);

							// If the mMediaPlayer is already playing or it
							// hasn't been prepared yet, we can't use crossfade.
							if (!getMediaPlayer().isPlaying()) {

								if (mMediaPlayerPrepared == true) {

									if (checkAndRequestAudioFocus() == true) {

										// Check if the the user requested to
										// save the track's last playback
										// position.
										if (getMediaPlayerSongHelper()
												.getSavedPosition() != -1) {
											// Seek to the saved track position.
											getMediaPlayer()
													.seekTo((int) getMediaPlayerSongHelper()
															.getSavedPosition());
											mCommon.broadcastUpdateUICommand(
													new String[] { MusicCommon.SHOW_AUDIOBOOK_TOAST },
													new String[] { ""
															+ getMediaPlayerSongHelper()
																	.getSavedPosition() });

										}

										getMediaPlayer().start();
									} else {
										return;
									}

								}

							}

						}

						mFadeInVolume = mFadeInVolume
								+ (float) (1.0f / (((float) mCrossfadeDuration) * 10.0f));
						mFadeOutVolume = mFadeOutVolume
								- (float) (1.0f / (((float) mCrossfadeDuration) * 10.0f));

						mHandler.postDelayed(crossFadeRunnable, 100);
					}

				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	};

	/**
	 * Grabs the song parameters at the specified index, retrieves its data
	 * source, and beings to asynchronously prepare mMediaPlayer. Once
	 * mMediaPlayer is prepared, mediaPlayerPrepared is called.
	 * 
	 * @return True if the method completed with no exceptions. False,
	 *         otherwise.
	 */
	public boolean prepareMediaPlayer(int songIndex) {

		try {

			// Stop here if we're at the end of the queue.
			if (songIndex == -1)
				return true;

			// Reset mMediaPlayer to it's uninitialized state.
			getMediaPlayer().reset();

			// Loop the player if the repeat mode is set to repeat the current
			// song.
			if (getRepeatMode() == MusicCommon.REPEAT_SONG) {
				getMediaPlayer().setLooping(true);
			}

			// Set mMediaPlayer's song data.
			SongHelper songHelper = new SongHelper();
			if (mFirstRun) {
				/*
				 * We're not preloading the next song (mMediaPlayer2 is not
				 * playing right now). mMediaPlayer's song is pointed at by
				 * mCurrentSongIndex.
				 */
				songHelper.populateSongData(mContext, songIndex);
				setMediaPlayerSongHelper(songHelper);

			} else {
				songHelper.populateSongData(mContext, songIndex);
				setMediaPlayerSongHelper(songHelper);
			}

			/*
			 * Set the data source for mMediaPlayer and start preparing it
			 * asynchronously.
			 */
			if (getMediaPlayerSongHelper().getSong() != null) {
				getMediaPlayer().setDataSource(mContext,
						getSongDataSource(getMediaPlayerSongHelper()));
				getMediaPlayer().setOnPreparedListener(mediaPlayerPrepared);
				getMediaPlayer()
						.setOnBufferingUpdateListener(bufferingListener);
				getMediaPlayer().setOnErrorListener(onErrorListener);
				getMediaPlayer().prepareAsync();
			}
		} catch (Exception e) {
			Log.e("DEBUG", "MESSAGE", e);
			e.printStackTrace();

			// Display an error toast to the user.
			showErrorToast();

			// Add the current song index to the list of failed indeces.
			getFailedIndecesList().add(songIndex);

			// Start preparing the next song.
			if (!isAtEndOfQueue() && getCursor() != null)
				prepareMediaPlayer(songIndex + 1);
			else
				return false;

			return false;
		}

		return true;
	}

	/**
	 * Grabs the song parameters at the specified index, retrieves its data
	 * source, and beings to asynchronously prepare mMediaPlayer2. Once
	 * mMediaPlayer2 is prepared, mediaPlayer2Prepared is called.
	 * 
	 * @return True if the method completed with no exceptions. False,
	 *         otherwise.
	 */
	public boolean prepareMediaPlayer2(int songIndex) {

		try {

			// Stop here if we're at the end of the queue.
			if (songIndex == -1)
				return true;

			// Reset mMediaPlayer2 to its uninitialized state.
			getMediaPlayer2().reset();

			// Loop the player if the repeat mode is set to repeat the current
			// song.
			if (getRepeatMode() == MusicCommon.REPEAT_SONG) {
				getMediaPlayer2().setLooping(true);
			}

			// Set mMediaPlayer2's song data.
			SongHelper songHelper = new SongHelper();
			songHelper.populateSongData(mContext, songIndex);
			setMediaPlayer2SongHelper(songHelper);

			/*
			 * Set the data source for mMediaPlayer and start preparing it
			 * asynchronously.
			 */
			if (getMediaPlayer2SongHelper().getSong() != null) {
				getMediaPlayer2().setDataSource(mContext,
						getSongDataSource(getMediaPlayer2SongHelper()));
				getMediaPlayer2().setOnPreparedListener(mediaPlayer2Prepared);
				getMediaPlayer2().setOnBufferingUpdateListener(
						bufferingListener);
				getMediaPlayer2().setOnErrorListener(onErrorListener);
				getMediaPlayer2().prepareAsync();
			}
		} catch (Exception e) {
			e.printStackTrace();

			// Display an error toast to the user.
			showErrorToast();

			// Add the current song index to the list of failed indeces.
			getFailedIndecesList().add(songIndex);

			// Start preparing the next song.
			if (!isAtEndOfQueue() && getCursor() != null)
				prepareMediaPlayer2(songIndex + 1);
			else
				return false;

			return false;
		}

		return true;
	}

	/**
	 * Returns the Uri of a song's data source. If the song is a local file, its
	 * file path is returned. If the song is from GMusic, its local copy path is
	 * returned (if it exists). If no local copy exists, the song's remote URL
	 * is requested from Google's servers and a temporary placeholder
	 * (URI_BEING_LOADED) is returned.
	 */
	private Uri getSongDataSource(SongHelper songHelper) {
		// TODO 获取音乐源
		if (songHelper.getFilePath() == null) {
			mCommon.getSharedPreferences().edit()
					.putBoolean("NOW_PLAYING_ACTIVE", true).commit();
			// Request the remote URL and return a placeholder Uri.
			return Uri.parse(songHelper.getM4aUrl());

		} else {
			mCommon.getSharedPreferences().edit()
					.putBoolean("NOW_PLAYING_ACTIVE", false).commit();
			// Return the song's file path.
			return Uri.parse(songHelper.getFilePath());
		}

	}

	/**
	 * Displays an error toast.
	 */
	private void showErrorToast() {
		Toast.makeText(mContext, "加载音乐失败~", Toast.LENGTH_SHORT).show();
	}

	/**
	 * Increments mCurrentSongIndex based on mErrorCount. Returns the new value
	 * of mCurrentSongIndex.
	 */
	public int incrementCurrentSongIndex() {
		if (getCursor() == null || getCursor().getCount() == 0) {
			mCurrentSongIndex = -1;
		} else if (getRepeatMode() == MusicCommon.REPEAT_RANDOM) {
			if (getCursor().getCount() < 2)
				mCurrentSongIndex = 0;
			else {
				Random rdm = new Random(System.nanoTime());
				int rd = Math.abs(rdm.nextInt()) % getCursor().getCount();
				while (rd == getCurrentSongIndex()) {
					rd = Math.abs(rdm.nextInt()) % getCursor().getCount();
				}
				mCurrentSongIndex = rd;
			}
		} else {
			if ((getCurrentSongIndex() + 1) < getCursor().getCount())
				mCurrentSongIndex++;
			else
				mCurrentSongIndex = 0;
		}
		return mCurrentSongIndex;
	}

	/**
	 * Decrements mCurrentSongIndex by one. Returns the new value of
	 * mCurrentSongIndex.
	 */
	public int decrementCurrentSongIndex() {
		if ((getCurrentSongIndex() - 1) > -1)
			mCurrentSongIndex--;

		return mCurrentSongIndex;
	}

	/**
	 * Increments mEnqueueReorderScalar. Returns the new value of
	 * mEnqueueReorderScalar.
	 */
	public int incrementEnqueueReorderScalar() {
		mEnqueueReorderScalar++;
		return mCurrentSongIndex;
	}

	/**
	 * Decrements mEnqueueReorderScalar. Returns the new value of
	 * mEnqueueReorderScalar.
	 */
	public int decrementEnqueueReorderScalar() {
		mEnqueueReorderScalar--;
		return mCurrentSongIndex;
	}

	/**
	 * Starts playing mMediaPlayer and sends out the update UI broadcast, and
	 * updates the notification and any open widgets.
	 * 
	 * Do NOT call this method before mMediaPlayer has been prepared.
	 */
	private void startMediaPlayer() throws IllegalStateException {

		// Aaaaand let the show begin!
		setCurrentMediaPlayer(1);
		if (!finish) {
			getMediaPlayer().start();
		} else {
			finish = false;
		}

		// Set the new value for mCurrentSongIndex.
		if (mFirstRun == false) {
			do {
				setCurrentSongIndex(determineNextSongIndex());
			} while (getFailedIndecesList().contains(getCurrentSongIndex()));

			getFailedIndecesList().clear();

		} else {
			while (getFailedIndecesList().contains(getCurrentSongIndex())) {
				setCurrentSongIndex(determineNextSongIndex());
			}

			// Initialize the crossfade runnable.
			if (mHandler != null && mCommon.isCrossfadeEnabled()) {
				mHandler.post(startCrossFadeRunnable);
			}

		}

		// Update the UI.
		String[] updateFlags = new String[] {
				MusicCommon.UPDATE_PAGER_POSTIION,
				MusicCommon.UPDATE_PLAYBACK_CONTROLS,
				MusicCommon.HIDE_STREAMING_BAR,
				MusicCommon.UPDATE_SEEKBAR_DURATION,
				MusicCommon.UPDATE_EQ_FRAGMENT };

		String[] flagValues = new String[] { getCurrentSongIndex() + "", "",
				"", getMediaPlayer().getDuration() + "", "" };

		mCommon.broadcastUpdateUICommand(updateFlags, flagValues);
		setCurrentSong(getCurrentSong());

		// Start preparing the next song.
		prepareMediaPlayer2(determineNextSongIndex());
	}

	/**
	 * Starts playing mMediaPlayer2, sends out the update UI broadcast, and
	 * updates the notification and any open widgets.
	 * 
	 * Do NOT call this method before mMediaPlayer2 has been prepared.
	 */
	private void startMediaPlayer2() throws IllegalStateException {

		// Aaaaaand let the show begin!
		setCurrentMediaPlayer(2);
		if (!finish) {
			getMediaPlayer2().start();
		} else {
			finish = false;
		}

		// Set the new value for mCurrentSongIndex.
		do {
			setCurrentSongIndex(determineNextSongIndex());
		} while (getFailedIndecesList().contains(getCurrentSongIndex()));

		getFailedIndecesList().clear();

		// Update the UI.
		String[] updateFlags = new String[] {
				MusicCommon.UPDATE_PAGER_POSTIION,
				MusicCommon.UPDATE_PLAYBACK_CONTROLS,
				MusicCommon.HIDE_STREAMING_BAR,
				MusicCommon.UPDATE_SEEKBAR_DURATION,
				MusicCommon.UPDATE_EQ_FRAGMENT };

		String[] flagValues = new String[] { getCurrentSongIndex() + "", "",
				"", getMediaPlayer2().getDuration() + "", "" };

		mCommon.broadcastUpdateUICommand(updateFlags, flagValues);
		setCurrentSong(getCurrentSong());

		// Start preparing the next song.
		prepareMediaPlayer(determineNextSongIndex());
	}

	/**
	 * Starts/resumes the current media player. Returns true if the operation
	 * succeeded. False, otherwise.
	 */
	public boolean startPlayback() {

		try {
			// Check to make sure we have audio focus.
			if (checkAndRequestAudioFocus()) {
				getCurrentMediaPlayer().start();

				// Update the UI and scrobbler.
				String[] updateFlags = new String[] { MusicCommon.UPDATE_SEEKBAR_DURATION };
				String[] flagValues = new String[] { "" };

				mCommon.broadcastUpdateUICommand(updateFlags, flagValues);

			} else {
				return false;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Pauses the current media player. Returns true if the operation succeeded.
	 * False, otherwise.
	 */
	public boolean pausePlayback() {

		try {
			getCurrentMediaPlayer().pause();

			// Update the UI and scrobbler.
			String[] updateFlags = new String[] { MusicCommon.UPDATE_SEEKBAR_DURATION };
			String[] flagValues = new String[] { "" };

			mCommon.broadcastUpdateUICommand(updateFlags, flagValues);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Stops the current media player. Returns true if the operation succeeded.
	 * False, otherwise.
	 */
	public boolean stopPlayback() {

		try {
			getCurrentMediaPlayer().stop();

			// Update the UI and scrobbler.
			String[] updateFlags = new String[] { MusicCommon.UPDATE_PLAYBACK_CONTROLS };
			String[] flagValues = new String[] { "" };

			mCommon.broadcastUpdateUICommand(updateFlags, flagValues);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Skips to the next track (if there is one) and starts playing it. Returns
	 * true if the operation succeeded. False, otherwise.
	 */
	public boolean skipToNextTrack() {
		try {
			// Reset both MediaPlayer objects.
			getMediaPlayer().reset();
			getMediaPlayer2().reset();
			clearCrossfadeCallbacks();

			// Loop the players if the repeat mode is set to repeat the current
			// song.
			if (getRepeatMode() == MusicCommon.REPEAT_SONG) {
				getMediaPlayer().setLooping(true);
				getMediaPlayer2().setLooping(true);
			}

			// Remove crossfade runnables and reset all volume levels.
			getHandler().removeCallbacks(crossFadeRunnable);
			getMediaPlayer().setVolume(1.0f, 1.0f);
			getMediaPlayer2().setVolume(1.0f, 1.0f);

			// Increment the song index.
			incrementCurrentSongIndex();

			if (getCurrentSongIndex() == -1) {
				mMediaPlayerSongHelper = null;
				mMediaPlayer2SongHelper = null;
			}

			// Update the UI.
			String[] updateFlags = new String[] { MusicCommon.UPDATE_PAGER_POSTIION };
			String[] flagValues = new String[] { getCurrentSongIndex() + "" };
			mCommon.broadcastUpdateUICommand(updateFlags, flagValues);

			// Start the playback process.
			mFirstRun = true;
			prepareMediaPlayer(getCurrentSongIndex());

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Skips to the previous track (if there is one) and starts playing it.
	 * Returns true if the operation succeeded. False, otherwise.
	 */
	public boolean skipToPreviousTrack() {

		/*
		 * If the current track is not within the first three seconds, reset it.
		 * If it IS within the first three seconds, skip to the previous track.
		 */
		try {
			if (getCurrentMediaPlayer().getCurrentPosition() > 3000) {
				getCurrentMediaPlayer().seekTo(0);
				return true;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		try {
			// Reset both MediaPlayer objects.
			getMediaPlayer().reset();
			getMediaPlayer2().reset();
			clearCrossfadeCallbacks();

			// Loop the players if the repeat mode is set to repeat the current
			// song.
			if (getRepeatMode() == MusicCommon.REPEAT_SONG) {
				getMediaPlayer().setLooping(true);
				getMediaPlayer2().setLooping(true);
			}

			// Remove crossfade runnables and reset all volume levels.
			getHandler().removeCallbacks(crossFadeRunnable);
			getMediaPlayer().setVolume(1.0f, 1.0f);
			getMediaPlayer2().setVolume(1.0f, 1.0f);

			// Decrement the song index.
			decrementCurrentSongIndex();

			// Update the UI.
			String[] updateFlags = new String[] { MusicCommon.UPDATE_PAGER_POSTIION };
			String[] flagValues = new String[] { getCurrentSongIndex() + "" };
			mCommon.broadcastUpdateUICommand(updateFlags, flagValues);

			// Start the playback process.
			mFirstRun = true;
			prepareMediaPlayer(getCurrentSongIndex());

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Skips to the specified track index (if there is one) and starts playing
	 * it. Returns true if the operation succeeded. False, otherwise.
	 */
	public boolean skipToTrack(int trackIndex) {
		try {
			// Reset both MediaPlayer objects.
			getMediaPlayer().reset();
			getMediaPlayer2().reset();
			clearCrossfadeCallbacks();

			// Loop the players if the repeat mode is set to repeat the current
			// song.
			if (getRepeatMode() == MusicCommon.REPEAT_SONG) {
				getMediaPlayer().setLooping(true);
				getMediaPlayer2().setLooping(true);
			}

			// Remove crossfade runnables and reset all volume levels.
			getHandler().removeCallbacks(crossFadeRunnable);
			getMediaPlayer().setVolume(1.0f, 1.0f);
			getMediaPlayer2().setVolume(1.0f, 1.0f);

			// Update the song index.
			setCurrentSongIndex(trackIndex);

			// Update the UI.
			String[] updateFlags = new String[] { MusicCommon.UPDATE_PAGER_POSTIION };
			String[] flagValues = new String[] { getCurrentSongIndex() + "" };
			mCommon.broadcastUpdateUICommand(updateFlags, flagValues);

			// Start the playback process.
			mFirstRun = true;
			prepareMediaPlayer(trackIndex);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Toggles the playback state between playing and paused and returns whether
	 * the current media player is now playing music or not.
	 */
	public boolean togglePlaybackState() {
		if (isPlayingMusic())
			pausePlayback();
		else
			startPlayback();

		return isPlayingMusic();
	}

	/**
	 * Determines the next song's index based on the repeat mode and current
	 * song index. Returns -1 if we're at the end of the queue.
	 */
	private int determineNextSongIndex() {
		if (isAtEndOfQueue() && getRepeatMode() == MusicCommon.REPEAT_PLAYLIST)
			return 0;
		else if (!isAtEndOfQueue()
				&& getRepeatMode() == MusicCommon.REPEAT_SONG)
			return getCurrentSongIndex();
		else if (getRepeatMode() == MusicCommon.REPEAT_RANDOM) {
			if (getCursor().getCount() < 2) {
				return 0;
			}
			Random rdm = new Random(System.nanoTime());
			int rd = Math.abs(rdm.nextInt()) % getCursor().getCount();
			while (rd == getCurrentSongIndex()) {
				rd = Math.abs(rdm.nextInt()) % getCursor().getCount();
			}
			return rd;
		} else if (isAtEndOfQueue())
			return -1;
		else
			return (getCurrentSongIndex() + 1);

	}

	/**
	 * Checks which MediaPlayer object is currently in use, and starts preparing
	 * the other one.
	 */
	public void prepareAlternateMediaPlayer() {
		if (mCurrentMediaPlayer == 1)
			prepareMediaPlayer2(determineNextSongIndex());
		else
			prepareMediaPlayer(determineNextSongIndex());

	}

	/**
	 * Toggles shuffle mode and returns whether shuffle is now on or off.
	 */
	public boolean toggleShuffleMode() {
		if (isShuffleOn()) {
			// Set shuffle off.
			mCommon.getSharedPreferences().edit()
					.putBoolean(MusicCommon.SHUFFLE_ON, false).commit();

			// Save the element at the current index.
			int currentElement = getPlaybackIndecesList().get(
					getCurrentSongIndex());

			// Reset the cursor pointers list.
			Collections.sort(getPlaybackIndecesList());

			// Reset the current index to the index of the old element.
			setCurrentSongIndex(getPlaybackIndecesList()
					.indexOf(currentElement));

		} else {
			// Set shuffle on.
			mCommon.getSharedPreferences().edit()
					.putBoolean(MusicCommon.SHUFFLE_ON, true).commit();

			// Build a new list that doesn't include the current song index.
			ArrayList<Integer> newList = new ArrayList<Integer>(
					getPlaybackIndecesList());
			newList.remove(getCurrentSongIndex());

			// Shuffle the new list.
			Collections.shuffle(newList, new Random(System.nanoTime()));

			// Plug in the current song index back into the new list.
			newList.add(getCurrentSongIndex(), getCurrentSongIndex());
			mPlaybackIndecesList = newList;

			// Collections.shuffle(getPlaybackIndecesList().subList(0,
			// getCurrentSongIndex()));
			// Collections.shuffle(getPlaybackIndecesList().subList(getCurrentSongIndex()+1,
			// getPlaybackIndecesList().size()));

		}

		/*
		 * Since the queue changed, we're gonna have to update the next
		 * MediaPlayer object with the new song info.
		 */
		prepareAlternateMediaPlayer();

		// Update all UI elements with the new queue order.
		mCommon.broadcastUpdateUICommand(
				new String[] { MusicCommon.NEW_QUEUE_ORDER },
				new String[] { "" });
		return isShuffleOn();
	}

	/**
	 * Applies the specified repeat mode.
	 */
	public void setRepeatMode(int repeatMode) {
		if (repeatMode == MusicCommon.REPEAT_OFF
				|| repeatMode == MusicCommon.REPEAT_PLAYLIST
				|| repeatMode == MusicCommon.REPEAT_SONG
				|| repeatMode == MusicCommon.REPEAT_RANDOM) {
			// Save the repeat mode.
			mCommon.getSharedPreferences().edit()
					.putInt(MusicCommon.REPEAT_MODE, repeatMode).commit();
		} else {
			// Just in case a bogus value is passed in.
			mCommon.getSharedPreferences().edit()
					.putInt(MusicCommon.REPEAT_MODE, MusicCommon.REPEAT_OFF)
					.commit();
		}

		/*
		 * Set the both MediaPlayer objects to loop if the repeat mode is
		 * Common.REPEAT_SONG.
		 */
		try {
			if (repeatMode == MusicCommon.REPEAT_SONG) {
				getMediaPlayer().setLooping(true);
				getMediaPlayer2().setLooping(true);
			} else {
				getMediaPlayer().setLooping(false);
				getMediaPlayer2().setLooping(false);
			}

			// Prepare the appropriate next song.
			if (getCurrentSongIndex() != -1)
				prepareAlternateMediaPlayer();

		} catch (Exception e) {
			e.printStackTrace();
		}

		/*
		 * Remove the crossfade callbacks and reinitalize them only if the user
		 * didn't select A-B repeat.
		 */
		clearCrossfadeCallbacks();

		if (mHandler != null && mCommon.isCrossfadeEnabled())
			mHandler.post(startCrossFadeRunnable);

	}

	/**
	 * Returns the current active MediaPlayer object.
	 */
	public MediaPlayer getCurrentMediaPlayer() {
		if (mCurrentMediaPlayer == 1)
			return mMediaPlayer;
		else
			return mMediaPlayer2;
	}

	/**
	 * Returns the primary MediaPlayer object. Don't use this method directly
	 * unless you have a good reason to explicitly call mMediaPlayer. Use
	 * getCurrentMediaPlayer() whenever possible.
	 */
	public MediaPlayer getMediaPlayer() {
		return mMediaPlayer;
	}

	/**
	 * Returns the secondary MediaPlayer object. Don't use this method directly
	 * unless you have a good reason to explicitly call mMediaPlayer2. Use
	 * getCurrentMediaPlayer() whenever possible.
	 */
	public MediaPlayer getMediaPlayer2() {
		return mMediaPlayer2;
	}

	/**
	 * Indicates if mMediaPlayer is prepared and ready for playback.
	 */
	public boolean isMediaPlayerPrepared() {
		return mMediaPlayerPrepared;
	}

	/**
	 * Indicates if mMediaPlayer2 is prepared and ready for playback.
	 */
	public boolean isMediaPlayer2Prepared() {
		return mMediaPlayer2Prepared;
	}

	/**
	 * Indicates if music is currently playing.
	 */
	public boolean isPlayingMusic() {
		try {
			if (getCurrentMediaPlayer().isPlaying())
				return true;
			else
				return false;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	/**
	 * Returns an instance of SongHelper. This object can be used to pull
	 * details about the current song.
	 */
	public SongHelper getCurrentSong() {
		if (getCurrentMediaPlayer() == mMediaPlayer) {
			return mMediaPlayerSongHelper;
		} else {
			return mMediaPlayer2SongHelper;
		}

	}

	/**
	 * Removes all crossfade callbacks on the current Handler object. Also
	 * resets the volumes of the MediaPlayer objects to 1.0f.
	 */
	private void clearCrossfadeCallbacks() {
		if (mHandler == null)
			return;

		mHandler.removeCallbacks(startCrossFadeRunnable);
		mHandler.removeCallbacks(crossFadeRunnable);

		try {
			getMediaPlayer().setVolume(1.0f, 1.0f);
			getMediaPlayer2().setVolume(1.0f, 1.0f);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Returns mMediaPlayer's SongHelper instance.
	 */
	public SongHelper getMediaPlayerSongHelper() {
		return mMediaPlayerSongHelper;
	}

	/**
	 * Returns mMediaPlayer2's SongHelper instance.
	 */
	public SongHelper getMediaPlayer2SongHelper() {
		return mMediaPlayer2SongHelper;
	}

	/**
	 * Returns the service's cursor object.
	 */
	public Cursor getCursor() {
		return mCursor;
	}

	/**
	 * Returns the list of playback indeces that are used to traverse the cursor
	 * object.
	 */
	public ArrayList<Integer> getPlaybackIndecesList() {
		return mPlaybackIndecesList;
	}

	/**
	 * Returns the list of playback indeces that could not be played.
	 */
	public ArrayList<Integer> getFailedIndecesList() {
		return mFailedIndecesList;
	}

	/**
	 * Returns the current value of mCurrentSongIndex.
	 */
	public int getCurrentSongIndex() {
		return mCurrentSongIndex;
	}

	/**
	 * Indicates if the track was changed by the user.
	 */
	public boolean getTrackChangedByUser() {
		return mTrackChangedByUser;
	}

	/**
	 * Indicates if an enqueue operation was performed.
	 */
	public boolean getEnqueuePerformed() {
		return mEnqueuePerformed;
	}

	/**
	 * Returns the mAudioManagerHelper instance. This can be used to modify
	 * AudioFocus states.
	 */
	public AudioManagerHelper getAudioManagerHelper() {
		return mAudioManagerHelper;
	}

	/**
	 * Returns the mHandler object.
	 */
	public Handler getHandler() {
		return mHandler;
	}

	/**
	 * Returns the current enqueue reorder scalar.
	 */
	public int getEnqueueReorderScalar() {
		return mEnqueueReorderScalar;
	}

	/**
	 * Returns the current repeat mode. The repeat mode is determined based on
	 * the value that is saved in SharedPreferences.
	 */
	public int getRepeatMode() {
		return mCommon.getSharedPreferences().getInt(MusicCommon.REPEAT_MODE,
				MusicCommon.REPEAT_OFF);
	}

	/**
	 * Indicates if shuffle mode is turned on or off.
	 */
	public boolean isShuffleOn() {
		return mCommon.getSharedPreferences().getBoolean(
				MusicCommon.SHUFFLE_ON, false);
	}

	/**
	 * Indicates if mCurrentSongIndex points to the last song in the current
	 * queue.
	 */
	public boolean isAtEndOfQueue() {
		if (getCursor() != null) {
			int count = getCursor().getCount();
			int index = getCurrentSongIndex();
			if (index + 1 >= count) {
				return true;
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * Indicates if mCurrentSongIndex points to the first song in the current
	 * queue.
	 */
	public boolean isAtStartOfQueue() {
		return getCurrentSongIndex() == 0;
	}

	/**
	 * Sets the current active media player. Note that this method does not
	 * modify the MediaPlayer objects in any way. It simply changes the int
	 * variable that points to the new current MediaPlayer object.
	 */
	public void setCurrentMediaPlayer(int currentMediaPlayer) {
		mCurrentMediaPlayer = currentMediaPlayer;
	}

	/**
	 * Sets the prepared flag for mMediaPlayer.
	 */
	public void setIsMediaPlayerPrepared(boolean prepared) {
		mMediaPlayerPrepared = prepared;
	}

	/**
	 * Sets the prepared flag for mMediaPlayer2.
	 */
	public void setIsMediaPlayer2Prepared(boolean prepared) {
		mMediaPlayer2Prepared = prepared;
	}

	/**
	 * Changes the value of mCurrentSongIndex.
	 */
	public void setCurrentSongIndex(int currentSongIndex) {
		mCurrentSongIndex = currentSongIndex;
	}

	/**
	 * Sets whether the track was changed by the user or not.
	 */
	public void setTrackChangedByUser(boolean trackChangedByUser) {
		mTrackChangedByUser = trackChangedByUser;
	}

	/**
	 * Sets whether an enqueue operation was performed or not.
	 */
	public void setEnqueuePerformed(boolean enqueuePerformed) {
		mEnqueuePerformed = enqueuePerformed;
	}

	/**
	 * Sets the new enqueue reorder scalar value.
	 */
	public void setEnqueueReorderScalar(int scalar) {
		mEnqueueReorderScalar = scalar;
	}

	/**
	 * Replaces the current cursor object with the new one.
	 */
	public void setCursor(Cursor cursor) {
		mCursor = cursor;
	}

	/**
	 * Moves the cursor back to the first song in the queue. This does not
	 * necessarily move the cursor to index 0. It moves it to the element at
	 * index 0 in mPlaybackIndecesList.
	 */
	public void moveCursorToQueueStart() {
		getCursor().moveToPosition(getPlaybackIndecesList().get(0));
	}

	/**
	 * Moves the cursor forward to the last song in the queue. This does not
	 * necessarily move the cursor to index {cursorSize-1}. It moves it to the
	 * element at index {cursorSize-1} in mPlaybackIndecesList.
	 */
	public void moveCursorToQueueEnd() {
		getCursor().moveToPosition(
				getPlaybackIndecesList().get(
						getPlaybackIndecesList().size() - 1));
	}

	/**
	 * Moves the cursor to the specified index in the queue. Returns true if the
	 * index was valid and the cursor position was moved successfully. False,
	 * otherwise.
	 */
	public boolean moveCursorToIndex(int index) {
		if (index < getCursor().getCount() && index > -1) {
			getCursor().moveToPosition(getPlaybackIndecesList().get(index));
			return true;
		} else {
			return false;
		}

	}

	/**
	 * Returns true if there's only one song in the current queue. False,
	 * otherwise.
	 */
	public boolean isOnlySongInQueue() {
		if (getCurrentSongIndex() == 0 && getCursor().getCount() == 1)
			return true;
		else
			return false;

	}

	/**
	 * Returns true if mCurrentSongIndex is pointing at the first song in the
	 * queue and there is more than one song in the queue. False, otherwise.
	 */
	public boolean isFirstSongInQueue() {
		if (getCurrentSongIndex() == 0 && getCursor().getCount() > 1)
			return true;
		else
			return false;

	}

	/**
	 * Returns true if mCurrentSongIndex is pointing at the last song in the
	 * queue. False, otherwise.
	 */
	public boolean isLastSongInQueue() {
		if (getCurrentSongIndex() == (getCursor().getCount() - 1))
			return true;
		else
			return false;

	}

	/**
	 * Sets mMediaPlayerSongHelper.
	 */
	public void setMediaPlayerSongHelper(SongHelper songHelper) {
		mMediaPlayerSongHelper = songHelper;
	}

	/**
	 * Sets mMediaPlayer2SongHelper.
	 */
	public void setMediaPlayer2SongHelper(SongHelper songHelper) {
		mMediaPlayer2SongHelper = songHelper;
	}

	/**
	 * Sets the current MediaPlayer's SongHelper object. Also indirectly calls
	 * the updateNotification() and updateWidgets() methods via the [CURRENT
	 * SONG HELPER].setIsCurrentSong() method.
	 */
	private void setCurrentSong(SongHelper songHelper) {
		if (getCurrentMediaPlayer() == mMediaPlayer) {
			mMediaPlayerSongHelper = songHelper;
		} else {
			mMediaPlayer2SongHelper = songHelper;
		}

	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {

		// Notify the UI that the service is about to stop.
		mCommon.broadcastUpdateUICommand(
				new String[] { MusicCommon.SERVICE_STOPPING },
				new String[] { "" });

		// Save the last track's info within the current queue.
		try {
			mCommon.getSharedPreferences()
					.edit()
					.putLong("LAST_SONG_TRACK_POSITION",
							getCurrentMediaPlayer().getCurrentPosition());
		} catch (Exception e) {
			e.printStackTrace();
			mCommon.getSharedPreferences().edit()
					.putLong("LAST_SONG_TRACK_POSITION", 0);
		}

		// If the current song is repeating a specific range, reset the repeat
		// option.
		if (getRepeatMode() == MusicCommon.REPEAT_SONG) {
			setRepeatMode(MusicCommon.REPEAT_OFF);
		}

		mFadeInVolume = 0.0f;
		mFadeOutVolume = 1.0f;

		if (mMediaPlayer != null)
			mMediaPlayer.release();

		if (mMediaPlayer2 != null)
			getMediaPlayer2().release();

		mMediaPlayer = null;
		mMediaPlayer2 = null;

		// Close the cursor(s).
		try {
			getCursor().close();
			setCursor(null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Remove audio focus and unregister the audio buttons receiver.
		mAudioManagerHelper.setHasAudioFocus(false);
		mAudioManager.abandonAudioFocus(audioFocusChangeListener);
		mAudioManager = null;
		mMediaButtonReceiverComponent = null;

		// Nullify the service object.
		mCommon.setService(null);
		mCommon.setIsServiceRunning(false);
		mCommon = null;

	}

	private boolean checkAndRequestAudioFocus() {
		if (mAudioManagerHelper.hasAudioFocus() == false) {
			if (requestAudioFocus() == true) {
				return true;
			} else {
				// Unable to get focus. Notify the user.
				return false;
			}

		} else {
			return true;
		}

	}

	private boolean finish = false;

}
