/**
 * 
 */
package com.starbug1.android.newsapp;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.GridView;

import com.starbug1.android.newsapp.data.DatabaseHelper;
import com.starbug1.android.newsapp.data.NewsListItem;

/**
 * @author smeghead
 *
 */
public class NewsCollectTask extends AsyncTask<String, Integer, List<NewsListItem>> {
	private static final String TAG = "NewsCollectTask";
	
	private final MainActivity activity_;
	private final DatabaseHelper dbHelper_;
	private final NewsListAdapter adapter_;
	private final GridView grid_;
	private int page_;

	public NewsCollectTask(MainActivity activity, DatabaseHelper helper, GridView grid, NewsListAdapter adapter) {
		activity_ = activity;
		dbHelper_ = helper;
		grid_ = grid;
		adapter_ = adapter;
		activity_.gridUpdating = true;
	}
	
	@Override
	protected void onPreExecute() {
		Log.d(TAG, "onPreExecute");
		activity_.startLoading();
	}
	
	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected List<NewsListItem> doInBackground(String... params) {
		Log.d(TAG, "doInBackground");
		final List<NewsListItem> result = new ArrayList<NewsListItem>(30);
		page_ = Integer.parseInt(params[0]);
		return dbHelper_.getItems(result, activity_, page_);
	}

	@Override
	protected void onPostExecute(List<NewsListItem> result) {
		Log.d(TAG, "onPostExecute ");
		progresCancel();
		
		activity_.hasNextPage = result.size() > DatabaseHelper.MAX_ENTRIES_PER_PAGE;
		int addedCount = 0;
		for (NewsListItem item : result) {
			if (addedCount >= DatabaseHelper.MAX_ENTRIES_PER_PAGE) {
				break;
			}
			adapter_.add(item);
			addedCount++;
		}
		if (page_ == 0) {
			grid_.setAdapter(adapter_);
		}
		activity_.stopLoading();
		activity_.gridUpdating = false;
	}
	
	public void progresCancel() {
		activity_.stopLoading();
	}
}
