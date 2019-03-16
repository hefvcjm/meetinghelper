package com.meeting.helper.meetinghelper.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.meeting.helper.meetinghelper.R;
import com.meeting.helper.meetinghelper.adapter.FolderListAdapter;
import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;
import com.meeting.helper.meetinghelper.ftp.FtpWorker;
import com.meeting.helper.meetinghelper.ftp.OnTaskStatusChangedListener;
import com.meeting.helper.meetinghelper.ftp.task.FtpTask;
import com.meeting.helper.meetinghelper.ftp.task.ListFilesTask;
import com.meeting.helper.meetinghelper.model.FileInfo;
import com.meeting.helper.meetinghelper.utils.FileUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SecondFragment extends Fragment {

    private static final String TAG = "SecondFragment";

    private static final String TEMP_PATH = "/storage/emulated/0/meetinghelper/temp";
    private static final String BASE_PATH = "/storage/emulated/0/meetinghelper/records";

    private ListView folderList;
    private FolderListAdapter adapter;
    private ArrayList<String> list = new ArrayList<>();
    private boolean isLocalMode;
    private ArrayList<FileInfo> remoteFiles = new ArrayList<>();
    ;
    private String parentFolder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        isLocalMode = ((MainActivity) getActivity()).isLocalMode();
        View view = inflater.inflate(R.layout.fragment_layout, container, false);
        init(view);
        return view;
    }

    private void init(View view) {
        Log.d(TAG, "SecondFragment init");
        folderList = view.findViewById(R.id.lv_main_list);
        adapter = new FolderListAdapter(getActivity(), R.layout.item_folder, list);
        folderList.setAdapter(adapter);
        folderList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((MainActivity) getActivity()).getThirdFragment().setParentFolder(parentFolder + "/" + list.get(position));
                FragmentManager fm = getFragmentManager();
                FragmentTransaction tx = fm.beginTransaction();
                tx.hide(SecondFragment.this);
                tx.show(((MainActivity) getActivity()).getThirdFragment());
                tx.addToBackStack(null);
                tx.commit();
            }
        });
        refreshListView();
    }

    public void refreshListView() {
        if (parentFolder == null) {
            return;
        }
        Log.d(TAG, "refresh secondFragment");
        if (isLocalMode()) {
            ArrayList<String> temp = FileUtils.getFolder(BASE_PATH + "/" + parentFolder, new String[]{"班前会", "班后会", "安全学习", "其他"});
            list.clear();
            list.addAll(temp);
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
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!isLocalMode()) {
                                    list.clear();
                                    list.addAll(getFolderListByFileInfoList(remoteFiles));
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                }
            });
            FtpWorker.getInstance().addTask(task);
        }
    }

    private boolean isLocalMode() {
        isLocalMode = ((MainActivity) getActivity()).isLocalMode();
        return isLocalMode;
    }

    private ArrayList<String> getFolderListByFileInfoList(ArrayList<FileInfo> infos) {
        List<String> meetingType = Arrays.asList(new String[]{"班前会", "班后会", "安全学习", "其他"});
        ArrayList<String> temp = new ArrayList<>();
        for (FileInfo info : infos) {
            if (!info.isFileMode()) {
                if (meetingType.contains(info.getFileName())) {
                    temp.add(info.getFileName());
                }
            }
        }
        return temp;
    }

    public void setParentFolder(String parentFolder) {
        this.parentFolder = parentFolder;
        refreshListView();
    }
}
