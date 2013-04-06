package com.starbug1.android.newsapp;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class DeleteOldArticlesTask extends AsyncTask<String, Void, Integer> {
	private static final String TAG = "DeleteOldArticlesTask";
	private final AppPrefActivity activity_;
	private ProgressDialog dialog_;

	public DeleteOldArticlesTask(AppPrefActivity context) {
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
		dialog_ = new ProgressDialog(activity_);
		dialog_.setTitle(R.string.delete_old_articles_deleting);
		dialog_.setMessage(activity_
				.getString(R.string.delete_old_articles_deleting_articles));
		dialog_.setIndeterminate(false);
		dialog_.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog_.setCancelable(false);
		dialog_.show();
	}

	/**
	 * The system calls this to perform work in the UI thread and delivers the
	 * result from doInBackground()
	 */
	@Override
	protected void onPostExecute(Integer result) {
		dialog_.dismiss();
		String message = activity_.getResources().getString(
				R.string.deleted_old_articles_no_feeds);
		if (result > 0) {
			message = activity_.getResources().getString(
					R.string.deleted_old_articles, result);
		} else if (result < 0) {
			// error
			message = "error";
		}
		Toast.makeText(activity_, message, Toast.LENGTH_LONG).show();
	}

	public void progresCancel() {
		dialog_.dismiss();
	}
}
