package com.starbug1.android.newsapp;

import me.parappa.sdk.PaRappa;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.starbug1.android.newsapp.data.DatabaseHelper;
import com.starbug1.android.newsapp.utils.AppUtils;
import com.starbug1.android.newsapp.utils.GIFView;

public class FavoriteListActivity extends AbstractActivity {
	private static final String TAG = "FavoriteListActivity";
	final Handler handler_ = new Handler();
	private DatabaseHelper dbHelper_ = null;
	public boolean gridUpdating = false;

	ProgressBar loading_ = null;
	private void setLoading(boolean start) {
		if (loading_ == null) {
			loading_ = (ProgressBar) findViewById(R.id.loading);
			if (loading_ == null) {
				Log.e(TAG, "no loading progressbar.");
				return;
			}
		}
		loading_.setVisibility(start ? ProgressBar.VISIBLE : ProgressBar.INVISIBLE);
	}
	public void startLoading() {
		setLoading(true);
	}
	public void stopLoading() {
		setLoading(false);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (AppUtils.DEBUG) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
					.detectDiskReads().detectDiskWrites().detectAll()
					.penaltyLog().build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
					.detectLeakedSqlLiteObjects()
					.penaltyLog().penaltyDeath().build());
		}
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		
		setContentView(R.layout.favorite_list);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);

		dbHelper_ = new DatabaseHelper(this);

		startLoading();
		
		final String versionName = AppUtils.getVersionName(this);
		final TextView version = (TextView) this.findViewById(R.id.version);
		version.setText(versionName);
		
		setupGridColumns();
		FavoriteNewsCollectTask task = new FavoriteNewsCollectTask(this);
		task.execute();

		parappa_ = new PaRappa(this);
	
		initAdditional();
		AppUtils.onCreateAditional(this);
	}

	protected void initAdditional() {
		
	}

	@Override
	public DatabaseHelper getDbHelper() {
		return dbHelper_;
	}

	@Override
	public int getGridColumnCount() {
		return column_count_;
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setupGridColumns();
	}

	private int column_count_ = 1;
	private void setupGridColumns() {
		final WindowManager w = getWindowManager();
		final Display d = w.getDefaultDisplay();
		final int width = d.getWidth();
		column_count_ = width / 160;
		final ListView list = (ListView) this.findViewById(R.id.favorite_blocks);
		for (int i = 0, len = list.getChildCount(); i < len; i++) {
			final View child = list.getChildAt(i);
			final GridView grid = (GridView) child.findViewById(R.id.grid);
			if (grid != null) {
				grid.setNumColumns(column_count_);
			}
		}
	}

	@Override
	public void resetGridInfo() {
		// TODO Auto-generated method stub
		
	}


}
