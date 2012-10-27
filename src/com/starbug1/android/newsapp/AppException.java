package com.starbug1.android.newsapp;

public class AppException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AppException() {
		super();
	}

	public AppException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public AppException(String detailMessage) {
		super(detailMessage);
	}

	public AppException(Throwable throwable) {
		super(throwable);
	}

}
