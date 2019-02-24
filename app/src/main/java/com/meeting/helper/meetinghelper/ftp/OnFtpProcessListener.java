package com.meeting.helper.meetinghelper.ftp;

public interface OnFtpProcessListener {

    /**
     * ftp处理过程
     *
     * @param direction 处理方向，0：上传；1：下载
     * @param file      目前处理的文件
     * @param size      文件总大小
     * @param process   当前文件处理进度
     */
    void onProcess(int direction, String file, long size, long process);
}