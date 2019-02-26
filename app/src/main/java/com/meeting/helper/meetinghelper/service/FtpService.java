package com.meeting.helper.meetinghelper.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.meeting.helper.meetinghelper.R;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;
import com.meeting.helper.meetinghelper.ftp.FtpWorker;
import com.meeting.helper.meetinghelper.ftp.OnFtpProcessListener;
import com.meeting.helper.meetinghelper.ftp.OnTaskStatusChangedListener;
import com.meeting.helper.meetinghelper.ftp.task.DownloadTask;
import com.meeting.helper.meetinghelper.ftp.task.FtpTask;
import com.meeting.helper.meetinghelper.ftp.task.UploadTask;
import com.meeting.helper.meetinghelper.utils.FileUtils;

import java.util.ArrayList;
import java.util.UUID;

public class FtpService extends Service {

    private static final String TAG = "FtpService";
    private static final int UPLOAD_NOTIFICATION_ID = 0x10000;
    private static final int DOWNLOAD_NOTIFICATION_ID = 0x10001;
    private static final String BASE_PATH = "/storage/emulated/0/meetinghelper/records";

    private ArrayList<String> uploadList = new ArrayList<>();
    private long[] fileSize;
    private NotificationManager manager = null;
    private NotificationCompat.Builder builderUpload;
    private Notification notificationUpload;
    private NotificationCompat.Builder builderDownload;
    private Notification notificationDownload;
    private int direction = 0;
    private FtpWorker ftpWorker;

    private long totalSize = 0;
    private int total = 0;
    private int success = 0;
    private int failure = 0;
    private long remainSize = 0;
    private long remainSizeTemp = 0;
    private String nowFile = "";

    private OnTaskStatusChangedListener onTaskStatusChangedListener = new OnTaskStatusChangedListener() {
        @Override
        public void onStatusChanged(FtpTask ftpTask, FtpTaskStatus status, Object object) {
            switch (status) {
                case WAITING:
                    break;
                case FINISHED:
                    success++;
                    onFtpProcessListener.onProcess(direction, UUID.randomUUID().toString(), -1, -1);
                    break;
                case EXECUTING:
                    break;
                case STOPPED:
                case DISCONNECTED:
                case EXEC_TIMEOUT:
                case WAIT_TIMEOUT:
                case EXCEPTION:
                    failure++;
                    if (direction == 0) {
                        onFtpProcessListener.onProcess(direction, UUID.randomUUID().toString(), ((UploadTask) ftpTask).getFileSize(), -1);
                    } else {
                        onFtpProcessListener.onProcess(direction, UUID.randomUUID().toString(), ((DownloadTask) ftpTask).getFileSize(), -1);
                    }
                    break;
                case BLOCK:
                default:
                    break;
            }
        }
    };

