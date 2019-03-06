package com.meeting.helper.audio;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.aip.asrwakeup3.core.mini.AutoCheck;
import com.baidu.aip.asrwakeup3.core.recog.IStatus;
import com.baidu.aip.asrwakeup3.core.recog.MyRecognizer;
import com.baidu.aip.asrwakeup3.core.recog.listener.IRecogListener;
import com.baidu.aip.asrwakeup3.core.recog.listener.MessageStatusRecogListener;
import com.baidu.aip.asrwakeup3.uiasr.params.OfflineRecogParams;
import com.github.promeg.pinyinhelper.Pinyin;
import com.meeting.helper.audiotool.Config;
import com.meeting.helper.audiotool.player.AudioPlayer;
import com.meeting.helper.audiotool.player.OnPlayerStatusChangedListener;
import com.meeting.helper.audiotool.player.PlayerStatus;
import com.meeting.helper.audiotool.recorder.AudioRecorder;
import com.meeting.helper.audiotool.recorder.OnRecorderStatusChangedListener;
import com.meeting.helper.audiotool.recorder.RecorderStatus;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class AudioActivity extends AppCompatActivity implements IStatus {

    private static final String TAG = "AudioActivity";
    private static final int PERMISSION_REQUEST_CODE = 0;
    private static final int AUDIOPLAYER_STOPPED_CODE = 1;

    private enum NowRecorder {
        NO_RUNNING,//未录音
        BAIDU,//百度语音录音器
        SELF;//程序自身
    }

    private static String tempPcmPatchPath;
    private static String tempPcmMergePath;
    private static final String pcmMergeName = "merge.pcm";

    private int color = R.color.colorPrimary;
    private boolean keepDisplayOn = true;

    private Timer timer;
    private MenuItem saveMenuItem;
    private int recorderSecondsElapsed;
    private int playerSecondsElapsed;

    private RelativeLayout contentLayout;
    private TextView statusView;
    private TextView timerView;
    private ImageView restartView;
    private TextView tvRestart;
    private ImageView recordView;
    private TextView tvRecord;
    private ImageView finishView;
    private TextView tvFinish;
    private ImageView restartAtPlayView;
    private TextView tvRestartAtPlay;
    private ImageView playView;
    private TextView tvPlay;
    private LinearLayout llRecord;
    private LinearLayout llPlay;
    private TextView tvRecogResult;

    private boolean isBaiduRecording = false;
    private boolean isGetFileName = false;
    private String filename = "";
    private String location = "";
    private String meeting = "";
    private String recognizeName = "未命名";

    private AudioRecorder recorder;
    private AudioPlayer player;

    private int reTryTimeout = 10;
    private Timer reTryTimer;

    private OnRecorderStatusChangedListener onRecorderStatusChangedListener = new OnRecorderStatusChangedListener() {
        @Override
        public void onRecorderStatusChanged(RecorderStatus status) {
            switch (status) {
                case RECORDING:
                    if (reTryTimer != null) {
                        reTryTimer.cancel();
                        reTryTimer.purge();
                        reTryTimer = null;
                    }
                    break;
                case PAUSED:
                    break;
                case STOPPED:
                    break;
                case OCCUPIED:
                    Log.d(TAG, "onRecorderStatusChanged OCCUPIED");
                    if (reTryTimer == null) {
                        reTryTimer = new Timer();
                        reTryTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                recorder.setStatus(RecorderStatus.EXCEPTION);
                            }
                        }, reTryTimeout * 1000);
                    }
                    recorder.resume();
                    break;
                case RELEASED:
                case EXCEPTION:
                case INITIALIZED:
                case NULL_RECORDER:
                case UNINITIALIZED:
                    if (reTryTimer != null) {
                        reTryTimer.cancel();
                        reTryTimer.purge();
                        reTryTimer = null;
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private OnPlayerStatusChangedListener onPlayerStatusChangedListener = new OnPlayerStatusChangedListener() {
        @Override
        public void onPlayerStatusChanged(PlayerStatus status) {
            switch (status) {
                case PLAYING:
                    break;
                case NULL_RECORDER:
                case EXCEPTION:
                case RELEASED:
                    Message msg = new Message();
                    msg.what = AUDIOPLAYER_STOPPED_CODE;
                    handler.sendMessage(msg);
                    break;
                case STOPPED:
                case PAUSED:
                case UNINITIALIZED:
                case INITIALIZED:
                    break;
                default:
                    break;
            }
        }
    };

    private NowRecorder nowRecorder = NowRecorder.NO_RUNNING;

    private ArrayList<String> patchFileList = new ArrayList<>();

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_TIME:
                    timerView.setText(Util.formatSeconds((Integer) msg.obj));
                    break;
                case AUDIOPLAYER_STOPPED_CODE:
                    stopPlaying();
                    break;
                default:
                    break;
            }
            handleMsg(msg);
        }
    };


    //百度语音识别

    /**
     * 识别控制器，使用MyRecognizer控制识别的流程
     */
    protected MyRecognizer myRecognizer;

    /**
     * 百度语音识别状态
     */
    protected int status = STATUS_NONE;

    /*
     * 本Activity中是否需要调用离线命令词功能。根据此参数，判断是否需要调用SDK的ASR_KWS_LOAD_ENGINE事件
     */
    protected boolean enableOffline = true;

    private static final int MSG_UPDATE_TIME = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_record);
        init(savedInstanceState);
    }

    private void init(Bundle savedInstanceState) {
        tempPcmPatchPath = getCacheDir().getPath() + "/pcm/patch";//录音pcm文件碎片缓存目录
        tempPcmMergePath = getCacheDir().getPath() + "/pcm/merge";//录音pcm文件合并后缓存目录
        isGetFileName = false;
        File file = new File(tempPcmPatchPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(tempPcmMergePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        if (keepDisplayOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        color = R.color.colorPrimary;

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setElevation(0);
            getSupportActionBar().setBackgroundDrawable(
                    new ColorDrawable(getResources().getColor(color)));
            getSupportActionBar().setHomeAsUpIndicator(
                    ContextCompat.getDrawable(this, R.drawable.aar_ic_clear));
        }

        contentLayout = findViewById(R.id.content);
        statusView = findViewById(R.id.status);
        timerView = findViewById(R.id.timer);
        restartView = findViewById(R.id.iv_record_restart);
        tvRestart = findViewById(R.id.tv_record_restart);
        recordView = findViewById(R.id.iv_record_start);
        tvRecord = findViewById(R.id.tv_record_start);
        finishView = findViewById(R.id.iv_record_stop);
        tvFinish = findViewById(R.id.tv_record_stop);
        restartAtPlayView = findViewById(R.id.iv_record_restart_at_play_view);
        tvRestartAtPlay = findViewById(R.id.tv_record_restart_at_play_view);
        playView = findViewById(R.id.iv_audio_play);
        tvPlay = findViewById(R.id.tv_audio_play);
        llRecord = findViewById(R.id.ll_record);
        llPlay = findViewById(R.id.ll_play);
        tvRecogResult = findViewById(R.id.tv_recognized_result);

        finishView.setImageResource(R.drawable.ic_record_finished_fade);
        finishView.setClickable(false);
        restartView.setImageResource(R.drawable.ic_record_restart_fade);
        restartView.setClickable(false);
        contentLayout.setBackgroundColor(getResources().getColor(color));
        llPlay.setVisibility(View.GONE);
        llRecord.setVisibility(View.VISIBLE);

        if (Util.isBrightColor(color)) {
            ContextCompat.getDrawable(this, R.drawable.aar_ic_clear)
                    .setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
            ContextCompat.getDrawable(this, R.drawable.aar_ic_check)
                    .setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
            statusView.setTextColor(Color.BLACK);
            timerView.setTextColor(Color.BLACK);
            restartView.setColorFilter(Color.BLACK);
            recordView.setColorFilter(Color.BLACK);
            finishView.setColorFilter(Color.BLACK);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.INTERNET);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            }
            if (permissions.size() != 0) {
                String[] arrayPermissions = new String[permissions.size()];
                permissions.toArray(arrayPermissions);
                ActivityCompat.requestPermissions(AudioActivity.this, arrayPermissions, PERMISSION_REQUEST_CODE);
            }
        }
        recorder = AudioRecorder.getInstance();
        recorder.setOnRecorderStatusChangedListener(onRecorderStatusChangedListener);
        initRecognizer();
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopPlaying();
        if (!hasFinishRecord) {
            finishRecording(null);
        }
        setResult(RESULT_CANCELED);
        recorder.release();
        if (player != null) {
            player.release();
        }
        recognizerRelease();
        Log.i(TAG, "onDestory");
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.aar_audio_recorder, menu);
        saveMenuItem = menu.findItem(R.id.action_save);
        saveMenuItem.setIcon(ContextCompat.getDrawable(this, R.drawable.aar_ic_check));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            if (!hasFinishRecord) {
                finishRecording(null);
            }
            stopPlaying();
            setResult(RESULT_CANCELED);
            finish();
        } else if (i == R.id.action_save) {
            if (!hasFinishRecord) {
                finishRecording(null);
            }
            if (player != null) {
                stopPlaying();
            }
            selectAudio();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(AudioActivity.this, "用户禁止了应用相关权限，无法运行应用", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }


    private void selectAudio() {
        if (!hasFinishRecord) {
            finishRecording(null);
        }
        stopTimer();
        if (player != null) {
            player.release();
        }
        if (filename == null) {
            setResult(RESULT_CANCELED);
            finish();
        } else {
            recognizeName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date(new File(filename).lastModified())) + "_" + recognizeName + ".wav";
            Intent intent = new Intent();
            intent.putExtra("filePath", filename);
            intent.putExtra("recognize_result", recognizeName);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void initRecognizer() {
        if (myRecognizer != null) {
            myRecognizer.release();
        }
        // 基于DEMO集成第1.1, 1.2, 1.3 步骤 初始化EventManager类并注册自定义输出事件
        // DEMO集成步骤 1.2 新建一个回调类，识别引擎会回调这个类告知重要状态和识别结果
        IRecogListener listener = new MessageStatusRecogListener(handler);
        // DEMO集成步骤 1.1 1.3 初始化：new一个IRecogListener示例 & new 一个 MyRecognizer 示例,并注册输出事件
        myRecognizer = new MyRecognizer(AudioActivity.this, listener);
        if (enableOffline && myRecognizer != null) {
            // 基于DEMO集成1.4 加载离线资源步骤(离线时使用)。offlineParams是固定值，复制到您的代码里即可
            Map<String, Object> offlineParams = OfflineRecogParams.fetchOfflineParams();
            myRecognizer.loadOfflineEngine(offlineParams);
        }
    }

    public void recognizerRelease() {
        if (myRecognizer != null) {
            myRecognizer.release();
        }
    }

    private String addAndGetPatchName(boolean isClear) {
        if (isClear) {
            patchFileList.clear();
        }
        String str = patchFileList.size() + ".pcm";
        Log.d(TAG, "recorder path: " + str);
        patchFileList.add(str);
        return str;
    }

    public void toggleRecording(View v) {
        hasStarted = true;
        finishView.setImageResource(R.drawable.ic_record_finished);
        finishView.setClickable(true);
        restartView.setImageResource(R.drawable.ic_record_restart);
        restartView.setClickable(true);
        Util.wait(100, new Runnable() {
            @Override
            public void run() {
                if (recorder.getStatus() == RecorderStatus.RECORDING || isBaiduRecording) {
                    pauseRecording();
                } else {
                    resumeRecording();
                }
            }
        });
    }

    public void togglePlaying(View v) {
        Util.wait(100, new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                if (player != null && player.getStatus() == PlayerStatus.PLAYING) {
                    stopPlaying();
                } else {
                    startPlaying();
                }
            }
        });
    }

    public void restartRecording(View v) {
        meeting = "";
        location = "";
        tvRecogResult.setText("");
        stopTimer();
        hasFinishRecord = false;
        if (player != null) {
            player.release();
        }
        if (recorder.getStatus() == RecorderStatus.RECORDING) {
            recorder.stop();
            recorder.release();
        }
        if (isBaiduRecording) {
            stop();
            cancel();
            recognizerRelease();
        }
        isGetFileName = false;
        recognizeName = "未命名";
        nowRecorder = NowRecorder.NO_RUNNING;
        patchFileList.clear();
        FileUtils.deleteFiles(tempPcmPatchPath);
        FileUtils.deleteFiles(tempPcmMergePath);
        resumeRecording();
    }

    private void resumeRecording() {
        Log.d(TAG, "resume recorder, now recorder is " + nowRecorder);
        if (player != null) {
            player.release();
        }
        if (nowRecorder == NowRecorder.NO_RUNNING) {
            if (isGetFileName) {
                if (patchFileList.size() == 0) {
                    recorder.setFilePath(tempPcmPatchPath + "/" + addAndGetPatchName(true));
                }
                recorder.resume();
                nowRecorder = NowRecorder.SELF;
            } else {
                initRecognizer();
                start(tempPcmPatchPath + "/" + addAndGetPatchName(false));
                nowRecorder = NowRecorder.BAIDU;
            }
        } else if (nowRecorder == NowRecorder.BAIDU) {
            initRecognizer();
            start(tempPcmPatchPath + "/" + addAndGetPatchName(false));
        } else if (nowRecorder == NowRecorder.SELF) {
            if (patchFileList.size() == 0) {
                recorder.setFilePath(tempPcmPatchPath + "/" + addAndGetPatchName(true));
            }
            recorder.resume();
        }
        if (timer == null) {
            startTimer();
        }
        if (nowRecorder == NowRecorder.SELF) {
            Log.d(TAG, "now recorder path: " + recorder.getFilePath());
        }
        Log.d(TAG, "patchFileList=" + patchFileList.size());
        saveMenuItem.setVisible(false);
        playView.setImageResource(R.drawable.ic_play_start);
        tvPlay.setText("播放录音");
        llPlay.setVisibility(View.GONE);
        llRecord.setVisibility(View.VISIBLE);
        statusView.setText("录音中...");
        recordView.setImageResource(R.drawable.ic_audio_paused);
        tvRecord.setText("暂停录音");
    }

    private void pauseRecording() {
        tvRecogResult.setText("");
        if (!isFinishing()) {
            saveMenuItem.setVisible(true);
        }
        if (nowRecorder == NowRecorder.BAIDU) {
            stop();
            cancel();
            recognizerRelease();
        }
        if (nowRecorder == NowRecorder.SELF) {
            recorder.pause();
        }
        statusView.setText("暂停");
        llPlay.setVisibility(View.GONE);
        llRecord.setVisibility(View.VISIBLE);
        recordView.setImageResource(R.drawable.ic_record_start);
        tvRecord.setText("继续录音");
        playView.setImageResource(R.drawable.ic_play_start);
        tvPlay.setText("播放录音");
    }

    private boolean hasFinishRecord = false;

    public void finishRecording(View v) {
        meeting = "";
        location = "";
        tvRecogResult.setText("");
        hasFinishRecord = true;
        if (!isFinishing()) {
            saveMenuItem.setVisible(true);
        }
        if (nowRecorder == NowRecorder.BAIDU) {
            stop();
            cancel();
            recognizerRelease();
        }
        if (player != null && player.getStatus() == PlayerStatus.PLAYING) {
            Log.d(TAG, "finishRecording: stop player");
            player.release();
        }
        recorder.stop();
        recorder.release();
        stopTimer();
        nowRecorder = NowRecorder.NO_RUNNING;
        saveMenuItem.setVisible(true);
        statusView.setText("");
        llRecord.setVisibility(View.GONE);
        llPlay.setVisibility(View.VISIBLE);
        recordView.setImageResource(R.drawable.ic_record_start);
        tvRecord.setText("开始录音");
        playView.setImageResource(R.drawable.ic_play_start);
        tvPlay.setText("播放录音");
        playView.setEnabled(false);
        filename = Util.mergeAndSavePcmFiles(tempPcmPatchPath, tempPcmMergePath + "/" + pcmMergeName, patchFileList);
        FileUtils.deleteFiles(tempPcmPatchPath);
        patchFileList.clear();
        isGetFileName = false;
        Log.d(TAG, "tempPcmPatchPath: " + tempPcmPatchPath);
        Log.d(TAG, "tempPcmMergePath: " + tempPcmMergePath + "/" + pcmMergeName);
        Log.d(TAG, "player path: " + filename);
        playView.setEnabled(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startPlaying() {
        try {
            if (!hasFinishRecord) {
                finishRecording(null);
            }
            startTimer();
            player = new AudioPlayer(new Config().setFilePath(filename));
            player.setOnPlayerStatusChangedListener(onPlayerStatusChangedListener);
            player.start();
            statusView.setText("播放中...");
            llRecord.setVisibility(View.GONE);
            llPlay.setVisibility(View.VISIBLE);
            recordView.setImageResource(R.drawable.ic_record_start);
            tvRecord.setText("开始录音");
            playView.setImageResource(R.drawable.ic_audio_paused);
            tvPlay.setText("暂停播放");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopPlaying() {
        if (recorder.getStatus() == RecorderStatus.RECORDING || isBaiduRecording) {
            return;
        }
        stopTimer();
        if (player != null) {
            player.stop();
        }
        statusView.setText("停止");
        llRecord.setVisibility(View.GONE);
        llPlay.setVisibility(View.VISIBLE);
        recordView.setImageResource(R.drawable.ic_record_start);
        tvRecord.setText("开始录音");
        playView.setImageResource(R.drawable.ic_play_start);
        tvPlay.setText("播放录音");
    }

    private void startTimer() {
        stopTimer();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateTimer();
            }
        }, 0, 1000);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
        recorderSecondsElapsed = 0;
        playerSecondsElapsed = 0;
    }

    private void updateTimer() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "timer: recorder status [" + recorder.getStatus() + "];isBaiduRecording [" + isBaiduRecording + "]");
                if (player != null) {
                    Log.d(TAG, "timer: player status [" + player.getStatus() + "]");
                }
                if (recorder.getStatus() == RecorderStatus.RECORDING || isBaiduRecording) {
                    recorderSecondsElapsed++;
                    Message msg = new Message();
                    msg.what = MSG_UPDATE_TIME;
                    msg.obj = recorderSecondsElapsed;
                    handler.sendMessage(msg);
                } else if (player != null && player.getStatus() == PlayerStatus.PLAYING) {
                    playerSecondsElapsed++;
                    Message msg = new Message();
                    msg.what = MSG_UPDATE_TIME;
                    msg.obj = playerSecondsElapsed;
                    handler.sendMessage(msg);
                }
            }
        });
    }


    /**
     * 开始录音，点击“开始”按钮后调用。
     * 基于DEMO集成2.1, 2.2 设置识别参数并发送开始事件
     */
    protected void start(String outputPath) {
        // DEMO集成步骤2.1 拼接识别参数： 此处params可以打印出来，直接写到你的代码里去，最终的json一致即可。
        final Map<String, Object> params = new HashMap<>();
        params.put("pid", 1936);
        params.put("accept-audio-volume", false);
        params.put("vad.endpoint-timeout", 0);
        params.put("accept-audio-data", true);
        params.put("outfile", outputPath);
        // params 也可以根据文档此处手动修改，参数会以json的格式在界面和logcat日志中打印
        Log.i(TAG, "设置的start输入参数：" + params);
        // 复制此段可以自动检测常规错误
        (new AutoCheck(getApplicationContext(), new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 100) {
                    AutoCheck autoCheck = (AutoCheck) msg.obj;
                    synchronized (autoCheck) {
                        String message = autoCheck.obtainErrorMessage(); // autoCheck.obtainAllMessage();
//                        txtLog.append(message + "\n");
                        ; // 可以用下面一行替代，在logcat中查看代码
                        // Log.w("AutoCheckMessage", message);
                    }
                }
            }
        }, enableOffline)).checkAsr(params);
        // 这里打印出params， 填写至您自己的app中，直接调用下面这行代码即可。
        // DEMO集成步骤2.2 开始识别
        myRecognizer.start(params);
        isBaiduRecording = true;
        nowRecorder = NowRecorder.BAIDU;
        status = STATUS_WAITING_READY;
    }

    /**
     * 开始录音后，手动点击“停止”按钮。
     * SDK会识别不会再识别停止后的录音。
     * 基于DEMO集成4.1 发送停止事件 停止录音
     */
    protected void stop() {
        isBaiduRecording = false;
        nowRecorder = NowRecorder.NO_RUNNING;
        status = STATUS_STOPPED; // 引擎识别中
        if (myRecognizer != null) {
            myRecognizer.stop();
        }
    }

    /**
     * 开始录音后，手动点击“取消”按钮。
     * SDK会取消本次识别，回到原始状态。
     * 基于DEMO集成4.2 发送取消事件 取消本次识别
     */
    protected void cancel() {
        isBaiduRecording = false;
        nowRecorder = NowRecorder.NO_RUNNING;
        if (myRecognizer != null) {
            myRecognizer.cancel();
        }
        status = STATUS_NONE; // 识别结束，回到初始状态
    }

    protected void handleMsg(Message msg) {
        Log.d(TAG, "recognizer status:" + msg.what);
        switch (msg.what) { // 处理MessageStatusRecogListener中的状态回调
            case STATUS_FINISHED:
                status = msg.what;
                if (msg.arg2 == 1) {
                    String result = msg.obj.toString();
                    String str = result;
                    if (result.indexOf("”") != -1 && result.lastIndexOf("”") != -1) {
                        str = result.substring(result.indexOf("”") + 1, result.lastIndexOf("”"));
                    }
                    String pinyin = Pinyin.toPinyin(str, "/");
                    tvRecogResult.setText(str);
                    Log.d(TAG, pinyin);
                    Log.d(TAG, str);
                    if (str.contains("【asr.finish事件】识别错误, 错误码")) {
                        stop();
                        cancel();
                        recognizerRelease();
                        nowRecorder = NowRecorder.SELF;
                        isGetFileName = true;
                        recorder = AudioRecorder.getInstance();
                        recorder.setFilePath(tempPcmPatchPath + "/" + addAndGetPatchName(false));
                        resumeRecording();
                        break;
                    }
                    if (!isGetFileName) {
                        boolean isNamed = false;
                        for (String location_meeting : PinyinMatch.FUll.keySet()) {
                            if (isNamed) {
                                break;
                            }
                            if (str.contains(location_meeting)) {
                                recognizeName = "500kV" + location_meeting;
                                isNamed = true;
                                break;
                            }
                            for (String item : PinyinMatch.FUll.get(location_meeting)) {
                                if (pinyin.contains(item)) {
                                    recognizeName = "500kV" + location_meeting;
                                    isNamed = true;
                                    break;
                                }
                            }
                        }
                        if (!isNamed && location.equals("")) {
                            for (String l : PinyinMatch.LOCATION.keySet()) {
                                if (str.contains(l)) {
                                    location = l;
                                    break;
                                }
                                for (String item : PinyinMatch.LOCATION.get(l)) {
                                    if (pinyin.contains(item)) {
                                        location = l;
                                        break;
                                    }
                                }
                            }
                        }
                        if (!isNamed && meeting.equals("")) {
                            for (String m : PinyinMatch.MEETING.keySet()) {
                                if (isNamed) {
                                    break;
                                }
                                if (str.contains(m)) {
                                    meeting = m;
                                    if (location.equals("")) {
                                        recognizeName = meeting;
                                    } else {
                                        recognizeName = "500kV" + location + "变电站" + meeting;
                                    }
                                    isNamed = true;
                                    break;
                                }
                                for (String item : PinyinMatch.MEETING.get(m)) {
                                    if (pinyin.contains(item)) {
                                        meeting = m;
                                        if (location.equals("")) {
                                            recognizeName = meeting;
                                        } else {
                                            recognizeName = "500kV" + location + "变电站" + meeting;
                                        }
                                        isNamed = true;
                                        break;
                                    }
                                }
                            }
                        }
                        Log.d(TAG, isNamed + "");
                        if (isNamed) {
                            Log.d(TAG, recognizeName);
                            tvRecogResult.setText("识别结果：" + recognizeName);
                            stop();
                            cancel();
                            recognizerRelease();
                            nowRecorder = NowRecorder.SELF;
                            meeting = "";
                            location = "";
                            isGetFileName = true;
                            recorder = AudioRecorder.getInstance();
                            recorder.setFilePath(tempPcmPatchPath + "/" + addAndGetPatchName(false));
                            resumeRecording();
                        }
                    }
                }
                break;
            case STATUS_NONE:
            case STATUS_READY:
            case STATUS_SPEAKING:
            case STATUS_RECOGNITION:
                status = msg.what;
                break;
            default:
                break;
        }

    }

    private void setBottomBarItemStatusSelector(final ImageView imageView,
                                                final int defaultIcon, final int pressed) {
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!v.isClickable()) {
                    return false;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        imageView.setImageDrawable(getResources().getDrawable(pressed));
                        break;
                    case MotionEvent.ACTION_UP:
                        imageView.setImageDrawable(getResources().getDrawable(defaultIcon));
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }


    private AlertDialog dlg;
    private boolean hasStarted = false;

    @Override
    public void onBackPressed() {
        if (!hasStarted) {
            super.onBackPressed();
            return;
        }
        if (dlg != null && dlg.isShowing()) {
            dlg.dismiss();
            return;
        }
        AlertDialog.Builder dialog = new AlertDialog.Builder(AudioActivity.this);
        final View dialogView = LayoutInflater.from(AudioActivity.this).inflate(R.layout.back_dialog, null);
        dialog.setView(dialogView);
        dialog.setCancelable(true);
        dlg = dialog.show();
        Button ok = dialogView.findViewById(R.id.bt_dialog_ok);
        Button cancel = dialogView.findViewById(R.id.bt_dialog_cancel);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.dismiss();
                finish();
//                AudioRecorderActivity.super.onBackPressed();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.dismiss();
            }
        });
    }
}
