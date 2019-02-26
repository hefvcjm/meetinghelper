package com.meeting.helper.meetinghelper.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class FileInfo{

    private static final String TAG = "FileInfo";

    private String fileName;
    private long fileSize;
    private long fileTime;
    private String filePath;
    private boolean isSelected = false;

    public FileInfo() {

    }

    public FileInfo(String fileName, long fileSize, long fileTime, String filePath) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileTime = fileTime;
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getFileTime() {
        return fileTime;
    }

    public void setFileTime(long fileTime) {
        this.fileTime = fileTime;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
