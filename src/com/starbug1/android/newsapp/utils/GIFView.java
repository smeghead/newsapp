package com.starbug1.android.newsapp.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class GIFView extends View {
	private static final String TAG = "GIFView";

	private Movie movie;
	private long moviestart;

	public GIFView(Context context) {
		super(context);
	}

	public GIFView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public GIFView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setResouceId(int id) {
		InputStream inputStream = getContext().getResources().openRawResource(id);
		movie = Movie.decodeStream(inputStream);
		moviestart = 0;
	}

	public void setImagePath(String path) {
		try {
			File f = new File(path);
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
			bis.mark((int) f.length());
			movie = Movie.decodeStream(bis);
			bis.close();
			moviestart = 0;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (movie == null) {
			return;
		}
		long now = android.os.SystemClock.uptimeMillis();
		if (moviestart == 0) {
			moviestart = now;
		}
		int relTime = (int) ((now - moviestart) % movie.duration());
		movie.setTime(relTime);
		movie.draw(canvas, 2, 2);
		this.invalidate();
	}
}