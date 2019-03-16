package com.meeting.helper.meetinghelper.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.aip.asrwakeup3.core.recog.IStatus;
import com.meeting.helper.audio.AudioActivity;
import com.meeting.helper.meetinghelper.R;
import com.meeting.helper.meetinghelper.adapter.RecordListAdapter;
import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpWorker;
import com.meeting.helper.meetinghelper.ftp.task.DownloadTask;
import com.meeting.helper.meetinghelper.ftp.task.UploadTask;
import com.meeting.helper.meetinghelper.model.FileInfo;
import com.meeting.helper.meetinghelper.service.FtpService;
import com.meeting.helper.meetinghelper.utils.FileUtils;
import com.melnykov.fab.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements IStatus {

    private static final String TAG = "MainActivity";

    private static final int PERMISSION_REQUEST_CODE = 0;

    private static final int REMOTE_LIST = 1;
    private static final int REMOTE_RENAME = 2;
    private static final int REMOTE_DELETE = 3;

    private static final int REQUEST_RECORD_AUDIO = 0;
    private static final String TEMP_PATH = "/storage/emulated/0/meetinghelper/temp";
    private static final String BASE_PATH = "/storage/emulated/0/meetinghelper/records";

    private FloatingActionButton btn;
    private Button selectAll;
    private Button cancelSelectAll;
    private LinearLayout reverseSelected;
    private ImageView ivReverseSelected;
    private LinearLayout upload;
    private ImageView ivUpload;
    private LinearLayout delete;
    private ImageView ivDelete;
    private LinearLayout rename;
    private ImageView ivRename;
    private TextView tvCountSelected;

    private FtpWorker ftpWorker;

    private AlertDialog dlg;
    private ImageView ivReverseList;
    private ImageView ivTitle;
    private TextView tvTitle;

    private boolean isLocalMode = true;

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            handleMsg(msg);
        }
    };

    private FirstFragment firstFragment;
    private SecondFragment secondFragment;
    private ThirdFragment thirdFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        init();
        initOnClick();
    }

    @Override
    protected void onResume() {
        ftpWorker = FtpWorker.getInstance();
        FileUtils.clearEmptyDirectory(BASE_PATH);
        if (!new File(BASE_PATH).exists()) {
            new File(BASE_PATH).mkdirs();
        }
        refreshListView();
        super.onResume();
    }

    private void init() {
        FileUtils.clearEmptyDirectory(BASE_PATH);
        if (!new File(BASE_PATH).exists()) {
            new File(BASE_PATH).mkdirs();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(BASE_PATH);
                if (!file.exists()) {
                    file.mkdirs();
                }
                file = new File(TEMP_PATH);
                if (!file.exists()) {
                    file.mkdirs();
                }
                FtpClient.getInstance().clearEmptyDirectory(FtpClient.REMOTE_BASE_PATH);
            }
        }).start();
        ftpWorker = FtpWorker.getInstance();
        btn = findViewById(R.id.new_record);
        selectAll = findViewById(R.id.top_bar).findViewById(R.id.select_all);
        cancelSelectAll = findViewById(R.id.top_bar).findViewById(R.id.cancel_select_all);

        reverseSelected = findViewById(R.id.bottom_bar).findViewById(R.id.ll_reverse_selected);
        ivReverseSelected = reverseSelected.findViewById(R.id.iv_reverse_selected);
        setBottomBarItemStatusSelector(reverseSelected, ivReverseSelected, R.drawable.ic_reverse, R.drawable.ic_reverse_pressed);
        upload = findViewById(R.id.bottom_bar).findViewById(R.id.ll_upload);
        ivUpload = upload.findViewById(R.id.iv_upload);
        setBottomBarItemStatusSelector(upload, ivUpload, R.drawable.ic_upload, R.drawable.ic_upload_pressed);
        upload.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!v.isClickable()) {
                    return false;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (isLocalMode) {
                            ivUpload.setImageDrawable(getResources().getDrawable(R.drawable.ic_upload_pressed));
                        } else {
                            ivUpload.setImageDrawable(getResources().getDrawable(R.drawable.ic_download_pressed));
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (isLocalMode) {
                            ivUpload.setImageDrawable(getResources().getDrawable(R.drawable.ic_upload));
                        } else {
                            ivUpload.setImageDrawable(getResources().getDrawable(R.drawable.ic_download));
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
        delete = findViewById(R.id.bottom_bar).findViewById(R.id.ll_delete);
        ivDelete = delete.findViewById(R.id.iv_delete);
        setBottomBarItemStatusSelector(delete, ivDelete, R.drawable.ic_delete, R.drawable.ic_delete_pressed);
        rename = findViewById(R.id.bottom_bar).findViewById(R.id.ll_rename);
        ivRename = rename.findViewById(R.id.iv_rename);
        setBottomBarItemStatusSelector(rename, ivRename, R.drawable.ic_rename, R.drawable.ic_rename_pressed);

        tvCountSelected = findViewById(R.id.top_bar).findViewById(R.id.tv_count_selected);
        ivReverseList = findViewById(R.id.title).findViewById(R.id.iv_reverse_list);
        ivReverseList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!v.isClickable()) {
                    return false;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (isLocalMode) {
                            ivReverseList.setImageDrawable(getResources().getDrawable(R.drawable.ic_cloud_pressed));
                        } else {
                            ivReverseList.setImageDrawable(getResources().getDrawable(R.drawable.ic_local_pressed));
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (isLocalMode) {
                            ivReverseList.setImageDrawable(getResources().getDrawable(R.drawable.ic_cloud));
                        } else {
                            ivReverseList.setImageDrawable(getResources().getDrawable(R.drawable.ic_local));
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });

        ivTitle = findViewById(R.id.iv_title_icon);
        tvTitle = findViewById(R.id.tv_title_text);
        tvTitle.setText("本地列表");
        tvTitle.setGravity(Gravity.CENTER);
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
                ActivityCompat.requestPermissions(MainActivity.this, arrayPermissions, PERMISSION_REQUEST_CODE);
            }
        }
        firstFragment = (FirstFragment) getSupportFragmentManager().findFragmentById(R.id.fg_main_first);
        secondFragment = (SecondFragment) getSupportFragmentManager().findFragmentById(R.id.fg_main_second);
        thirdFragment = (ThirdFragment) getSupportFragmentManager().findFragmentById(R.id.fg_main_third);
        getSupportFragmentManager().beginTransaction().hide(secondFragment).hide(thirdFragment).commit();
    }

    @Override
    public void onBackPressed() {
        if (thirdFragment != null && thirdFragment.getAdapter().getMultiSelected()) {
            thirdFragment.getAdapter().cancelSelected();
            thirdFragment.getAdapter().cancelSelectAll();
            return;
//        } else if (!isLocalMode) {
//            isLocalMode = !isLocalMode;
//            if (isLocalMode) {
//                tvTitle.setText("本地列表");
//            } else {
//                tvTitle.setText("远程列表");
//            }
//            refreshListView();
        } else {
            super.onBackPressed();
        }
    }

    private void refreshListView() {
        if (firstFragment != null && firstFragment.isVisible()) {
            firstFragment.refreshListView();
        }
        if (secondFragment != null && secondFragment.isVisible()) {
            secondFragment.refreshListView();
        }
        if (thirdFragment != null && thirdFragment.isVisible()) {
            thirdFragment.refreshListView();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "保存录音成功", Toast.LENGTH_SHORT).show();
                if (data != null) {
                    final String filePath = data.getStringExtra("filePath");
                    final String recognizeName = data.getStringExtra("recognize_result");
                    if (filePath != null) {
                        Log.d(TAG, "filePath: " + filePath);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String temp_name = recognizeName;
                                String date = temp_name.substring(0, 10);
                                if (date.matches("^[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}$")) {
                                    date = date.split("-")[0] + "年" + date.split("-")[1] + "月";
                                } else {
                                    date = "其他";
                                }
                                String meetingType = "其他";
                                if (recognizeName.contains("班前会")) {
                                    meetingType = "班前会";
                                } else if (recognizeName.contains("班后会")) {
                                    meetingType = "班后会";
                                } else if (recognizeName.contains("安全学习")) {
                                    meetingType = "安全学习";
                                }
                                String fileDir = BASE_PATH + "/" + date + "/" + meetingType + "/";
                                File dir = new File(fileDir);
                                if (!dir.exists()) {
                                    dir.mkdirs();
                                }
                                File file = new File(fileDir + temp_name + ".wav");
                                if (file.exists()) {
                                    int i = 0;
                                    while (true) {
                                        i++;
                                        file = new File(fileDir + temp_name + "(" + i + ").wav");
                                        if (!file.exists()) {
                                            temp_name = temp_name + "(" + i + ")";
                                            break;
                                        }
                                        if (i >= 100) {
                                            break;
                                        }
                                    }
                                }
                                FileUtils.convertPcm2Wav(filePath, fileDir + temp_name + ".wav", 16000, 1, 16);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        refreshListView();
                                    }
                                });
                            }
                        }).start();
                    }
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "未保存录音", Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "用户禁止了应用相关权限，无法运行应用", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    public void recordAudio(View v) {
        Intent intent = new Intent(MainActivity.this, AudioActivity.class);
        startActivityForResult(intent, REQUEST_RECORD_AUDIO);
    }


    private void setBottomBarItemStatusSelector(View view, final ImageView imageView, final int defaultIcon, final int pressed) {
        view.setOnTouchListener(new View.OnTouchListener() {
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


    private void grantUriPermission(Context context, Uri fileUri, Intent intent) {
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    public boolean isLocalMode() {
        return isLocalMode;
    }

    public Handler getHandler() {
        return handler;
    }

    public void setFirstFragment(FirstFragment firstFragment) {
        this.firstFragment = firstFragment;
    }

    public void setSecondFragment(SecondFragment secondFragment) {
        this.secondFragment = secondFragment;
    }

    public void setThirdFragment(ThirdFragment thirdFragment) {
        this.thirdFragment = thirdFragment;
    }

    public FirstFragment getFirstFragment() {
        return firstFragment;
    }

    public SecondFragment getSecondFragment() {
        return secondFragment;
    }

    public ThirdFragment getThirdFragment() {
        return thirdFragment;
    }

    public void onItemClick(String filePath) {
        if (!isLocalMode) {
            return;
        }
        File file = new File(filePath);
        openFileWithOtherApp(MainActivity.this, file);
    }

    public boolean onItemLongClick(RecordListAdapter adapter, int position) {
        String text = "上传";
        if (!isLocalMode) {
            text = "下载";
        }
        ((TextView) findViewById(R.id.bottom_bar).findViewById(R.id.tv_up_down_load)).setText(text);
        return adapter.onLongClick(position);
    }

    public void openFileWithOtherApp(Context context, File file) {
        try {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //设置intent的Action属性
            intent.setAction(Intent.ACTION_VIEW);
            //获取file的MIME类型
            String type = FileUtils.getMIMEType(file);
            //设置intent的data和Type属性。android 7.0以上crash,改用provider
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.d(TAG, context.getPackageName());
                Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);//android 7.0以上
                intent.setDataAndType(fileUri, type);
                grantUriPermission(context, fileUri, intent);
            } else {
                intent.setDataAndType(/*uri*/Uri.fromFile(file), type);
            }
            //跳转
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initOnClick() {
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordAudio(v);
            }
        });
        selectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (thirdFragment == null) {
                    return;
                }
                if (!thirdFragment.getAdapter().getMultiSelected()) {
                    thirdFragment.getAdapter().cancelSelected();
                    thirdFragment.getAdapter().cancelSelectAll();
                    return;
                }
                if (selectAll.getText().toString().equals("全选")) {
                    selectAll.setText("全不选");
                    thirdFragment.getAdapter().selectAll();
                } else if (selectAll.getText().toString().equals("全不选")) {
                    selectAll.setText("全选");
                    thirdFragment.getAdapter().cancelSelectAll();
                }

            }
        });
        cancelSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (thirdFragment == null) {
                    return;
                }
                if (!thirdFragment.getAdapter().getMultiSelected()) {
                    thirdFragment.getAdapter().cancelSelected();
                    thirdFragment.getAdapter().cancelSelectAll();
                    return;
                }
                thirdFragment.getAdapter().cancelSelected();
                thirdFragment.getAdapter().cancelSelectAll();
            }
        });
        reverseSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (thirdFragment == null) {
                    return;
                }
                if (!thirdFragment.getAdapter().getMultiSelected()) {
                    thirdFragment.getAdapter().cancelSelected();
                    thirdFragment.getAdapter().cancelSelectAll();
                    return;
                }
                thirdFragment.getAdapter().reverseSelected();
            }
        });
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (thirdFragment == null) {
                    return;
                }
                ArrayList<FileInfo> fileInfo = thirdFragment.getAdapter().getDeleteList();
                ArrayList<String> fileNameList = new ArrayList<>();
                long[] fileSize = new long[fileInfo.size()];
                int i = 0;
                for (FileInfo info : fileInfo) {
                    if (isLocalMode) {
                        fileNameList.add(info.getFilePath());
                    } else {
                        fileNameList.add(info.getFileName());
                    }
                    fileSize[i] = info.getFileSize();
                    i++;
                }
                Intent intent = new Intent(MainActivity.this, FtpService.class);
                intent.putStringArrayListExtra("ftp_list", fileNameList);
                intent.putExtra("files_size", fileSize);
                ArrayList<String> remotePath = new ArrayList<>();
                for (FileInfo item : thirdFragment.getAdapter().getDeleteList()) {
                    remotePath.add(FtpClient.REMOTE_BASE_PATH + "/" + thirdFragment.getParentFolder());
                }
                intent.putExtra("remote_path", remotePath);
                if (isLocalMode) {
                    intent.putExtra("direction", 0);
                } else {
                    intent.putExtra("direction", 1);
                }
                startService(intent);
                thirdFragment.getAdapter().cancelSelected();
            }
        });
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (thirdFragment == null) {
                    return;
                }
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                final View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.delete_dialog, null);
                dialog.setView(dialogView);
                dialog.setCancelable(true);
                dlg = dialog.show();
                Button ok = dialogView.findViewById(R.id.bt_dialog_ok);
                Button cancel = dialogView.findViewById(R.id.bt_dialog_cancel);
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dlg.dismiss();
                        if (isLocalMode) {
                            if (FileUtils.deleteFiles(thirdFragment.getAdapter().getDeleteFileNameList())) {
                                thirdFragment.refreshListView();
                                Toast.makeText(MainActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            for (FileInfo item : thirdFragment.getAdapter().getDeleteList()) {
                                FtpWorker.getInstance().addDeleteTask(item.getFilePath(), item.getFileName());
                            }
                        }
                        refreshListView();
                        thirdFragment.getAdapter().cancelSelected();
                    }
                });
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dlg.dismiss();
                    }
                });
            }
        });
        rename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (thirdFragment == null) {
                    return;
                }
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                final View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.rename_dialog, null);
                dialog.setView(dialogView);
                dialog.setCancelable(true);
                dlg = dialog.show();
                Button ok = dialogView.findViewById(R.id.bt_dialog_ok);
                Button cancel = dialogView.findViewById(R.id.bt_dialog_cancel);
                final Spinner location = dialogView.findViewById(R.id.sp_location);
                final Spinner meeting = dialogView.findViewById(R.id.sp_meeting);
