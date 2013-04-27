package com.starbug1.android.newsapp;

import me.parappa.sdk.PaRappa;
import android.app.Activity;

import com.starbug1.android.newsapp.data.DatabaseHelper;

public abstract class AbstractActivity extends Activity {
	public abstract DatabaseHelper getDbHelper();

	public abstract int getGridColumnCount();

	public abstract void resetGridInfo();

	public PaRappa parappa_;

	public abstract void startLoading();

	public abstract void stopLoading();

	public abstract FetchFeedService getService();
}
