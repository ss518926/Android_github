package com.example.edittexthistory;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {

	private Context context;
	private EditText editText;
	private Button btnSearch;
	private HistoryPopWindow historyPopWindow;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = this;

		editText = (EditText) findViewById(R.id.editText);
		btnSearch = (Button) findViewById(R.id.btnSearch);

		editText.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				showSearchHistory(context, v);
			}
		});

		btnSearch.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				saveKeywords();
			}
		});
	}

	private void saveKeywords() {
		if (historyPopWindow == null)
			historyPopWindow = new HistoryPopWindow(context);
		String key = editText.getText().toString().trim();
		if (!key.equals(""))
			historyPopWindow.addNewKey(key);
	}

	private void showSearchHistory(Context context, View parent) {
		if (historyPopWindow == null)
			historyPopWindow = new HistoryPopWindow(context);
		historyPopWindow
				.setOnClickListener(new HistoryPopWindow.OnClickListener() {

					@Override
					public void onClick(String key) {
						editText.setText(key);
					}
				});
		historyPopWindow.setListViewWidth(editText.getWidth());
		historyPopWindow.ShowAtBottom(parent);
	}

}
