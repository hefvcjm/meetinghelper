package com.meeting.helper.meetinghelper.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class FileInfo implements Parcelable {

    private static final String TAG = "FileInfo";

    private String fileName;
    private String fileSize;
    private String fileTime;
    private String filePath;
    private boolean isSelected = false;

    public FileInfo() {

    }

    public FileInfo(String fileName, String fileSize, String fileTime, String filePath) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileTime = fileTime;
        this.filePath = filePath;
    }

    protected FileInfo(Parcel in) {
        fileName = in.readString();
        fileSize = in.readString();
        fileTime = in.readString();
        filePath = in.readString();
        isSelected = in.readByte() != 0;
    }

    public static final Creator<FileInfo> CREATOR = new Creator<FileInfo>() {
        @Override
        public FileInfo createFromParcel(Parcel in) {
            return new FileInfo(in);
        }

        @Override
        public FileInfo[] newArray(int size) {
            Log.d(TAG, size + "");
            return new FileInfo[size];
        }
    };

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileTime() {
        return fileTime;
    }

    public void setFileTime(String fileTime) {
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fileName);
        dest.writeString(fileSize);
        dest.writeString(fileTime);
        dest.writeString(filePath);
        dest.writeByte((byte) (isSelected ? 1 : 0));
    }
}
