package com.music.adapter;

import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.music.R;
import com.music.entity.PathItem;

public class ScanResultAdapter extends BaseAdapter {

	private Context context;
	private List<PathItem> list;

	// 用来控制CheckBox的选中状况
	@SuppressLint("UseSparseArrays")
	private static HashMap<Integer, Boolean> isSelected = new HashMap<Integer, Boolean>();;

	public ScanResultAdapter(Context context, List<PathItem> list) {
		this.context = context;
		this.list = list;
		initDate();
	}

	// 初始化isSelected的数据
	private void initDate() {
		for (int i = 0; i < list.size(); i++) {
			getIsSelected().put(i, true);
		}
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
			convertView = inflater.inflate(R.layout.scan_result_list_item,
					parent, false);
			holder.tvFolderName = (TextView) convertView
					.findViewById(R.id.tv_folder_name);
			holder.tvFilePath = (TextView) convertView
					.findViewById(R.id.tv_file_path);
			holder.tvSongCount = (TextView) convertView
					.findViewById(R.id.tv_song_count);
			holder.checkBox = (CheckBox) convertView
					.findViewById(R.id.checkBox);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		PathItem item = list.get(position);
		holder.tvFolderName.setText(item.getFolderName());
		holder.tvFilePath.setText(item.getFilePath());
		holder.tvSongCount.setText(item.getCount()+"首");

		holder.checkBox.setChecked(getIsSelected().get(position));

		return convertView;
	}

	public static HashMap<Integer, Boolean> getIsSelected() {
		return isSelected;
	}

	public static void setIsSelected(HashMap<Integer, Boolean> isSelected) {
		ScanResultAdapter.isSelected = isSelected;
	}

	public static class ViewHolder {
		public TextView tvFolderName;
		public TextView tvFilePath;
		public TextView tvSongCount;
		public CheckBox checkBox;
	}

}
