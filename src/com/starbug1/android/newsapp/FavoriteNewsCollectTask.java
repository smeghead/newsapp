/**
 * 
 */
package com.starbug1.android.newsapp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import com.starbug1.android.newsapp.data.DatabaseHelper;
import com.starbug1.android.newsapp.data.FavoriteMonth;

/**
 * @author smeghead
 *
 */
public class FavoriteNewsCollectTask extends AsyncTask<String, Integer, List<FavoriteMonth>> {
	private final String TAG = "FavoriteNewsCollectTask";
	private final FavoriteListActivity activity_;
	private final DatabaseHelper dbHelper_;
	
	public FavoriteNewsCollectTask(FavoriteListActivity activity, DatabaseHelper dbHelper) {
		Log.d(TAG, "FavoriteNewsCollectTask");
		activity_ = activity;
		dbHelper_ = dbHelper;
		activity_.gridUpdating = true;
	}
	
	@Override
	protected void onPreExecute() {
		activity_.startLoading();
	}
	
	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected List<FavoriteMonth> doInBackground(String... params) {
		final List<FavoriteMonth> result = new ArrayList<FavoriteMonth>();
		final List<Date> months = new ArrayList<Date>();

		return dbHelper_.getFavoriteItems(result, months);
	}


	@Override
	protected void onPostExecute(List<FavoriteMonth> result) {
		progresCancel();
		
		final ListView list = (ListView)activity_.findViewById(R.id.favorite_blocks);
		
		final FavoriteMonthListAdapter adapter = new FavoriteMonthListAdapter(activity_, dbHelper_);
		for (FavoriteMonth month : result) {
			adapter.add(month);
		}
		list.setAdapter(adapter);
		activity_.stopLoading();
		
		activity_.findViewById(R.id.no_favorites_message).setVisibility(result.size() == 0 ? TextView.VISIBLE : TextView.GONE);
	}
	
	public void progresCancel() {
		activity_.stopLoading();
	}
}
