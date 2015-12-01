package com.music;

public class MusicClient {

	private static MusicClient mClient;
	private String user;

	public static MusicClient getInstance() {
		if (mClient == null)
			mClient = new MusicClient();
		return mClient;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

}
