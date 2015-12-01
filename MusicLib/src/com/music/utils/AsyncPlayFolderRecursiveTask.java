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
package com.music.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.widget.Toast;

import com.music.MusicClient;
import com.music.cb.SwitchFragmentCB;
import com.music.database.ScanListDbHelper;
import com.music.entity.PathItem;
import com.music.entity.Song;
import com.music.fragment.ScanResultFragment;

public class AsyncPlayFolderRecursiveTask extends AsyncTask<String, Void, Void> {
	private static Context mContext;
	private ProgressDialog pd;
	boolean dialogVisible = true;

	private ArrayList<String> audioFilePathsInFolder = new ArrayList<String>();
	private ArrayList<String> subdirectoriesList = new ArrayList<String>();
	private ArrayList<Song> Songlist = new ArrayList<Song>();
	private ArrayList<PathItem> pathList = new ArrayList<PathItem>();

	public AsyncPlayFolderRecursiveTask(Context context) {
		mContext = context;
	}

	protected void onPreExecute() {
		pd = new ProgressDialog(mContext);
		pd.setCancelable(false);
		pd.setIndeterminate(false);
		pd.setTitle("扫描歌曲");
		pd.setButton(DialogInterface.BUTTON_NEUTRAL, "取消",
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						pd.dismiss();
					}

				});

		pd.show();

	}

	@Override
	protected Void doInBackground(String... params) {
		String folderPath = params[0];
		getAudioFilePathsInFolder(folderPath);

		// Get the list of subdirectories and iterate through them for audio
		// files.
		iterateThruFolder(folderPath);

		for (int i = 0; i < subdirectoriesList.size(); i++) {
			getAudioFilePathsInFolder(subdirectoriesList.get(i));
		}

		return null;

	}

	// Stores an ArrayList of all the audio files' paths within the specified
	// folder.
	public void getAudioFilePathsInFolder(String folderPath) {

		// We'll use a filter to retrieve a list of all files with a matching
		// extension.
		File file = new File(folderPath);
		FileExtensionFilter AUDIO_FILES_FILTER = new FileExtensionFilter(
				new String[] { ".mp3", ".3gp", ".mp4", ".m4a", ".aac", ".ts",
						".flac", ".mid", ".xmf", ".mxmf", ".midi", ".rtttl",
						".rtx", ".ota", ".imy", ".ogg", ".mkv", ".wav" });

		File[] filesInFolder = file.listFiles(AUDIO_FILES_FILTER);

		// Loop through the list of files and add their file paths to the
		// corresponding ArrayList.
		String name = FileUtils.getFilename(folderPath);
		if (!name.startsWith(".")) {// 排除隐藏文件
			for (int i = 0; i < filesInFolder.length; i++) {
				if (i == 0) {
					int count = filesInFolder.length;
					pathList.add(new PathItem(folderPath, name, count));
				}
				try {
					String filePath = filesInFolder[i].getCanonicalPath();
					audioFilePathsInFolder.add(filePath);
					Song song = extractFileMetadata(filePath);
					Songlist.add(song);
				} catch (IOException e) {
					// Skip any corrupt audilo files.
					continue;
				}
			}
		}

	}

	/*
	 * This method goes through a folder recursively and saves all its
	 * subdirectories to an ArrayList (subdirectoriesList).
	 */
	public void iterateThruFolder(String path) {

		File root = new File(path);
		File[] list = root.listFiles();

		if (list == null) {
			return;
		}

		for (File f : list) {

			publishProgress();

			if (f.isDirectory()) {
				iterateThruFolder(f.getAbsolutePath());

				if (!subdirectoriesList.contains(f.getPath())) {
					subdirectoriesList.add(f.getPath());
				}

			}

		}

	}

	@Override
	protected void onProgressUpdate(Void... v) {

		// Update the progress on the progress dialog.
		pd.setMessage("扫描到" + audioFilePathsInFolder.size() + "首歌曲");

	}

	// Extracts specific ID3 metadata from an audio file and returns them in an
	// ArrayList.
	public static Song extractFileMetadata(String filePath) {

		Cursor cursor = mContext.getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				new String[] { MediaStore.Audio.Media.DATA,
						MediaStore.Audio.Media.ARTIST,
						MediaStore.Audio.Media.ALBUM,
						MediaStore.Audio.Media.SIZE,
						MediaStore.Audio.Media.MIME_TYPE,
						MediaStore.Audio.Media.DISPLAY_NAME,
						MediaStore.Audio.Media.DURATION },
				MediaStore.Audio.Media.DATA + "='" + filePath + "'", null,
				MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

		Song song = new Song();
		int duration = 0;
		String album = null;
		String artisName = null;
		String mine_type = null;

		while (cursor.moveToNext()) {
			album = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.ALBUM));
			artisName = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.ARTIST));
			mine_type = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE));
			duration = cursor.getInt(cursor
					.getColumnIndex(MediaStore.Audio.Media.DURATION));
		}

		String title = FileUtils.getFilename(filePath);
		title = FileUtils.getFileNameNoEx(title);
		String parentPath = FileUtils.getSavePath(filePath);

		song.setFilePath(filePath);
		song.setDisplayName(title);
		song.setAlbum(album);
		song.setArtisName(artisName);
		song.setDuration(duration);
		song.setMine_type(mine_type);
		song.setParentPath(parentPath);

		return song;

	}

	// Call the player activity once we've accumulated the first song's path.

	@Override
	protected void onPostExecute(Void arg0) {

		/*
		 * Now that we have a list of audio files within the folder, pass them
		 * on to NowPlayingActivity (which will assemble the files into a cursor
		 * for the service.
		 */

		// Check if the list is empty. If it is, show a Toast message to the
		// user.
		if (audioFilePathsInFolder.size() > 0) {

			// Toast.makeText(mContext, pathList.size() + "," + Songlist.size(),
			// Toast.LENGTH_LONG).show();

			ScanListDbHelper scanListDb = new ScanListDbHelper(mContext);
			scanListDb.clearTable();
			scanListDb.insert(Songlist, MusicClient.getInstance().getUser());

			ScanResultFragment fragment = new ScanResultFragment(mContext,
					pathList);
			SwitchFragmentCB switchFragmentCB = (SwitchFragmentCB) mContext;
			switchFragmentCB.SwitchToFragment(fragment, "ScanResultFragment");

			pd.dismiss();

		} else {
			pd.dismiss();
			Toast.makeText(mContext, "没有发现音乐文件~", Toast.LENGTH_LONG).show();
		}

		mContext = null;

	}

}
