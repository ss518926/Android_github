package com.music.adapter;

import java.util.List;

import android.content.Context;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.music.MusicClient;
import com.music.R;
import com.music.database.MusicDbHelper;
import com.music.dialog.DeleteDialog;
import com.music.dialog.MusicInfoDialog;
import com.music.entity.Song;
import com.music.utils.FileUtils;
import com.music.utils.download.DownloadUtil;
import com.music.utils.download.DownloadUtil.OnDownloadListener;

public class AudioAdapter extends BaseAdapter {

	private String owner;

	private int selectItem = -1;
	private ListView listview;
	private Context context;
	private LayoutInflater inflater = null;
	private List<Song> songList;
	private int titleId;
	private GridView menuView;
	private ItemMenuAdapter menuAdapter;

	public void initDatas(List<Song> songList) {
		this.songList = songList;
	}

	/**
	 * @param listview
	 *            操作的列表
	 * @param context
	 *            上下文
	 * @param titleId
	 *            标题Id
	 * 
	 */
	public AudioAdapter(ListView listview, Context context, int titleId) {
		this.context = context;
		owner = MusicClient.getInstance().getUser();
		this.listview = listview;
		this.titleId = titleId;
		inflater = LayoutInflater.from(context);
	}

	public void setSelectItem(int position) {
		selectItem = position;
	}

	@Override
	public int getCount() {
		if (songList != null)
			return songList.size();
		else
			return 0;
	}

