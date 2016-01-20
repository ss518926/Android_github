package com.example.edittexthistory;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * @author XieHao
 * 
 *         搜索历史弹出框
 */
public class HistoryPopWindow {

	/**
	 * Item项点击事件接口
	 * 
	 */
	public interface OnClickListener {
		public void onClick(String key);
	}

	public void setOnClickListener(OnClickListener clickListener) {
		this.clickListener = clickListener;
	}

	private OnClickListener clickListener;

	private Context context;
	private PopupWindow popupWindow;
	private ArrayList<String> datalist = new ArrayList<String>();

	private Button btnClear;
	private ListView listView;
	private HistoryAdapter adapter = new HistoryAdapter();

	private int limitSize = 5;// 存储数据上限
	private String SAVE_STATUS = "fuzzy_search";// 存储数据的标志位

	@SuppressLint("InflateParams")
	public HistoryPopWindow(final Context context) {
		this.context = context;
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View mRootView = inflater.inflate(R.layout.search_history_window, null);

		btnClear = (Button) mRootView.findViewById(R.id.btn_clear);
		listView = (ListView) mRootView.findViewById(R.id.listView);

		datalist = loadArray();
		adapter.InitDate(datalist);
		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String key = String.valueOf(adapter.getItem(position));
				if (clickListener != null) {
					clickListener.onClick(key);
				}
				dismiss();
			}
		});

		btnClear.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				datalist.clear();
				saveArray(datalist);
				dismiss();
			}
		});

		popupWindow = new PopupWindow(mRootView, LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);

		popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		popupWindow.setOutsideTouchable(true); // 设置PopupWindow点击外部区域消失
	}

	/**
	 * 在父控件底部显示
	 * 
	 * @param parent
	 */
	public void ShowAtBottom(View parent) {
		if (datalist.size() == 0)
			return;
		int[] location = new int[2];
		parent.getLocationOnScreen(location);
		popupWindow.showAtLocation(parent, Gravity.NO_GRAVITY, location[0],
				location[1] + parent.getHeight());
	}

	/**
	 * 动态设置listView的宽度
	 * 
	 * @param width
	 */
	public void setListViewWidth(int width) {
		ViewGroup.LayoutParams params = listView.getLayoutParams();
		params.width = width;
		listView.setLayoutParams(params);
	}

	public void dismiss() {
		popupWindow.dismiss();
	}

	public boolean isShowing() {
		return popupWindow.isShowing();
	}

	/**
	 * 存储新的数据
	 * 
	 * @param key
	 */
	public void addNewKey(String key) {
		if (datalist == null) {
			datalist = new ArrayList<String>();
		}

		if (datalist.contains(key))
			datalist.remove(key);

		if (datalist.size() >= limitSize) {
			datalist.remove(0);
		}

		datalist.add(key);
		saveArray(datalist);
	}

	/**
	 * 用SharedPreferences存储列表
	 * 
	 * @param list
	 * @return
	 */
	public boolean saveArray(final ArrayList<String> list) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor mEdit1 = pref.edit();

		int listSize = list.size();
		mEdit1.putInt(SAVE_STATUS + "_size", listSize);

		for (int i = 0; i < listSize; i++) {
			mEdit1.remove(SAVE_STATUS + "_" + i);
			mEdit1.putString(SAVE_STATUS + "_" + i, list.get(i));
		}

		return mEdit1.commit();
	}

	/**
	 * 从SharedPreferences取出列表数据
	 * 
	 * @return
	 */
	public ArrayList<String> loadArray() {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		int size = pref.getInt(SAVE_STATUS + "_size", 0);

		datalist = new ArrayList<String>();
		for (int i = 0; i < size; i++) {
			datalist.add(pref.getString(SAVE_STATUS + "_" + i, null));
		}

		return datalist;
	}

	private class HistoryAdapter extends BaseAdapter {

		private ArrayList<String> datalist;

		public void InitDate(ArrayList<String> datalist) {
			this.datalist = datalist;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				convertView = inflater.inflate(
						R.layout.search_history_window_item, parent, false);

				holder = new ViewHolder();
				holder.tvText = (TextView) convertView
						.findViewById(R.id.tv_text);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.tvText.setText(datalist.get(datalist.size() - 1 - position));
			// 倒序显示，最近搜索的显示在最上方

			return convertView;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public Object getItem(int position) {
			if (datalist != null)
				return datalist.get(datalist.size() - 1 - position);
			return null;
		}

		@Override
		public int getCount() {
			return datalist.size();
		}
	};

	public class ViewHolder {
		private TextView tvText;
	}

}
