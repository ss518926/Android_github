package com.music.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.music.R;

public class MenuAdapter extends BaseAdapter {

	Context context;

	int[] titleID = { R.string.title_mylist_music, R.string.title_disc_music,
			R.string.title_online_music };
	int[] drawableId = { R.drawable.menu_icon_mylist,
			R.drawable.menu_icon_disc, R.drawable.menu_icon_online };

	// , R.string.title_favorite_music
	// , R.drawable.menu_icon_favorite

	private LayoutInflater inflater = null;

	public MenuAdapter(Context context) {
		this.context = context;
		inflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {
		return titleID.length;
	}

	@Override
	public Object getItem(int position) {
		return titleID[position];
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
			convertView = inflater.inflate(R.layout.fragment_music_menu_item,
					parent, false);
			holder.menu_tv = (TextView) convertView
					.findViewById(R.id.menu_item_text);
			holder.menu_iv = (ImageView) convertView
					.findViewById(R.id.menu_item_view);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.menu_tv.setText(context.getString(titleID[position]));
		holder.menu_iv.setImageResource(drawableId[position]);

		return convertView;
	}

	public static class ViewHolder {
		public TextView menu_tv;
		public ImageView menu_iv;
	}

}
