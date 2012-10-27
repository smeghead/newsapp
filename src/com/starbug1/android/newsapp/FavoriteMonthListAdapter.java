/**
 * 
 */
package com.starbug1.android.newsapp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import com.starbug1.android.newsapp.data.DatabaseHelper;
import com.starbug1.android.newsapp.data.FavoriteMonth;
import com.starbug1.android.newsapp.data.NewsListItem;

/**
 * @author smeghead
 * 
 */
public class FavoriteMonthListAdapter extends ArrayAdapter<FavoriteMonth> {
	private final LayoutInflater inflater_;
	private final FavoriteListActivity context_;
	private final DatabaseHelper dbHelper_;
	private final SimpleDateFormat monthDateFormat_;

	public FavoriteMonthListAdapter(Context context, DatabaseHelper dbHelper) {
		super(context, 0, new ArrayList<FavoriteMonth>());
		context_ = (FavoriteListActivity) context;
		dbHelper_ = dbHelper;
		inflater_ = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		monthDateFormat_ = new SimpleDateFormat(context.getResources()
				.getString(R.string.month_date_format));
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;

		if (view == null) {
			view = inflater_.inflate(R.layout.favorite_block, null);
		}

		if (this.getCount() < position + 1) {
			Log.w("FavoriteMonthListAdapter", "position invalid!");
			return null;
		}
		final FavoriteMonth month = this.getItem(position);
		if (month != null) {
			view.setTag(month);

			final List<NewsListItem> items = month.getItems();

			final TextView monthLabel = (TextView) view
					.findViewById(R.id.month);
			monthLabel.setText(monthDateFormat_.format(month.getMonth()));

			final NewsListAdapter adapter = new NewsListAdapter(context_,
					dbHelper_);
			for (NewsListItem item : items) {
				adapter.add(item);
			}
			final GridView grid = (GridView) view.findViewById(R.id.grid);
			grid.setNumColumns(context_.getGridColumnCount());
			grid.setAdapter(adapter);
			final int height = (int) (Math.ceil(Double.valueOf(month.getItems()
					.size()) / context_.getGridColumnCount()) * 160 + 40);
			view.setLayoutParams(new ListView.LayoutParams(
					ListView.LayoutParams.MATCH_PARENT, height));

			grid.setOnItemClickListener(new NewsGridEvents.NewsItemClickListener(
					context_, dbHelper_, EntryActivity.class));

			grid.setOnItemLongClickListener(new NewsGridEvents.NewsItemLognClickListener(
					context_, dbHelper_, R.class));

		}
		return view;
	}

}
