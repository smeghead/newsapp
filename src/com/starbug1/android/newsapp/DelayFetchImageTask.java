package com.starbug1.android.newsapp;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.ImageView;

import com.starbug1.android.newsapp.data.NewsListItem;

public class DelayFetchImageTask extends
		AsyncTask<String, Integer, NewsListItem> {
	private final String TAG = "DelayFetchImageTask";

	private static final List<Integer> fetchedImages_ = new LinkedList<Integer>();
	private final AbstractActivity activity_;
	private final NewsListItem item_;

	public DelayFetchImageTask(AbstractActivity activity, NewsListItem item) {
		activity_ = activity;
		item_ = item;
	}

	@Override
	protected NewsListItem doInBackground(String... params) {
		if (fetchedImages_.contains(item_.getId())) {
			// 今回の起動中に既に画層取得が試みられていれば、取得しないで、そのまま返却する。
			Log.d(TAG, "already tried. " + item_.getId());
			return item_;
		}
		fetchedImages_.add(item_.getId());
		activity_.getService().fetchImage(item_);
		if (item_.getImage() == null) {
			Log.i(TAG, "fetch failed.");
		}
		activity_.getDbHelper().insertImage(item_, new Date());
		return item_;
	}

	@Override
	protected void onPostExecute(NewsListItem item) {
		Log.d(TAG, "onPostExecute begin");
		if (item.getImage() != null) {
			Log.d(TAG, "onPostExecute image exists");
			// 画像取得が成功していれば、画像を更新する。
			final GridView grid = (GridView) activity_.findViewById(R.id.grid);
			if (grid != null) {
				Log.d(TAG, "onPostExecute image count:" + grid.getCount());
				for (int i = 0; i < grid.getCount(); i++) {
					// gridの背景を設定する。
					Log.d(TAG, "item:" + grid.getItemAtPosition(i));
					NewsListItem gridItem = (NewsListItem) grid
							.getItemAtPosition(i);
					if (gridItem.getId() == item.getId()) {
						ImageView image = (ImageView) grid.findViewWithTag(item
								.getId());
						if (image != null) {
							final WindowManager w = activity_
									.getWindowManager();
							final Display d = w.getDefaultDisplay();

							final int size = d.getWidth()
									/ activity_.getGridColumnCount();
							Log.d(TAG, "size:" + size);
							// サイズ調整
							final byte[] data = item.getImage();
							Bitmap b = null;
							try {
								b = BitmapFactory.decodeByteArray(data, 0,
										data.length);
							} catch (OutOfMemoryError e) {
								Log.e(TAG, e.getMessage());
							}
							b = Bitmap.createScaledBitmap(b, size, size, false);
							image.setImageBitmap(b);
						}
						break;
					}
				}
			}
		}
		Log.d(TAG, "onPostExecute end");
		activity_.stopLoading();
	}

	@Override
	protected void onPreExecute() {
		activity_.startLoading();
	}

}
