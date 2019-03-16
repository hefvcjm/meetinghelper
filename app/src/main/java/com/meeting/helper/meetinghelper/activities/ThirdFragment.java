package com.meeting.helper.meetinghelper.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.meeting.helper.meetinghelper.R;
import com.meeting.helper.meetinghelper.adapter.RecordListAdapter;
import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;
import com.meeting.helper.meetinghelper.ftp.FtpWorker;
import com.meeting.helper.meetinghelper.ftp.OnTaskStatusChangedListener;
import com.meeting.helper.meetinghelper.ftp.task.FtpTask;
import com.meeting.helper.meetinghelper.ftp.task.ListFilesTask;
import com.meeting.helper.meetinghelper.model.FileInfo;
import com.meeting.helper.meetinghelper.utils.FileUtils;

import java.util.ArrayList;

public class ThirdFragment extends Fragment {

    private static final String TAG = "ThirdFragment";

    private static final String TEMP_PATH = "/storage/emulated/0/meetinghelper/temp";
    private static final String BASE_PATH = "/storage/emulated/0/meetinghelper/records";

    private ListView lvFileList;
    private RecordListAdapter adapter;
    private ArrayList<FileInfo> list = new ArrayList<>();
    private boolean isLocalMode;
    private ArrayList<FileInfo> remoteFiles = new ArrayList<>();
    private String parentFolder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        isLocalMode = getBindActivity().isLocalMode();
        View view = inflater.inflate(R.layout.fragment_layout, container, false);
        init(view);
        return view;
    }

    private void init(View view) {
        Log.d(TAG, "ThirdFragment init");
        lvFileList = view.findViewById(R.id.lv_main_list);
        adapter = new RecordListAdapter(getActivity(), R.layout.item_local_record, list, ((MainActivity) getActivity()).getHandler());
        lvFileList.setAdapter(adapter);
        lvFileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                getBindActivity().onItemClick(list.get(position).getFilePath());
            }
        });
        lvFileList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return getBindActivity().onItemLongClick(adapter, position);
            }
        });
        refreshListView();
    }

    public void refreshListView() {
        if (parentFolder == null) {
            return;
        }
        Log.d(TAG, "refresh thirdFragment");
        if (isLocalMode()) {
            ArrayList<FileInfo> files = FileUtils.getFiles(BASE_PATH + "/" + parentFolder);
            list.clear();
            list.addAll(files);
            adapter.notifyDataSetChanged();
        } else {
            list.clear();
            adapter.notifyDataSetChanged();
            ListFilesTask task = new ListFilesTask(FtpClient.REMOTE_BASE_PATH + "/" + parentFolder);
            task.setOnTaskStatusChangedListener(new OnTaskStatusChangedListener() {
                @Override
                public void onStatusChanged(FtpTask ftpTask, FtpTaskStatus status, Object object) {
                    if (object != null) {
                        remoteFiles = (ArrayList<FileInfo>) object;
                        getBindActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!isLocalMode) {
                                    list.clear();
                                    list.addAll(remoteFiles);
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                }
            });
            FtpWorker.getInstance().addTask(task);
            list.clear();
            list.addAll(remoteFiles);
            adapter.notifyDataSetChanged();
        }
    }

    private boolean isLocalMode() {
        isLocalMode = getBindActivity().isLocalMode();
        return isLocalMode;
    }

    private MainActivity getBindActivity() {
        return ((MainActivity) getActivity());
    }

    public void setParentFolder(String parentFolder) {
        this.parentFolder = parentFolder;
        refreshListView();
    }

    public RecordListAdapter getAdapter() {
        return adapter;
    }

    public ArrayList<FileInfo> getList() {
        return list;
    }

    public ArrayList<FileInfo> getRemoteFiles() {
        return remoteFiles;
    }

    public String getParentFolder() {
        return parentFolder;
    }
}
