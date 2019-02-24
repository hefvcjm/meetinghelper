package com.meeting.helper.meetinghelper.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.meeting.helper.meetinghelper.R;
import com.meeting.helper.meetinghelper.interfaces.OnFtpProcessListener;
import com.meeting.helper.meetinghelper.utils.MyFtpClient;

import java.util.ArrayList;

public class FtpService extends Service {

    private static final String TAG = "FtpService";
    private static final int UPLOAD_NOTIFICATION_ID = 0x10000;
    private static final int DOWNLOAD_NOTIFICATION_ID = 0x10001;

    private ArrayList<String> uploadList = new ArrayList<>();
    private NotificationManager manager = null;
    private NotificationCompat.Builder builderUpload;
    private Notification notificationUpload;
    private NotificationCompat.Builder builderDownload;
    private Notification notificationDownload;
    private Thread taskThread;
    private int direction = 0;

    private MyFtpClient client;

    @Override
    public void onCreate() {
        super.onCreate();
        init();
        Log.d(TAG, "onStart");
    }

    private void init() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                client = MyFtpClient.getFtpClient();
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        uploadList = intent.getStringArrayListExtra("ftp_list");
        direction = intent.getIntExtra("direction", -1);
        if (direction == -1) {
            return super.onStartCommand(intent, flags, startId);
        }
        Log.d(TAG, direction + "");
        startTask();
        return super.onStartCommand(intent, flags, startId);
    }

    private double uploadProcess = 0;
    private double downloadProcess = 0;

    private void startTask() {
        initNotification();
        if (taskThread != null && taskThread.isAlive()) {
            if (client != null) {
                if (direction == 0) {
                    client.addUploadFiles(uploadList);
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            client.addDownloadFiles(uploadList);
                        }
                    }).start();
                }
            }
            return;
        }
        uploadProcess = 0;
        downloadProcess = 0;
        Log.d(TAG, "new task thread");
        taskThread = new Thread(new Runnable() {
            @Override
            public void run() {
                client = MyFtpClient.getFtpClient();
                if (client != null) {
                    if (direction == 0) {
                        client.addUploadFiles(uploadList);
                    } else {
                        client.addDownloadFiles(uploadList);
                    }
                    client.setOnProcessListener(new OnFtpProcessListener() {
                        @Override
                        public void onProcess(int direction, String nowFile, String size, double process, int total, int success, int failure) {
                            switch (direction) {
                                case 0:
                                    builderUpload = new NotificationCompat.Builder(getApplicationContext());
                                    builderUpload.setSmallIcon(R.drawable.ic_app);
                                    builderUpload.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_app));
                                    builderUpload.setContentTitle("会议录音助手");
                                    builderUpload.setOngoing(false);
                                    builderUpload.setAutoCancel(true);
                                    builderUpload.setChannelId(getApplicationContext().getPackageName());
                                    builderUpload.setProgress(1000, (int) (process * 1000), false);
                                    builderUpload.setContentText("大小共" + size + "，上传已完成" + String.format("%.1f", process * 100) + "%");
                                    builderUpload.setContentInfo(total + "/" + success + "/" + failure);
                                    RemoteViews viewsUpload = new RemoteViews(getPackageName(), R.layout.layout_notification_process);
                                    viewsUpload.setProgressBar(R.id.pb_notification_process, 1000, (int) (process * 1000), false);
                                    viewsUpload.setTextViewText(R.id.tv_notification_content, "大小共" + size + "，上传已完成" + (int) (process * 100) + "%    " + total + "/" + success + "/" + failure);
                                    builderUpload.setContent(viewsUpload);
                                    notificationUpload = builderUpload.build();
//                                    notificationUpload.flags = Notification.FLAG_NO_CLEAR;
                                    manager.notify(UPLOAD_NOTIFICATION_ID, notificationUpload);
                                    if (process * 100 - uploadProcess >= 5) {
                                        Log.d(TAG, "大小共" + size + "，上传已完成" + String.format("%.1f", process * 100) + "%    " + total + "/" + success + "/" + failure);
                                        uploadProcess = process * 100;
                                    }
                                    if (process >= 1.0 && success + failure == total) {
                                        builderUpload.setContentText("大小共" + size + "，上传已完成" + String.format("%.1f", process * 100) + "%");
                                        builderUpload.setContentInfo(total + "/" + success + "/" + failure);
                                        builderUpload.setAutoCancel(true);
                                        RemoteViews views = new RemoteViews(getPackageName(), R.layout.layout_notification_process);
                                        views.setProgressBar(R.id.pb_notification_process, 1000, (int) (process * 1000), false);
                                        views.setTextViewText(R.id.tv_notification_content, "大小共" + size + "，上传已完成" + (int) (process * 100) + "%    " + total + "/" + success + "/" + failure);
                                        builderUpload.setContent(views);
                                        notificationUpload = builderUpload.build();
//                                        notificationUpload.flags = Notification.FLAG_AUTO_CANCEL;
                                        manager.notify(UPLOAD_NOTIFICATION_ID, notificationUpload);
                                        client.clearUpload();
//                                        Looper.prepare();
//                                        Toast.makeText(getApplicationContext(), "上传完成，成功" + success + "个，失败" + failure + "个", Toast.LENGTH_SHORT).show();
//                                        Looper.loop();
                                        stopSelf();
                                    }
                                    break;
                                case 1:
                                    builderDownload = new NotificationCompat.Builder(getApplicationContext());
                                    builderDownload.setSmallIcon(R.drawable.ic_app);
                                    builderDownload.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_app));
                                    builderDownload.setContentTitle("会议录音助手");
                                    builderDownload.setOngoing(false);
                                    builderDownload.setAutoCancel(true);
                                    builderDownload.setChannelId(getApplicationContext().getPackageName());
                                    builderDownload.setProgress(1000, (int) (process * 1000), false);
                                    builderDownload.setContentText("大小共" + size + "，下载已完成" + String.format("%.1f", process * 100) + "% ");
                                    builderDownload.setContentInfo(total + "/" + success + "/" + failure);
                                    RemoteViews viewsDownload = new RemoteViews(getPackageName(), R.layout.layout_notification_process);
                                    viewsDownload.setProgressBar(R.id.pb_notification_process, 1000, (int) (process * 1000), false);
                                    viewsDownload.setTextViewText(R.id.tv_notification_content, "大小共" + size + "，下载已完成" + (int) (process * 100) + "%    " + total + "/" + success + "/" + failure);
                                    builderDownload.setContent(viewsDownload);
                                    notificationDownload = builderDownload.build();
//                                    notificationDownload.flags = Notification.FLAG_NO_CLEAR;
                                    manager.notify(DOWNLOAD_NOTIFICATION_ID, notificationDownload);
                                    if (process * 100 - downloadProcess >= 5) {
                                        Log.d(TAG, "大小共" + size + "，下载已完成" + String.format("%.1f", process * 100) + "%    " + total + "/" + success + "/" + failure);
                                        downloadProcess = process * 100;
                                    }
                                    if (process >= 1.0 && success + failure == total) {
                                        builderDownload.setAutoCancel(true);
                                        RemoteViews views = new RemoteViews(getPackageName(), R.layout.layout_notification_process);
                                        views.setProgressBar(R.id.pb_notification_process, 1000, (int) (process * 1000), false);
                                        views.setTextViewText(R.id.tv_notification_content, "大小共" + size + "，下载已完成" + (int) (process * 100) + "%    " + total + "/" + success + "/" + failure);
                                        builderDownload.setContent(views);
                                        notificationDownload = builderDownload.build();
//                                        notificationDownload.flags = Notification.FLAG_AUTO_CANCEL;
                                        manager.notify(DOWNLOAD_NOTIFICATION_ID, notificationDownload);
                                        client.clearDownload();
//                                        Looper.prepare();
//                                        Toast.makeText(getApplicationContext(), "下载完成，成功" + success + "个，失败" + failure + "个", Toast.LENGTH_SHORT).show();
//                                        Looper.loop();
                                        stopSelf();
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                    client.doTask();
                } else {
//                    builder.setTicker("连接出错");
//                    builder.setContentText("连接出错");
//                    notification = builder.build();
//                    notification.flags = Notification.FLAG_AUTO_CANCEL;
//                    manager.notify(0, notification);
                }
            }
        });
        taskThread.start();

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
        builderUpload.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_app));
        builderUpload.setContentTitle("会议录音助手");
        builderUpload.setOngoing(false);
        builderUpload.setChannelId(getApplicationContext().getPackageName());
        notificationUpload = builderUpload.build();
