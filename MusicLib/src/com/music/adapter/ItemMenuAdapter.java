package com.music.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.music.R;

/**
 * 每首歌曲的菜单项
 * 
 * @author shen
 */
public class ItemMenuAdapter extends BaseAdapter {

	private static int TYPE_MYLIST = 0;// 我的歌单
	private static int TYPE_DISC = 1;// 本地音乐
	private static int TYPE_ONLINE = 2;// 在线音乐
	private static int TYPE_FAVORITE = 3;// 我的收藏

	private int menuType;

	int[][] menuTextList = {
			{ R.string.menu_action_down, R.string.menu_action_info,
					R.string.menu_action_share, R.string.menu_action_delete },
			{ R.string.menu_action_addto, R.string.menu_action_info,
					R.string.menu_action_share, R.string.menu_action_delete },
			{ R.string.menu_action_down, R.string.menu_action_addto,
					R.string.menu_action_info, R.string.menu_action_share },
			{ R.string.menu_action_down, R.string.menu_action_addto,
					R.string.menu_action_info, R.string.menu_action_share,
					R.string.menu_action_delete, R.string.menu_action_transfer } };

	int[][] menuResList = {
			{ R.drawable.audio_list_item_rightmenu_down_default,
					R.drawable.audio_list_item_rightmenu_info_default,
					R.drawable.audio_list_item_rightmenu_share_default,
					R.drawable.audio_list_item_rightmenu_delete_default },
			{ R.drawable.audio_list_item_rightmenu_addto_default,
					R.drawable.audio_list_item_rightmenu_info_default,
					R.drawable.audio_list_item_rightmenu_share_default,
					R.drawable.audio_list_item_rightmenu_delete_default },
			{ R.drawable.audio_list_item_rightmenu_down_default,
					R.drawable.audio_list_item_rightmenu_addto_default,
					R.drawable.audio_list_item_rightmenu_info_default,
					R.drawable.audio_list_item_rightmenu_share_default },
			{ R.drawable.audio_list_item_rightmenu_down_default,
					R.drawable.audio_list_item_rightmenu_addto_default,
					R.drawable.audio_list_item_rightmenu_info_default,
					R.drawable.audio_list_item_rightmenu_share_default,
					R.drawable.audio_list_item_rightmenu_delete_default,
					R.drawable.audio_list_item_rightmenu_transfer_default } };

	private Context context;
	private List<Integer> list;

	private void initMenus() {
		list = new ArrayList<Integer>();
		for (int i = 0; i < menuTextList[menuType].length; i++) {
			list.add(menuTextList[menuType][i]);
		}
	}

	/**
	 * @param context
	 * @param titleId
	 * 
	 */
	public ItemMenuAdapter(Context context, int titleId) {
		this.context = context;

		if (titleId == R.string.title_disc_music) {
			this.menuType = TYPE_DISC;
		} else if (titleId == R.string.title_online_music) {
			this.menuType = TYPE_ONLINE;
		} else if (titleId == R.string.title_favorite_music) {
			this.menuType = TYPE_FAVORITE;
		} else if (titleId == R.string.title_mylist_music) {
			this.menuType = TYPE_MYLIST;
		}

		initMenus();
	}

	public void setMenu(int menuType) {
		this.menuType = menuType;
	}

	@Override
	public int getCount() {
		if (list != null)
			return list.size();
		return 0;
	}

	@Override
	public Object getItem(int position) {
		if (this.list != null)
			return list.get(position);
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView == null) {
			holder = new ViewHolder();
			LayoutInflater inflater = LayoutInflater.from(context);
			convertView = inflater.inflate(R.layout.option_menu_item, parent,
					false);
			holder.menu_tv = (TextView) convertView
					.findViewById(R.id.menu_item_text);
			holder.menu_iv = (ImageView) convertView
					.findViewById(R.id.menu_item_view);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		int textId = menuTextList[menuType][position];
		int resId = menuResList[menuType][position];

		holder.menu_tv.setText(textId);
		holder.menu_iv.setImageResource(resId);

		return convertView;
	}

	public static class ViewHolder {
		public TextView menu_tv;
		public ImageView menu_iv;
	}

}
