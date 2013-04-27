/**
 * 
 */
package com.starbug1.android.newsapp;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.starbug1.android.newsapp.data.DatabaseHelper;
import com.starbug1.android.newsapp.data.NewsListItem;

/**
 * @author smeghead
 * 
 */
public class NewsListAdapter extends ArrayAdapter<NewsListItem> {
	private final String TAG = "NewsListAdapter";
	private final LayoutInflater inflater_;
	private TextView title_;
	private final AbstractActivity context_;
	private final DatabaseHelper dbHelper_;

	public NewsListAdapter(Context context, DatabaseHelper dbHelper) {
		super(context, 0, new ArrayList<NewsListItem>());
		context_ = (AbstractActivity) context;
		dbHelper_ = dbHelper;
		inflater_ = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;

		if (view == null) {
			view = inflater_.inflate(R.layout.item_row, null);
		}

		if (this.getCount() < position + 1) {
			Log.w(TAG, "position invalid!");
			return null;
		}
		final NewsListItem item = this.getItem(position);
		if (item != null) {
			view.setTag(item);

			final String title = item.getTitle().toString();
			title_ = (TextView) view.findViewById(R.id.item_title);
			title_.setText(title);
			final ImageView newEntry = (ImageView) view
					.findViewById(R.id.newEntry);
			newEntry.setVisibility(item.getViewCount() > 0 ? ImageView.GONE
					: ImageView.VISIBLE);
			title_.setTextColor(Color.argb(item.getViewCount() > 0 ? 168 : 230,
					255, 255, 255));
			final ImageView isFavorite = (ImageView) view
					.findViewById(R.id.favorite);
			isFavorite
					.setImageResource(item.isFavorite() ? android.R.drawable.btn_star_big_on
							: android.R.drawable.btn_star_big_off);
			isFavorite.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					final boolean add = !item.isFavorite();
					// お気に入り
					dbHelper_.favorite(context_, item, v, add);
				}
			});
			final WindowManager w = context_.getWindowManager();
			final Display d = w.getDefaultDisplay();

			final int size = d.getWidth() / context_.getGridColumnCount();
			Log.d(TAG, "size:" + size);
			if (item.getImage() != null) {
				Bitmap b = item.getImageBitmap();
				if (b == null) {
					final byte[] data = item.getImage();
					try {
						b = BitmapFactory.decodeByteArray(data, 0, data.length);
					} catch (OutOfMemoryError e) {
						Log.e("NewsListAdapter", e.getMessage());
					}
				}

				// サイズ調整
				b = Bitmap.createScaledBitmap(b, size, size, false);

				final ImageView image = (ImageView) view
						.findViewById(R.id.item_image);
				image.setImageDrawable(null);
				image.setImageBitmap(b);
				image.setVisibility(ImageView.VISIBLE);
			} else {
				final ImageView image = (ImageView) view
						.findViewById(R.id.item_image);
				Bitmap b = BitmapFactory.decodeResource(
						context_.getResources(), R.drawable.no_image);
				b = Bitmap.createScaledBitmap(b, size, size, false);
				image.setImageBitmap(b);
				image.setVisibility(ImageView.VISIBLE);
				image.setTag(item.getId()); // DelayFetchImageTaskで検索するためにTagを設定する。

				// backgroundで画像取得処理を起動する。
				Log.d(TAG, "delay fetch image task " + item.getId());
				DelayFetchImageTask fetchTask = new DelayFetchImageTask(
						context_, item);
				fetchTask.execute();
			}
		}
		return view;
	}

	@Override
	public void remove(NewsListItem object) {
		Log.d(TAG, "remove");
		super.remove(object);
	}

}