//        notificationUpload.flags = Notification.FLAG_NO_CLEAR;
        builderUpload.setTicker("正在上传...");
        builderUpload.setContentText("正在上传...");
        manager.notify(UPLOAD_NOTIFICATION_ID, notificationUpload);
    }

    private void initDownloadNotification() {
        initNotificationChannel("download");
        builderDownload = new NotificationCompat.Builder(getApplicationContext());
        builderDownload.setSmallIcon(R.drawable.ic_app);
        builderDownload.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_app));
        builderDownload.setContentTitle("会议录音助手");
        builderDownload.setOngoing(false);
        builderDownload.setChannelId(getApplicationContext().getPackageName());
        notificationDownload = builderDownload.build();
//        notificationDownload.flags = Notification.FLAG_NO_CLEAR;
        builderDownload.setTicker("正在上传...");
        builderDownload.setContentText("正在上传...");
        manager.notify(DOWNLOAD_NOTIFICATION_ID, notificationDownload);
    }

    private void initNotificationChannel(String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager != null) {
                manager.deleteNotificationChannel(getApplicationContext().getPackageName());
            }
            NotificationChannel channel = new NotificationChannel(getApplicationContext().getPackageName(), name, NotificationManager.IMPORTANCE_LOW);
//            NotificationChannel channel = new NotificationChannel("meeting_helper", name,
//                    NotificationManager.IMPORTANCE_DEFAULT);
//            //是否绕过请勿打扰模式
//            channel.canBypassDnd();
            //闪光灯
            channel.enableLights(false);
////            //锁屏显示通知
////            channel.setLockscreenVisibility(VISIBILITY_SECRET);
////            //闪关灯的灯光颜色
////            channel.setLightColor(Color.RED);
//            //桌面launcher的消息角标
//            channel.canShowBadge();
            //是否允许震动
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setVibrationPattern(new long[]{0});
////            //获取系统通知响铃声音的配置
////            channel.getAudioAttributes();
//            //获取通知取到组
//            channel.getGroup();
//            //设置可绕过  请勿打扰模式
//            channel.setBypassDnd(true);
////            //设置震动模式
////            channel.setVibrationPattern(new long[]{100, 100, 200});
////            //是否会有灯光
////            channel.shouldShowLights();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        stopSelf();
    }
}
