package com.meeting.helper.meetinghelper.interfaces;

public interface OnFtpProcessListener {

    /**
     * ftp处理过程
     *
     * @param direction 处理方向，0：上传；1：下载
     * @param nowFile   目前处理的文件
     * @param size      文件总大小
     * @param process   当前文件处理进度
     * @param total     总共处理文件数
     * @param success   成功处理个数
     * @param failure   处理失败个数
     */
    void onProcess(int direction, String nowFile, String size, double process, int total, int success, int failure);
}