//                final List<String> locations = new LinkedList<>(Arrays.asList("巴南", "隆盛", "陈家桥", "板桥", "圣泉", "玉屏", "长寿", "石坪", "思源", "如意", "明月山"));
//                final List<String> meetings = new LinkedList<>(Arrays.asList("班前会", "班后会", "安全学习"));
//                location.attachDataSource(locations);
//                meeting.attachDataSource(meetings);
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        EditText etRenameInput = dialogView.findViewById(R.id.et_dialog_input_rename);
//                        final String newName = etRenameInput.getText().toString().trim();
                        final String selectedLocation = (String) location.getSelectedItem();
                        final String selectedMeeting = (String) meeting.getSelectedItem();
                        final String newName = selectedLocation + "变电站" + selectedMeeting;
                        if (!newName.equals("")) {
                            if (isLocalMode) {
                                File oldFile = new File(thirdFragment.getAdapter().getDeleteFileNameList().get(0));
                                String fileDir = oldFile.getAbsolutePath().substring(0, oldFile.getAbsolutePath().lastIndexOf('/'));
                                String temp_name = newName;
                                File file = new File(fileDir + "/" + oldFile.getName().substring(0, 10) + temp_name + ".wav");
                                if (!oldFile.getName().equals(file.getName())) {
                                    if (file.exists()) {
                                        int i = 0;
                                        while (true) {
                                            i++;
                                            file = new File(fileDir + "/" + oldFile.getName().substring(0, 10) + temp_name + "(" + i + ").wav");
                                            if (oldFile.getName().equals(file.getName())) {
                                                temp_name = temp_name + "(" + i + ")";
                                                break;
                                            }
                                            if (!file.exists()) {
                                                temp_name = temp_name + "(" + i + ")";
                                                break;
                                            }
                                            if (i >= 100) {
                                                break;
                                            }
                                        }
                                    }
                                    if (!oldFile.getName().equals(file.getName())) {
                                        File newFile = new File(fileDir + "/" + oldFile.getName().substring(0, 10) + temp_name + ".wav");
                                        oldFile.renameTo(newFile);
                                    }
                                }
                                thirdFragment.refreshListView();
                            } else {
                                FileInfo info = thirdFragment.getAdapter().getDeleteList().get(0);
                                FtpWorker.getInstance().addRenameTask(info.getFilePath(), info.getFileName(),
                                        info.getFileName().substring(0, 10) + newName + ".wav");
                            }
                            refreshListView();
                            thirdFragment.getAdapter().cancelSelected();
                        }
                        dlg.dismiss();
                    }
                });
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dlg.dismiss();
                    }
                });
            }
        });
        ivReverseList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                int count = fragmentManager.getBackStackEntryCount();
                for (int i = 0; i < count; ++i) {
                    fragmentManager.popBackStack();
                }
                fragmentManager.beginTransaction().show(firstFragment).hide(secondFragment).hide(thirdFragment).commit();
                isLocalMode = !isLocalMode;
                if (!isLocalMode) {
                    ivReverseList.setImageResource(R.drawable.ic_local);
                    tvTitle.setText("远程列表");
                } else {
                    ivReverseList.setImageResource(R.drawable.ic_cloud);
                    tvTitle.setText("本地列表");
                }
                firstFragment.refreshListView();
            }
        });

        ivTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void handleMsg(Message msg) {
        switch (msg.what) {
            case RecordListAdapter.LISTVIEW_CHANGED:
                if (thirdFragment == null) {
                    break;
                }
                int countSelected = (int) msg.obj;
                tvCountSelected.setText("已选中" + countSelected + "项");
                if (countSelected == thirdFragment.getAdapter().getList().size()) {
                    selectAll.setText("全不选");
                } else {
                    selectAll.setText("全选");
                }
                if (isLocalMode) {
                    if (countSelected == 0) {
                        ivUpload.setImageDrawable(getResources().getDrawable(R.drawable.ic_upload_fade));
                        upload.setClickable(false);
                        ivDelete.setImageDrawable(getResources().getDrawable(R.drawable.ic_delete_fade));
                        delete.setClickable(false);
                        ivRename.setImageDrawable(getResources().getDrawable(R.drawable.ic_rename_fade));
                        rename.setClickable(false);
                    } else {
                        if (countSelected == 1) {
                            ivRename.setImageDrawable(getResources().getDrawable(R.drawable.ic_rename));
                            rename.setClickable(true);
                        } else {
                            ivRename.setImageDrawable(getResources().getDrawable(R.drawable.ic_rename_fade));
                            rename.setClickable(false);
                        }
                        ivUpload.setImageDrawable(getResources().getDrawable(R.drawable.ic_upload));
                        upload.setClickable(true);
                        ivDelete.setImageDrawable(getResources().getDrawable(R.drawable.ic_delete));
                        delete.setClickable(true);
                    }
                    if (FtpWorker.getInstance().getNowTask() != null
                            && FtpWorker.getInstance().getNowTask().getClass() == DownloadTask.class) {
                        ivUpload.setImageDrawable(getResources().getDrawable(R.drawable.ic_upload_fade));
                        upload.setClickable(false);
                    }
                } else {
                    if (countSelected == 0) {
                        ivUpload.setImageDrawable(getResources().getDrawable(R.drawable.ic_download_fade));
                        upload.setClickable(false);
                        ivDelete.setImageDrawable(getResources().getDrawable(R.drawable.ic_delete_fade));
                        delete.setClickable(false);
                        ivRename.setImageDrawable(getResources().getDrawable(R.drawable.ic_rename_fade));
                        rename.setClickable(false);
                    } else {
                        if (countSelected == 1) {
                            ivRename.setImageDrawable(getResources().getDrawable(R.drawable.ic_rename));
                            rename.setClickable(true);
                        } else {
                            ivRename.setImageDrawable(getResources().getDrawable(R.drawable.ic_rename_fade));
                            rename.setClickable(false);
                        }
                        ivUpload.setImageDrawable(getResources().getDrawable(R.drawable.ic_download));
                        upload.setClickable(true);
                        ivDelete.setImageDrawable(getResources().getDrawable(R.drawable.ic_delete));
                        delete.setClickable(true);
                    }
                    if (FtpWorker.getInstance().getNowTask() != null && FtpWorker.getInstance().getNowTask().getClass() == UploadTask.class) {
                        ivUpload.setImageDrawable(getResources().getDrawable(R.drawable.ic_download_fade));
                        upload.setClickable(false);
                        ivDelete.setImageDrawable(getResources().getDrawable(R.drawable.ic_delete_fade));
                        delete.setClickable(false);
                        ivRename.setImageDrawable(getResources().getDrawable(R.drawable.ic_rename_fade));
                        rename.setClickable(false);
                    }
                    if (FtpWorker.getInstance().getNowTask() != null && FtpWorker.getInstance().getNowTask().getClass() == DownloadTask.class) {
                        ivDelete.setImageDrawable(getResources().getDrawable(R.drawable.ic_delete_fade));
                        delete.setClickable(false);
                        ivRename.setImageDrawable(getResources().getDrawable(R.drawable.ic_rename_fade));
                        rename.setClickable(false);
                    }
                }
                break;
            case REMOTE_LIST:
                break;
            case REMOTE_RENAME:
                if (thirdFragment == null) {
                    break;
                }
                boolean result = (boolean) msg.obj;
                if (!result) {
                    Toast.makeText(MainActivity.this, "重命名失败", Toast.LENGTH_SHORT).show();
                }
                refreshListView();
                thirdFragment.getAdapter().cancelSelected();
                break;
            case REMOTE_DELETE:
                if (thirdFragment == null) {
                    break;
                }
                int count = (int) msg.obj;
                Toast.makeText(MainActivity.this,
                        "成功删除" + count + "个，失败" + (thirdFragment.getAdapter().getDeleteList().size() - count) + "个",
                        Toast.LENGTH_SHORT).show();
                refreshListView();
                thirdFragment.getAdapter().cancelSelected();
                break;
            default:
                break;
        }
    }
}

