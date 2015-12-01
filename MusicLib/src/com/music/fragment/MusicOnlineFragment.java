package com.music.fragment;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.music.MusicClient;
import com.music.R;
import com.music.entity.Song;
import com.music.utils.SongAPI;

/**
 * 
 * 在线音乐
 * 
 * @author shen
 * 
 */
public class MusicOnlineFragment extends AudioPlayFragment {

	private static final int SET_MUSIC_LIST = 1;

	private List<Song> musicList;

	private String owner;
	private Context context;

	private ProgressBar progressBar;
	private TextView tvEmptyDesc;
	private RelativeLayout llSerachBar;
	private ImageButton btnSearch, btnClearText;
	private EditText etSearch;

	Handler handler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {

			switch (msg.what) {
			case SET_MUSIC_LIST:
				if (musicList == null) {
					Toast.makeText(context, "音乐服务器连接失败~", Toast.LENGTH_SHORT).show();
				}
				setSongList(musicList, "没有搜索结果~");
				break;
			}
			return false;
		}

	});

	/**
	 * @param context
	 * @param title
	 *            标题
	 * @param hasTitle
	 *            是否显示标题栏
	 */
	public MusicOnlineFragment(final Context context, final int titleId,
			boolean hasTitle) {
		super(context, titleId, hasTitle);
		this.context = context;
		owner = MusicClient.getInstance().getUser();

	}

	private void GetOnlineMusic(String searchKey) {
		musicList = new ArrayList<Song>();
		setSearchKey(searchKey);
		musicList = SongAPI.Search(searchKey, 1);

		InitPlaylist(musicList, owner);
		handler.sendEmptyMessage(SET_MUSIC_LIST);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.audio_list, container, false);
		this.inflater = inflater;

		initBasicView();
		initPlayingBar();

		progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
		progressBar.setVisibility(View.GONE);
		tvEmptyDesc = (TextView) rootView.findViewById(R.id.tv_empty_desc);

		llSerachBar = (RelativeLayout) rootView
				.findViewById(R.id.navigation_search_result_bar);
		llSerachBar.setVisibility(View.VISIBLE);

		etSearch = (EditText) rootView
				.findViewById(R.id.navigation_search_edit);
		btnSearch = (ImageButton) rootView
				.findViewById(R.id.navigation_search_button);
		btnClearText = (ImageButton) rootView
				.findViewById(R.id.navigation_search_text_clear_button);

		etSearch.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (etSearch.getText().toString().length() == 0) {
					btnClearText.setVisibility(View.GONE);
				} else {
					btnClearText.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		btnSearch.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				final String searchKey = etSearch.getText().toString().trim();
				if (searchKey.equals("")) {
					Toast.makeText(context, "请输入搜索内容~", Toast.LENGTH_SHORT)
							.show();
					return;
				}

				setSongList(null, "");

				InputMethodManager imm = (InputMethodManager) context
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);

				tvEmptyDesc.setVisibility(View.GONE);
				progressBar.setVisibility(View.VISIBLE);
				new Thread(new Runnable() {
					public void run() {
						GetOnlineMusic(searchKey);
					}
				}).start();
			}
		});

		btnClearText.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				etSearch.setText("");
			}
		});

		return rootView;
	}

}
