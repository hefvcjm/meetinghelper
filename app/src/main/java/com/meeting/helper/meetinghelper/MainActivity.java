package com.meeting.helper.meetinghelper;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpWorker;
import com.meeting.helper.meetinghelper.ftp.task.DownloadTask;

public class MainActivity extends AppCompatActivity {

    FtpWorker ftpWorker;
    FtpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ftpWorker = FtpWorker.getInstance();
        client = FtpClient.getInstance();
        DownloadTask task = new DownloadTask(client
                , "20190218_232027_未命名.wav"
                , 1
                , "/storage/emulated/0/meetinghelper/temp/20190218_232027_未命名.wav"
                , null);
        ftpWorker.addTask(task);
    }

    public void addTask(View v) {
        String name = "20190218_232038_测试联系.wav";
        ftpWorker.addTask(new DownloadTask(client
                , "20190218_232038_测试联系.wav"
                , 1
                , "/storage/emulated/0/meetinghelper/temp/20190218_232038_测试联系.wav"
                , null));
    }
}
