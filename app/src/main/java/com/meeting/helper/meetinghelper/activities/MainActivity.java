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
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.aip.asrwakeup3.core.recog.IStatus;
import com.meeting.helper.audio.AudioActivity;
import com.meeting.helper.meetinghelper.R;
import com.meeting.helper.meetinghelper.adapter.RecordListAdapter;
import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;
import com.meeting.helper.meetinghelper.ftp.FtpWorker;
import com.meeting.helper.meetinghelper.ftp.OnTaskStatusChangedListener;
import com.meeting.helper.meetinghelper.ftp.task.DownloadTask;
import com.meeting.helper.meetinghelper.ftp.task.FtpTask;
import com.meeting.helper.meetinghelper.ftp.task.ListFilesTask;
import com.meeting.helper.meetinghelper.ftp.task.UploadTask;
import com.meeting.helper.meetinghelper.model.FileInfo;
import com.meeting.helper.meetinghelper.service.FtpService;
import com.meeting.helper.meetinghelper.utils.FileUtils;
import com.melnykov.fab.FloatingActionButton;

import org.angmarch.views.NiceSpinner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
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
    private ListView lv;
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

    private RecordListAdapter adapter;
    private ArrayList<FileInfo> fileList = new ArrayList<>();
    private ArrayList<FileInfo> remoteFiles = new ArrayList<>();
    private AlertDialog dlg;
    private ImageView ivReverseList;
    private ImageView ivTitle;
    private TextView tvTitle;

    private boolean isLocalMode = true;

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RecordListAdapter.LISTVIEW_CHANGED:
                    int countSelected = (int) msg.obj;
                    tvCountSelected.setText("已选中" + countSelected + "项");
                    if (countSelected == adapter.getList().size()) {
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
                    boolean result = (boolean) msg.obj;
                    if (!result) {
                        Toast.makeText(MainActivity.this, "重命名失败", Toast.LENGTH_SHORT).show();
                    }
                    refreshListView();
                    adapter.cancelSelected();
                    break;
                case REMOTE_DELETE:
                    int count = (int) msg.obj;
                    Toast.makeText(MainActivity.this,
                            "成功删除" + count + "个，失败" + (adapter.getDeleteList().size() - count) + "个",
                            Toast.LENGTH_SHORT).show();
                    refreshListView();
                    adapter.cancelSelected();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        init();
        adapter = new RecordListAdapter(MainActivity.this, R.layout.item_local_record, fileList, handler);
        lv.setAdapter(adapter);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordAudio(v);
            }
        });
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!isLocalMode) {
                    return;
                }
                File file = new File(fileList.get(position).getFilePath());
                openFileWithOtherApp(MainActivity.this, file);
            }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String text = "上传";
                if (!isLocalMode) {
                    text = "下载";
                }
                ((TextView) findViewById(R.id.bottom_bar).findViewById(R.id.tv_up_down_load)).setText(text);
                return adapter.onLongClick(position);
            }
        });
        selectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!adapter.getMultiSelected()) {
                    return;
                }
                if (selectAll.getText().toString().equals("全选")) {
                    selectAll.setText("全不选");
                    adapter.selectAll();
                } else if (selectAll.getText().toString().equals("全不选")) {
                    selectAll.setText("全选");
                    adapter.cancelSelectAll();
                }

            }
        });
        cancelSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!adapter.getMultiSelected()) {
                    return;
                }
                adapter.cancelSelected();
                adapter.cancelSelectAll();
            }
        });
        reverseSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.reverseSelected();
            }
        });
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<FileInfo> fileInfo = adapter.getDeleteList();
                ArrayList<String> fileNameList = new ArrayList<>();
                long[] fileSize = new long[fileInfo.size()];
                int i = 0;
                for (FileInfo info : fileInfo) {
                    fileNameList.add(info.getFilePath());
                    fileSize[i] = info.getFileSize();
                    i++;
                }
                Intent intent = new Intent(MainActivity.this, FtpService.class);
                intent.putStringArrayListExtra("ftp_list", adapter.getDeleteFileNameList());
                intent.putExtra("files_size", fileSize);
                if (isLocalMode) {
                    intent.putExtra("direction", 0);
                } else {
                    intent.putExtra("direction", 1);
                }
                startService(intent);
                adapter.cancelSelected();
            }
        });
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                            if (FileUtils.deleteFiles(adapter.getDeleteFileNameList())) {
                                fileList.clear();
                                fileList.addAll(FileUtils.getFiles(BASE_PATH));
                                adapter.cancelSelected();
                                Toast.makeText(MainActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            for (String item : adapter.getDeleteFileNameList()) {
                                FtpWorker.getInstance().addDeleteTask(item);
                            }
                            refreshListView();
                            adapter.cancelSelected();
                        }
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
                        final String newName = "500kV" + selectedLocation + "变电站" + selectedMeeting;
                        if (!newName.equals("")) {
                            if (isLocalMode) {
                                File oldFile = new File(adapter.getDeleteFileNameList().get(0));
                                String temp_name = newName;
                                File file = new File(BASE_PATH + "/" + oldFile.getName().substring(0, 9) + temp_name + ".wav");
                                if (!oldFile.getName().equals(file.getName())) {
                                    if (file.exists()) {
                                        int i = 0;
                                        while (true) {
                                            i++;
                                            file = new File(BASE_PATH + "/" + oldFile.getName().substring(0, 9) + temp_name + "(" + i + ").wav");
                                            if (oldFile.getName().equals(file.getName())){
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
                                    if (!oldFile.getName().equals(file.getName())){
                                        File newFile = new File(BASE_PATH + "/" + oldFile.getName().substring(0, 9) + temp_name + ".wav");
                                        oldFile.renameTo(newFile);
                                    }
                                }
                                fileList.clear();
                                fileList.addAll(FileUtils.getFiles(BASE_PATH));
                                adapter.cancelSelected();
                            } else {
                                FtpWorker.getInstance().addRenameTask(adapter.getDeleteFileNameList().get(0),
                                        adapter.getDeleteFileNameList().get(0).substring(0, 9) + newName + ".wav");
                                refreshListView();
                                adapter.cancelSelected();
                            }
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
                isLocalMode = !isLocalMode;
                if (!isLocalMode) {
                    ivReverseList.setVisibility(View.GONE);
                    ivTitle.setImageResource(R.drawable.ic_back);
                    ivTitle.setClickable(true);
                    tvTitle.setText("远程文件列表");
                }
                refreshListView();
            }
        });
        ivTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLocalMode = !isLocalMode;
                if (isLocalMode) {
                    ivTitle.setImageResource(R.drawable.ic_app);
                    ivTitle.setClickable(false);
                    tvTitle.setText(R.string.app_name);
                    ivReverseList.setVisibility(View.VISIBLE);
                }
                refreshListView();
            }
        });
        ivTitle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!v.isClickable()) {
                    return false;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (isLocalMode) {
                            ivTitle.setImageDrawable(getResources().getDrawable(R.drawable.ic_app));
                        } else {
                            ivTitle.setImageResource(R.drawable.ic_back_pressed);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (isLocalMode) {
                            ivTitle.setImageDrawable(getResources().getDrawable(R.drawable.ic_app));
                        } else {
                            ivTitle.setImageDrawable(getResources().getDrawable(R.drawable.ic_back));
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
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
            }
        }).start();
    }

    @Override
    protected void onResume() {
        ftpWorker = FtpWorker.getInstance();
        FtpClient.getInstance();
        refreshListView();
        super.onResume();
    }

    private void init() {
        ftpWorker = FtpWorker.getInstance();
        btn = findViewById(R.id.new_record);
        lv = findViewById(R.id.record_history);
        selectAll = findViewById(R.id.top_bar).findViewById(R.id.select_all);
        cancelSelectAll = findViewById(R.id.top_bar).findViewById(R.id.cancel_select_all);
        btn.attachToListView(lv);

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
//                            ivReverseList.setImageDrawable(getResources().getDrawable(R.drawable.ic_local_pressed));
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (isLocalMode) {
                            ivReverseList.setImageDrawable(getResources().getDrawable(R.drawable.ic_cloud));
                        } else {
//                            ivReverseList.setImageDrawable(getResources().getDrawable(R.drawable.ic_local));
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
        fileList = FileUtils.getFiles(BASE_PATH);
    }

    private void refreshListView() {
        if (isLocalMode) {
            ArrayList<FileInfo> files = FileUtils.getFiles(BASE_PATH);
            fileList.clear();
            fileList.addAll(files);
            ivReverseList.setImageDrawable(getResources().getDrawable(R.drawable.ic_cloud));
            adapter.notifyDataSetChanged();
        } else {
            ListFilesTask task = new ListFilesTask();
            task.setOnTaskStatusChangedListener(new OnTaskStatusChangedListener() {
                @Override
                public void onStatusChanged(FtpTask ftpTask, FtpTaskStatus status, Object object) {
                    if (object != null) {
                        remoteFiles = (ArrayList<FileInfo>) object;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!isLocalMode) {
                                    fileList.clear();
                                    fileList.addAll(remoteFiles);
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                }
            });
            FtpWorker.getInstance().addTask(task);
            fileList.clear();
            fileList.addAll(remoteFiles);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onBackPressed() {
        if (adapter.getMultiSelected()) {
            adapter.cancelSelected();
            adapter.cancelSelectAll();
            return;
        } else if (!isLocalMode) {
            isLocalMode = !isLocalMode;
            if (isLocalMode) {
                ivReverseList.setVisibility(View.VISIBLE);
                ivTitle.setImageResource(R.drawable.ic_app);
                ivTitle.setClickable(false);
                tvTitle.setText(R.string.app_name);
            }
            refreshListView();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "保存录音成功", Toast.LENGTH_SHORT).show();
                if (data != null) {
                    final String filePath = data.getStringExtra("filePath");
                    final String recognizeName = data.getStringExtra("recognize_result");
                    if (filePath != null) {
//                        if (FtpWorker.getInstance().getNowTask() != null && FtpWorker.getInstance().getNowTask().getClass() == DownloadTask.class) {
//                            Toast.makeText(this, "后台正忙，录音未上传", Toast.LENGTH_SHORT).show();
//                            return;
//                        }
                        Log.d(TAG, "filePath: " + filePath);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String temp_name = recognizeName;
                                File file = new File(BASE_PATH + "/" + temp_name + ".wav");
                                if (file.exists()) {
                                    int i = 0;
                                    while (true) {
                                        i++;
                                        file = new File(BASE_PATH + "/" + temp_name + "(" + i + ").wav");
                                        if (!file.exists()) {
                                            temp_name = temp_name + "(" + i + ")";
                                            break;
                                        }
                                        if (i >= 100) {
                                            break;
                                        }
                                    }
                                }
                                FileUtils.convertPcm2Wav(filePath, BASE_PATH + "/" + temp_name + ".wav", 16000, 1, 16);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        refreshListView();
                                    }
                                });
//                                runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        refreshListView();
//                                        if (FtpWorker.getInstance().getNowTask() != null && FtpWorker.getInstance().getNowTask().getClass() == DownloadTask.class) {
//                                            Toast.makeText(MainActivity.this, "后台正忙，录音未上传", Toast.LENGTH_SHORT).show();
//                                            return;
//                                        } else {
//                                            Intent intent = new Intent(MainActivity.this, FtpService.class);
//                                            ArrayList<String> recordFile = new ArrayList<>();
//                                            recordFile.add(BASE_PATH + "/" + recognizeName);
//                                            long[] size = {new File(BASE_PATH + "/" + recognizeName).length()};
//                                            intent.putStringArrayListExtra("ftp_list", recordFile);
//                                            intent.putExtra("files_size", size);
//                                            intent.putExtra("direction", 0);
//                                            startService(intent);
//                                        }
//                                    }
//                                });
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

    private void sendMessage(int what, Object object) {
        Message msg = new Message();
        msg.what = what;
        msg.obj = object;
        handler.sendMessage(msg);
    }

    private void grantUriPermission(Context context, Uri fileUri, Intent intent) {
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    public void openFileWithOtherApp(Context context, File file) {

        try {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //设置intent的Action属性
            intent.setAction(Intent.ACTION_VIEW);
            //获取文件file的MIME类型
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
}

