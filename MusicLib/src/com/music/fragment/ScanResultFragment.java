package com.music.fragment;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.music.R;
import com.music.adapter.ScanResultAdapter;
import com.music.adapter.ScanResultAdapter.ViewHolder;
import com.music.database.ScanListDbHelper;
import com.music.entity.PathItem;

public class ScanResultFragment extends Fragment {

	private int checkNum; // 记录选中的条目数量
	private TextView tvShowCount;// 用于显示选中的条目数量
	private RelativeLayout rlSelectAll;
	private List<PathItem> list;
	private ScanResultAdapter mAdapter;
	private ListView lvScanResult;
	private CheckBox checkBox;
	private Context context;
	private ImageButton btnBack;
	private Button btnAddtoDisc;

	public ScanResultFragment(Context context, List<PathItem> list) {
		this.context = context;
		this.list = list;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.scan_result_list, container,
				false);
		btnBack = (ImageButton) rootView
				.findViewById(R.id.common_title_bar_btn_back);
		lvScanResult = (ListView) rootView.findViewById(R.id.lv_scan_result);
		checkBox = (CheckBox) rootView.findViewById(R.id.checkBox);
		tvShowCount = (TextView) rootView.findViewById(R.id.tv_select_count);
		rlSelectAll = (RelativeLayout) rootView
				.findViewById(R.id.rl_select_all);
		btnAddtoDisc = (Button) rootView.findViewById(R.id.btn_addto_disc);

		mAdapter = new ScanResultAdapter(context, list);
		lvScanResult.setAdapter(mAdapter);

		btnBack.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				((FragmentActivity) context).getSupportFragmentManager()
						.popBackStack();
			}
		});

		checkNum = list.size();
		tvShowCount.setText("已选中" + checkNum + "项");

		// 绑定listView的监听器
		lvScanResult.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// 取得ViewHolder对象，这样就省去了通过层层的findViewById去实例化我们需要的cb实例的步骤
				ViewHolder holder = (ViewHolder) arg1.getTag();
				// 改变CheckBox的状态
				holder.checkBox.toggle();
				// 将CheckBox的选中状况记录下来
				ScanResultAdapter.getIsSelected().put(arg2,
						holder.checkBox.isChecked());
				// 调整选定条目
				if (holder.checkBox.isChecked() == true) {
					checkNum++;
				} else {
					checkNum--;
				}

				boolean isSelectAll = (checkNum >= list.size());
				checkBox.setChecked(isSelectAll);
				// 用TextView显示
				tvShowCount.setText("已选中" + checkNum + "项");

			}
		});

		rlSelectAll.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				boolean isChecked = checkBox.isChecked();
				checkBox.setChecked(!isChecked);
				if (!isChecked) {
					selectAll();
				} else {
					cancelAll();
				}
			}
		});

		btnAddtoDisc.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				List<String> dirs = new ArrayList<String>();
				for (int i = 0; i < list.size(); i++) {
					if (ScanResultAdapter.getIsSelected().get(i)) {
						dirs.add(list.get(i).getFilePath());
					}
				}
				ScanListDbHelper scanListDb = new ScanListDbHelper(context);
				scanListDb.CopyToDiscDb(dirs);

				((FragmentActivity) context).getSupportFragmentManager()
						.popBackStack();
			}
		});

		return rootView;
	}

	private void selectAll() {
		// 遍历list的长度，未选的设为已选
		for (int i = 0; i < list.size(); i++) {
			if (!ScanResultAdapter.getIsSelected().get(i)) {
				ScanResultAdapter.getIsSelected().put(i, true);
			}
		}
		checkNum = list.size();
		// 刷新listview和TextView的显示
		dataChanged();
	}

	private void cancelAll() {
		// 遍历list的长度，将已选的按钮设为未选
		for (int i = 0; i < list.size(); i++) {
			if (ScanResultAdapter.getIsSelected().get(i)) {
				ScanResultAdapter.getIsSelected().put(i, false);
			}
		}

		checkNum = 0;
		// 刷新listview和TextView的显示
		dataChanged();
	}

	// 刷新listview和TextView的显示
	private void dataChanged() {
		// 通知listView刷新
		mAdapter.notifyDataSetChanged();
		// TextView显示最新的选中数目
		tvShowCount.setText("已选中" + checkNum + "项");
	};

}