    private OnFtpProcessListener onFtpProcessListener = new OnFtpProcessListener() {

        @Override
        public void onProcess(int direction, String file, long size, long process) {
            if (process == -1)
                Log.d("hefvcjm", direction + "    " + file + "    " + size + "    " + process + "    " + success + "    " + failure);
            if (size != -1 && process != -1) {
                if (nowFile != file) {
                    nowFile = file;
                    remainSizeTemp -= size;
                }
                remainSize = remainSizeTemp + size - process;
            }
            if (size != -1 && process == -1) {
                remainSize = remainSizeTemp;
            }
            long processSize = totalSize - remainSize;
            if (direction == 0) {
                RemoteViews viewsUpload = new RemoteViews(getPackageName(), R.layout.layout_notification_process);
                viewsUpload.setProgressBar(R.id.pb_notification_process, 1000, (int) (processSize * 1.0 / totalSize * 1000), false);
                viewsUpload.setTextViewText(R.id.tv_notification_content, "大小共" + FileUtils.getFileSize(totalSize) + "，上传已完成" + (int) (processSize * 1.0 / totalSize * 100) + "%    " + total + "/" + success + "/" + failure);
                builderUpload.setContent(viewsUpload);
                notificationUpload = builderUpload.build();
                manager.notify(UPLOAD_NOTIFICATION_ID, notificationUpload);
            } else {
                RemoteViews viewsUpload = new RemoteViews(getPackageName(), R.layout.layout_notification_process);
                viewsUpload.setProgressBar(R.id.pb_notification_process, 1000, (int) (processSize * 1.0 / totalSize * 1000), false);
                viewsUpload.setTextViewText(R.id.tv_notification_content, "大小共" + FileUtils.getFileSize(totalSize) + "，下载已完成" + (int) (processSize * 1.0 / totalSize * 100) + "%    " + total + "/" + success + "/" + failure);
                builderDownload.setContent(viewsUpload);
                notificationDownload = builderDownload.build();
                manager.notify(DOWNLOAD_NOTIFICATION_ID, notificationDownload);
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ftpWorker = FtpWorker.getInstance();
        uploadList = intent.getStringArrayListExtra("ftp_list");
        direction = intent.getIntExtra("direction", -1);
        fileSize = intent.getLongArrayExtra("files_size");
        if (direction == -1) {
            return super.onStartCommand(intent, flags, startId);
        }
        Log.d(TAG, direction + "");
        if (ftpWorker.countTask() == 0) {
            totalSize = 0;
            total = 0;
            success = 0;
            failure = 0;
            remainSize = 0;
            remainSizeTemp = 0;
            String nowFile = "";
        }
        initNotification();
        startTask();
        return super.onStartCommand(intent, flags, startId);
    }

    private void startTask() {
        if (direction == 0) {
            for (String item : uploadList) {
                UploadTask task = new UploadTask(ftpWorker.getFtpClient(), item, onFtpProcessListener);
                task.setOnTaskStatusChangedListener(onTaskStatusChangedListener);
                ftpWorker.addTask(task);
            }
        } else {
            int i = 0;
            for (String item : uploadList) {
                Log.d(TAG, item);
                Log.d(TAG, fileSize[i] + "");
                Log.d(TAG, BASE_PATH + "/" + item);
                DownloadTask task = new DownloadTask(ftpWorker.getFtpClient(), item, fileSize[i], BASE_PATH, onFtpProcessListener);
                task.setOnTaskStatusChangedListener(onTaskStatusChangedListener);
                ftpWorker.addTask(task);
                i++;
            }
        }
        for (long i : fileSize) {
            totalSize += i;
            remainSize += i;
            remainSizeTemp += i;
        }
        total = uploadList.size();
    }

    private void initNotification() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        if (direction == 0) {
            initUploadNotification();
        } else {
            initDownloadNotification();
        }
    }

    private void initUploadNotification() {
        initNotificationChannel("upload");
        builderUpload = new NotificationCompat.Builder(getApplicationContext());
        builderUpload.setSmallIcon(R.drawable.ic_app);
        builderUpload.setOngoing(false);
        builderUpload.setAutoCancel(true);
        builderUpload.setChannelId(getApplicationContext().getPackageName());
        notificationUpload = builderUpload.build();
        manager.notify(UPLOAD_NOTIFICATION_ID, notificationUpload);
    }

    private void initDownloadNotification() {
        initNotificationChannel("download");
        builderDownload = new NotificationCompat.Builder(getApplicationContext());
        builderDownload.setSmallIcon(R.drawable.ic_app);
        builderDownload.setOngoing(false);
        builderDownload.setAutoCancel(true);
        builderDownload.setChannelId(getApplicationContext().getPackageName());
        notificationDownload = builderDownload.build();
        manager.notify(DOWNLOAD_NOTIFICATION_ID, notificationDownload);
    }

    private void initNotificationChannel(String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager != null) {
                manager.deleteNotificationChannel(getApplicationContext().getPackageName());
            }
            NotificationChannel channel = new NotificationChannel(getApplicationContext().getPackageName(), name, NotificationManager.IMPORTANCE_LOW);
            channel.enableLights(false);
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setVibrationPattern(new long[]{0});
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "set notification channel");
            } else {
                manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                manager.createNotificationChannel(channel);
                Log.d(TAG, "set notification channel");
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
