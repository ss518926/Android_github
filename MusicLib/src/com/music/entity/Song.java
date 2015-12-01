package com.music.entity;

public class Song {
	private String sid = "-1";
	private String displayName;
	private String trackName;
	private String artisName;
	private String album;
	private int duration;
	private int size;
	private String hashValue;
	private String m4aUrl;
	private String filePath;
	private String parentPath;
	private String mine_type;

	public Song() {

	}

	public Song(NetSong netsong) {
		this.sid = netsong.getSongId();
		this.displayName = netsong.getSongName();
		this.album = netsong.getAlbumName();
		this.trackName = netsong.getUserName();
		this.artisName = netsong.getUserName();
		this.m4aUrl = netsong.getSongUrl();
	}

	public String getSid() {
		return sid;
	}

	public void setSid(String sid) {
		this.sid = sid;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String getTrackName() {
		return trackName;
	}

	public void setTrackName(String trackName) {
		this.trackName = trackName;
	}

	public String getArtisName() {
		return artisName;
	}

	public void setArtisName(String artisName) {
		this.artisName = artisName;
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getHashValue() {
		return hashValue;
	}

	public void setHashValue(String hashValue) {
		this.hashValue = hashValue;
	}

	public String getM4aUrl() {
		return m4aUrl;
	}

	public void setM4aUrl(String m4aUrl) {
		this.m4aUrl = m4aUrl;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getParentPath() {
		return parentPath;
	}

	public void setParentPath(String parentPath) {
		this.parentPath = parentPath;
	}

	public String getMine_type() {
		return mine_type;
	}

	public void setMine_type(String mine_type) {
		this.mine_type = mine_type;
	}
	
	public boolean equals(Song song) {
		if (this.filePath != null) {
			if (filePath.equals(song.getFilePath()))
				return true;
		} else if (!this.sid.equals("-1")) {
			if (sid.equals(song.getSid()))
				return true;
		}
		return false;
	}

}
