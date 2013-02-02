package com.starbug1.android.newsapp;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class DeleteOldArticlesTask extends AsyncTask<String, Void, Integer> {
	private static final String TAG = "DeleteOldArticlesTask";
	private final MainActivity activity_;

	public DeleteOldArticlesTask(MainActivity context) {
		activity_ = context;
	}

	/**
	 * The system calls this to perform work in a worker thread and delivers it
	 * the parameters given to AsyncTask.execute()
	 */
	@Override
	protected Integer doInBackground(String... args) {
		return activity_.getDbHelper().deleteOldArticles();
	}

	@Override
	protected void onPreExecute() {
		Log.d(TAG, "onPreExecute");
		activity_.startLoading();
	}

	/**
	 * The system calls this to perform work in the UI thread and delivers the
	 * result from doInBackground()
	 */
	@Override
	protected void onPostExecute(Integer result) {
		String message = activity_.getResources().getString(
				R.string.deleted_old_articles_no_feeds);
		if (result > 0) {
			message = activity_.getResources().getString(
					R.string.deleted_old_articles, result);
		} else if (result < 0) {
			// error
			message = "error";
		}
		activity_.stopLoading();
		Toast.makeText(activity_, message, Toast.LENGTH_LONG).show();
	}

	public void progresCancel() {
		activity_.stopLoading();
	}
}