	@Override
	public Object getItem(int position) {
		if (songList != null)
			return songList.get(position);
		else
			return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = inflater.inflate(R.layout.kg_audio_list_item, parent,
					false);
			holder.drag_rl = (RelativeLayout) convertView
					.findViewById(R.id.drag_handle);
			holder.toggle_menu_iv = (ImageView) convertView
					.findViewById(R.id.btn_toggle_menu);
			holder.gridView = (GridView) convertView
					.findViewById(R.id.audio_list_menu_gridview);
			holder.title_tv = (TextView) convertView.findViewById(R.id.title);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		// 歌曲名称设置
		String displayName = songList.get(position).getDisplayName();
		String trackName = songList.get(position).getTrackName();
		String songName = displayName;
		if (trackName != null) {
			songName = songName + "-" + trackName;
		}
		holder.title_tv.setText(songName);

		holder.toggle_menu_iv.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (selectItem == position) {
					selectItem = -1;
				} else {
					selectItem = position;
				}
				notifyDataSetChanged();

				int i3, i4, i5;
				View localView1, localView2;

				float f2 = context.getResources().getDimension(
						R.dimen.list_menu_item_height);

				i3 = listview.getHeight();
				i4 = listview.getFirstVisiblePosition();
				i5 = listview.getHeaderViewsCount();

				localView1 = listview.getChildAt(0);
				localView2 = listview.getChildAt(position - (i4 - i5));

				if (localView2 != null && selectItem != -1) {
					int i6 = (int) (f2 + localView2.getBottom() - i3);
					if (f2 + localView2.getBottom() > i3)
						listview.setSelectionFromTop(i4,
								-i6 + localView1.getTop());
				}

			}
		});

		if (selectItem == position) {
			holder.toggle_menu_iv.setPressed(true);
			holder.gridView.setVisibility(View.VISIBLE);

			menuAdapter = new ItemMenuAdapter(context, titleId);
			int numColumns = menuAdapter.getCount();
			if (numColumns > 5) {
				numColumns = 5;
			}
			holder.gridView.setNumColumns(numColumns);
			holder.gridView.setAdapter(menuAdapter);

			menuView = holder.gridView;
			menuView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int menuPosition, long id) {

					DoAction(Integer.parseInt(String.valueOf(menuAdapter
							.getItem(menuPosition))), songList.get(position));

					selectItem = -1;
					notifyDataSetChanged();
				}
			});
		} else {
			holder.toggle_menu_iv.setPressed(false);
			holder.gridView.setVisibility(View.GONE);
		}

		return convertView;
	}

	public static class ViewHolder {
		public TextView title_tv;
		public RelativeLayout drag_rl;
		public ImageView toggle_menu_iv;
		public GridView gridView;
	}

	/**
	 * 歌曲菜单点击事件
	 * 
	 * @param actionId
	 *            歌曲菜单点击的item
	 * @param song
	 *            选中操作的歌曲
	 */
	private void DoAction(int actionId, Song song) {
		// 歌曲菜单点击事件
		if (actionId == R.string.menu_action_addto) {
			ActionAddto(song);
		} else if (actionId == R.string.menu_action_delete) {
			ActionDelete(song);
		} else if (actionId == R.string.menu_action_down) {
			ActionDownload(song);
		} else if (actionId == R.string.menu_action_share) {
			ActionShare(song);
		} else if (actionId == R.string.menu_action_info) {
			ActionInfo(song);
		} else if (actionId == R.string.menu_action_transfer) {
			ActionTransfer(song);
		}
	}

	private int getTableType() {
		int tableType = 0;
		if (titleId == R.string.title_disc_music) {
			tableType = MusicDbHelper.TYPE_DISC;
		} else if (titleId == R.string.title_online_music) {
			tableType = MusicDbHelper.TYPE_ONLINE;
		} else if (titleId == R.string.title_favorite_music) {
			tableType = MusicDbHelper.TYPE_FAVORITE;
		} else if (titleId == R.string.title_mylist_music) {
			tableType = MusicDbHelper.TYPE_MYLIST;
		}

		return tableType;
	}

	private boolean ActionAddto(Song song) {

		MusicDbHelper musicDb = new MusicDbHelper(context);
		int flag = musicDb.insert(song, owner, MusicDbHelper.TYPE_MYLIST);
		if (flag == 1) {
			Toast.makeText(context, "歌曲已存在", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(context, "歌曲成功添加到歌单", Toast.LENGTH_SHORT).show();
		}

		return true;
	}

	private boolean ActionDelete(Song song) {

		int tableType = getTableType();

		DeleteDialog deleteDialog = new DeleteDialog(context);
		deleteDialog.setAudioAdapter(this);
		deleteDialog.setSongList(songList);
		deleteDialog.setPosition(selectItem);
		deleteDialog.setTableType(tableType);
		deleteDialog.show();

		return true;
	}

	private boolean ActionDownload(Song song) {

		MusicDbHelper musicDb = new MusicDbHelper(context);
		int flag = musicDb.insert(song, owner, MusicDbHelper.TYPE_MYLIST);
		if (flag == 1) {
			String filePath = musicDb.getFilePathBySid(owner, song.getSid());
			if (FileUtils.FileExists(filePath)) {
				Toast.makeText(context, "歌曲已下载", Toast.LENGTH_SHORT).show();
				return false;
			}
		}

		String savePath = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/Didi/Music/";
		String filePath = savePath + song.getDisplayName() + ".mp3";
		// TODO 歌曲名重名处理
		song.setFilePath(filePath);

		DownloadUtil mDownloadUtil = new DownloadUtil(1, savePath,
				song.getDisplayName() + ".mp3", song.getM4aUrl(), context);
		mDownloadUtil.start();
		Toast.makeText(context, "歌曲已添加下载", Toast.LENGTH_SHORT).show();
		final Song downSong = song;
		mDownloadUtil.setOnDownloadListener(new OnDownloadListener() {

			@Override
			public void downloadStart(int fileSize) {
			}

			@Override
			public void downloadProgress(int downloadedSize, int fileSize) {
			}

			@Override
			public void downloadEnd() {
				MusicDbHelper musicDb = new MusicDbHelper(context);
				musicDb.updata(downSong, owner, MusicDbHelper.TYPE_MYLIST);
				Toast.makeText(context,
						"歌曲" + downSong.getDisplayName() + "下载完成",
						Toast.LENGTH_SHORT).show();
			}
		});

		return true;
	}

	private boolean ActionInfo(Song song) {
		//  歌曲信息
		MusicInfoDialog dialog = new MusicInfoDialog(context, song);
		dialog.show();
		return true;
	}

	private boolean ActionTransfer(Song song) {
		// TODO 歌曲传输
		return true;
	}

	private boolean ActionShare(Song song) {
		// TODO 歌曲分享
		return true;
	}

}
