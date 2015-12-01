package com.music.entity;

public class PathItem {
	String folderName;
	String filePath;
	int count;

	public PathItem() {
	}

	public PathItem(String filePath,String folderName, int count) {
		this.folderName = folderName;
		this.filePath = filePath;
		this.count = count;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String path) {
		this.filePath = path;
	}

	public String getFolderName() {
		return folderName;
	}

	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

}
